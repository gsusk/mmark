package org.adso.minimarket.validation;

import org.adso.minimarket.exception.AttributeValidationException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProductAttributeValidator {

    public void validate(Map<String, Object> attributes, List<Map<String, Object>> definitionMaps) {
        // Esta es la barrera de defensa cuando un admin intenta guardar un producto.
        // Comparamos el JSON que manda contra las reglas maestras de esa categoria en especifico.
        if (definitionMaps == null || definitionMaps.isEmpty()) {
            return;
        }

        List<ValidationError> errors = new ArrayList<>();

        List<AttributeDefinition> definitions;
        try {
            definitions = definitionMaps.stream()
                    .map(AttributeDefinition::fromMap)
                    .toList();
        } catch (Exception e) {
            errors.add(new ValidationError("schema", "Invalid attribute schema: " + e.getMessage()));
            throw new AttributeValidationException(errors);
        }

        Set<String> definedAttributeNames = new HashSet<>();
        for (AttributeDefinition def : definitions) {
            definedAttributeNames.add(def.getName());
        }

        for (AttributeDefinition def : definitions) {
            validateAttribute(def, attributes, errors);
        }

        if (attributes != null) {
            for (String attrName : attributes.keySet()) {
                if (!definedAttributeNames.contains(attrName)) {
                    errors.add(new ValidationError(
                            "specifications[" + attrName + "]",
                            "Attribute is not defined in the category schema",
                            attributes.get(attrName),
                            "UnknownAttribute"
                    ));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AttributeValidationException(errors);
        }
    }

    private void validateAttribute(AttributeDefinition def, Map<String, Object> attributes, List<ValidationError> errors) {
        String name = def.getName();
        String fieldName = "specifications[" + name + "]";
        Object value = attributes != null ? attributes.get(name) : null;

        if (def.isRequired()) {
            if (attributes == null || !attributes.containsKey(name)) {
                errors.add(new ValidationError(fieldName, "is required", null, "Required"));
                return;
            }
            if (value == null) {
                errors.add(new ValidationError(fieldName, "cannot be null", value, "NotNull"));
                return;
            }
        }

        if (value == null || (attributes != null && !attributes.containsKey(name))) {
            return;
        }

        validateType(def, fieldName, value, errors);

        if (def.hasOptions()) {
            validateEnum(def, fieldName, value, errors);
        }

        if (def.getType() == AttributeType.NUMBER) {
            validateRange(def, fieldName, value, errors);
        }
    }

    private void validateType(AttributeDefinition def, String fieldName, Object value, List<ValidationError> errors) {
        AttributeType expectedType = def.getType();

        boolean isValid = switch (expectedType) {
            case STRING, ENUM -> value instanceof String && !((String) value).isBlank();
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
        };

        if (!isValid) {
            // Un chequeo de tipos para asegurarnos de que no estemos guardando strings
            // en propiedades numericas que romperian Elasticsearch a futuro.
            String actualType = getActualType(value);
            String expectedTypeStr = expectedType.name().toLowerCase();
            
            if ((expectedType == AttributeType.STRING || expectedType == AttributeType.ENUM) && value instanceof String && ((String) value).isBlank()) {
                errors.add(new ValidationError(
                        fieldName,
                        "cannot be blank",
                        value,
                        "NotBlank"
                ));
            } else {
                errors.add(new ValidationError(
                        fieldName,
                        "must be a " + expectedTypeStr + ", but got " + actualType,
                        value,
                        "TypeMismatch"
                ));
            }
        }
    }

    private void validateEnum(AttributeDefinition def, String fieldName, Object value, List<ValidationError> errors) {
        List<?> options = def.getOptions();

        boolean found = false;

        if (value instanceof Number) {
            double valueDouble = ((Number) value).doubleValue();
            found = options.stream()
                    .filter(opt -> opt instanceof Number)
                    .map(opt -> ((Number) opt).doubleValue())
                    .anyMatch(optVal -> Double.compare(optVal, valueDouble) == 0);
        } else {
            found = options.contains(value);
        }

        if (!found) {
            errors.add(new ValidationError(
                    fieldName,
                    "must be one of: " + options,
                    value,
                    "InList"
            ));
        }
    }

    private void validateRange(AttributeDefinition def, String fieldName, Object value, List<ValidationError> errors) {
        if (!(value instanceof Number)) {
            return;
        }

        double doubleVal = ((Number) value).doubleValue();

        if (def.hasMin()) {
            double min = def.getMin().doubleValue();
            if (doubleVal < min) {
                errors.add(new ValidationError(
                        fieldName,
                        "must be >= " + min,
                        value,
                        "Min"
                ));
            }
        }

        if (def.hasMax()) {
            double max = def.getMax().doubleValue();
            if (doubleVal > max) {
                errors.add(new ValidationError(
                        fieldName,
                        "must be <= " + max,
                        value,
                        "Max"
                ));
            }
        }
    }

    private String getActualType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return ((String) value).isBlank() ? "blank string" : "string";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return value.getClass().getSimpleName();
    }
}
