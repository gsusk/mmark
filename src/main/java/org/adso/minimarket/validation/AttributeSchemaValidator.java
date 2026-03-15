package org.adso.minimarket.validation;

import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.repository.jpa.CategoryRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AttributeSchemaValidator {

    private final CategoryRepository categoryRepository;
    
    private final Map<String, Set<String>> filterableAttributesCache = new ConcurrentHashMap<>();
    
    private final Map<String, Map<String, AttributeDefinition>> attributeDefinitionsCache = new ConcurrentHashMap<>();
    
    private final Set<String> globalFilterableAttributesCache = new HashSet<>();

    private final Map<String, AttributeDefinition> globalAttributeDefinitionsCache = new ConcurrentHashMap<>();

    public AttributeSchemaValidator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadSchema() {
        // Cargamos todos los atributos globalmente en memoria la primera vez que arranca Spring Boot.
        // Esto es re importante para no destrozar la base de datos con peticiones cada vez
        // que alguien cambie los filtros del frontend o cree un producto.
        log.info("loading attribute schema into memry..");
        List<Category> allCategories = categoryRepository.findAll();

        for (Category category : allCategories) {
            String categoryKey = category.getName().toLowerCase();
            
            Set<String> filterableAttributes = new HashSet<>();
            Map<String, AttributeDefinition> definitions = new HashMap<>();
            
            List<Map<String, Object>> definitionMaps = category.getAllAttributeDefinitions();

            if (definitionMaps != null) {
                for (Map<String, Object> defMap : definitionMaps) {
                    try {
                        AttributeDefinition def = AttributeDefinition.fromMap(defMap);
                        String attrName = def.getName();
                        
                        definitions.put(attrName, def);
                        
                        if (def.isFilterable()) {
                            filterableAttributes.add(attrName);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse attribute definition for category '{}': {}", 
                                category.getName(), e.getMessage());
                    }
                }
            }

            filterableAttributesCache.put(categoryKey, filterableAttributes);
            attributeDefinitionsCache.put(categoryKey, definitions);
            globalAttributeDefinitionsCache.putAll(definitions);
            globalFilterableAttributesCache.addAll(filterableAttributes);
        }
        
        log.info("Loaded schema for {} categories with {} total unique filterable attributes.",
                attributeDefinitionsCache.size(), 
                globalFilterableAttributesCache.size());
    }



    public Set<String> getFilterableAttributes(String categoryName) {
        if (categoryName == null) {
            return globalFilterableAttributesCache;
        }
        return filterableAttributesCache.getOrDefault(categoryName.toLowerCase(), globalFilterableAttributesCache);
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
}
