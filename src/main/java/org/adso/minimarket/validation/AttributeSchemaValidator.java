package org.adso.minimarket.validation;

import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.repository.jpa.CategoryRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AttributeSchemaValidator {

    private final CategoryRepository categoryRepository;

    private final Map<String, Map<String, AttributeDefinition>> attributeDefinitionsCache = new ConcurrentHashMap<>();

    private final Map<String, AttributeDefinition> globalAttributeDefinitionsCache = new ConcurrentHashMap<>();

    private final Map<String, List<String>> categoryHierarchyCache = new ConcurrentHashMap<>();

    public AttributeSchemaValidator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadSchema() {
        // Cargamos todos los atributos globalmente en memoria la primera vez que arranca Spring Boot.
        // Esto es re importante para no destrozar la base de datos con peticiones cada vez
        // que alguien cambie los filtros del frontend o cree un producto.
        log.info("loading attribute schema into memry..");
        List<Category> allCategories = categoryRepository.findAll();

        for (Category category : allCategories) {
            String categoryKey = category.getName().toLowerCase();
            log.info("category key: {}", categoryKey);
            Map<String, AttributeDefinition> definitions = new HashMap<>();

            List<Map<String, Object>> definitionMaps = category.getAllAttributeDefinitions();

            if (definitionMaps != null) {
                for (Map<String, Object> defMap : definitionMaps) {
                    try {
                        AttributeDefinition def = AttributeDefinition.fromMap(defMap);
                        String attrName = def.getName();
                        
                        definitions.put(attrName, def);
                    } catch (Exception e) {
                        log.warn("Failed to parse attribute definition for category '{}': {}",
                                category.getName(), e.getMessage());
                    }
                }
            }

            attributeDefinitionsCache.put(categoryKey, definitions);
            globalAttributeDefinitionsCache.putAll(definitions);
        }

        //la idea es que las llamadas para una categoria llame a las categorias
        //hijas
        for (Category category : allCategories) {
            String categoryName = category.getName().toLowerCase();
            categoryHierarchyCache.put(categoryName, getAllNamesRecursive(category));
        }

        log.info("Loaded schema for {} categories and hierarchy for {} paths.",
                attributeDefinitionsCache.size(),
                categoryHierarchyCache.size());
    }

    private List<String> getAllNamesRecursive(Category category) {
        List<String> names = new ArrayList<>();
        names.add(category.getName());
        if (category.getSubcategories() != null) {
            for (Category sub : category.getSubcategories()) {
                names.addAll(getAllNamesRecursive(sub));
            }
        }
        return names;
    }


    public Set<String> getFilterableAttributes(String categoryName) {
        Map<String, AttributeDefinition> defs = categoryName == null
                ? globalAttributeDefinitionsCache
                : attributeDefinitionsCache.get(categoryName.toLowerCase());

        if (defs == null) {
            return Collections.emptySet();
        }

        Set<String> filterable = new HashSet<>();
        for (AttributeDefinition def : defs.values()) {
            if (def.isFilterable()) {
                filterable.add(def.getName());
            }
        }
        return filterable;
    }

    public Optional<AttributeDefinition> getAttributeDefinition(String categoryName, String attributeName) {
        if (attributeName == null) {
            return Optional.empty();
        }

        if (categoryName == null) {
            return Optional.ofNullable(globalAttributeDefinitionsCache.get(attributeName));
        }

        Map<String, AttributeDefinition> categoryDefs = attributeDefinitionsCache.get(categoryName.toLowerCase());
        if (categoryDefs == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(categoryDefs.get(attributeName));
    }

    public Map<String, AttributeDefinition> getAttributeDefinitions(String categoryName) {
        if (categoryName == null) {
            return globalAttributeDefinitionsCache;
        }
        return attributeDefinitionsCache.getOrDefault(categoryName.toLowerCase(), Collections.emptyMap());
    }

    public FilterType getFilterType(String categoryName, String attributeName) {
        return getAttributeDefinition(categoryName, attributeName)
                .map(AttributeDefinition::getFilterType)
                .orElse(FilterType.NONE);
    }

    public List<String> getAllDescendantNames(String categoryName) {
        if (categoryName == null) return Collections.emptyList();
        return categoryHierarchyCache.getOrDefault(categoryName.toLowerCase(), List.of(categoryName));
    }
}
