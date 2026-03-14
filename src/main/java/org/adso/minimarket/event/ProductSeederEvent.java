package org.adso.minimarket.event;

import org.adso.minimarket.models.document.ProductDocument;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.models.user.Role;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.adso.minimarket.repository.jpa.UserRepository;
import org.adso.minimarket.repository.elastic.ProductSearchRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductSeederEvent {

    private final ProductRepository dbRepo;
    private final ProductSearchRepository esRepo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProductSeederEvent(ProductRepository dbRepo, ProductSearchRepository esRepo,
            UserRepository userRepository
            , PasswordEncoder passwordEncoder) {
        this.dbRepo = dbRepo;
        this.esRepo = esRepo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void sync() {
        seedAdmin();
        esRepo.deleteAll();

        var products = dbRepo.findAll();
        List<ProductDocument> productDocuments = new ArrayList<>();

        for (Product p : products) {
            java.util.List<String> imageUrls = p.getImages() != null
                    ? p.getImages().stream().map(org.adso.minimarket.models.product.Image::getUrl).toList()
                    : java.util.Collections.emptyList();

            productDocuments.add(new ProductDocument(
                    p.getId(),
                    p.getName(),
                    p.getSlug(),
                    p.getDescription(),
                    p.getCategory().getName(),
                    p.getPrice(),
                    p.getBrand(),
                    p.getStock(),
                    p.getAttributes(),
                    imageUrls,
                    p.getCreatedAt()
            ));
        }

        esRepo.saveAll(productDocuments);

        System.out.println("Elasticsearch reseteado y sincronizado.");
    }

    private void seedAdmin() {
        String adminEmail = "admin@gmail.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User(
                    "Admin",
                    "System",
                    adminEmail,
                    passwordEncoder.encode("123456789")
            );
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("creado usuario admin: " + adminEmail);
        }
    }
}

