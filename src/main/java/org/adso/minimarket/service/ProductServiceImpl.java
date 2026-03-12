package org.adso.minimarket.service;

import org.adso.minimarket.dto.CreateProductRequest;
import org.adso.minimarket.dto.DetailedProduct;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.mappers.ProductMapper;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.models.document.ProductDocument;
import org.adso.minimarket.models.inventory.TransactionType;
import org.adso.minimarket.models.product.Image;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.adso.minimarket.validation.ProductAttributeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final SearchService searchService;
    private final ProductAttributeValidator attributeValidator;
    private final InventoryService inventoryService;

    ProductServiceImpl(CategoryService categoryService, ProductRepository productRepository,
                       ProductMapper productMapper, SearchService searchService,
                       ProductAttributeValidator attributeValidator, InventoryService inventoryService) {
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.searchService = searchService;
        this.attributeValidator = attributeValidator;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public Long createProduct(CreateProductRequest productRequest) {
        Category category = categoryService.getById(productRequest.getCategoryId());

        attributeValidator.validate(productRequest.getSpecifications(), category.getAllAttributeDefinitions());

        int initialStock = productRequest.getStock() != null ? productRequest.getStock() : 0;

        Product product = new Product(
                productRequest.getName(),
                productRequest.getDescription(),
                productRequest.getPrice(),
                initialStock,
                category,
                productRequest.getBrand(),
                productRequest.getSpecifications()
        );

        if (productRequest.getImages() != null) {
            for (int i = 0; i < productRequest.getImages().size(); i++) {
                Image image = new Image();
                image.setUrl(productRequest.getImages().get(i));
                image.setProduct(product);
                image.setPosition(i);
                product.getImages().add(image);
            }
        }


        Product savedProduct = productRepository.save(product);

        if (initialStock > 0) {
            inventoryService.logTransaction(
                    savedProduct,
                    initialStock,
                    TransactionType.RESTOCK,
                    "Initial Stock"
            );
        }

        indexProductToElasticsearch(savedProduct, category.getName());
        return savedProduct.getId();
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public List<DetailedProduct> getFeaturedProducts() {
        return productRepository.findTop8ByOrderByCreatedAtDesc()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    public DetailedProduct getDetailedProductById(Long id) {
        return productMapper.toDto(productRepository.findDetailedById(id).orElseThrow(
                () -> new NotFoundException("Product not found")
        ));
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Category category = categoryService.getById(request.getCategoryId());
        attributeValidator.validate(request.getSpecifications(), category.getAllAttributeDefinitions());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setBrand(request.getBrand());
        product.setCategory(category);
        product.setAttributes(request.getSpecifications());

        if (request.getStock() != null && !request.getStock().equals(product.getStock())) {
            int difference = request.getStock() - product.getStock();
            product.setStock(request.getStock());
            inventoryService.logTransaction(
                    product,
                    Math.abs(difference),
                    difference > 0 ? TransactionType.RESTOCK : TransactionType.SALE,
                    "Manual Update"
            );
        }

        if (request.getImages() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImages().size(); i++) {
                Image image = new Image();
                image.setUrl(request.getImages().get(i));
                image.setProduct(product);
                image.setPosition(i);
                product.getImages().add(image);
            }
        }

        Product savedProduct = productRepository.save(product);
        indexProductToElasticsearch(savedProduct, category.getName());
    }

    private void indexProductToElasticsearch(Product product, String categoryName) {
        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream().map(Image::getUrl).toList()
                : Collections.emptyList();

        ProductDocument productDocument = new ProductDocument(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                categoryName,
                product.getPrice(),
                product.getBrand(),
                product.getStock(),
                product.getAttributes(),
                imageUrls,
                product.getCreatedAt()
        );

        searchService.saveIndex(productDocument);
    }
}
