package org.adso.minimarket.validation;

import org.adso.minimarket.exception.AttributeValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductAttributeValidatorTest {

    private ProductAttributeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductAttributeValidator();
    }

    @Test
    void validate_withValidAttributes_shouldPass() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true, "options", List.of("S", "M", "L")),
                Map.of("name", "color", "type", "string", "required", false)
        );

        Map<String, Object> attributes = Map.of(
                "size", "M",
                "color", "blue"
        );

        assertDoesNotThrow(() -> validator.validate(attributes, definitions));
    }

    @Test
    void validate_withMissingRequiredAttribute_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true)
        );

        Map<String, Object> attributes = Map.of("color", "blue");
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("Missing required attribute"));
    }

    @Test
    void validate_withWrongType_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "price", "type", "number", "required", true)
        );

        Map<String, Object> attributes = Map.of("price", "not a number");
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("must be a number"));
    }

    @Test
    void validate_withBlankString_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "name", "type", "string", "required", true)
        );

        Map<String, Object> attributes = Map.of("name", "   ");
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("cannot be blank"));
    }

    @Test
    void validate_withInvalidEnumValue_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true, "options", List.of("S", "M", "L"))
        );

        Map<String, Object> attributes = Map.of("size", "XL");
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("invalid value"));
    }

    @Test
    void validate_withNumberBelowMin_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "age", "type", "number", "required", true, "min", 18)
        );

        Map<String, Object> attributes = Map.of("age", 15);
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("must be >= 18"));
    }

    @Test
    void validate_withNumberAboveMax_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "age", "type", "number", "required", true, "max", 100)
        );

        Map<String, Object> attributes = Map.of("age", 150);
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("must be <= 100"));
    }

    @Test
    void validate_withUnknownAttribute_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true)
        );

        Map<String, Object> attributes = Map.of(
                "size", "M",
                "unknownField", "value"
        );
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("Unknown attribute"));
    }

    @Test
    void validate_withMultipleErrors_shouldAccumulateAll() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true),
                Map.of("name", "price", "type", "number", "required", true, "min", 10),
                Map.of("name", "available", "type", "boolean", "required", true)
        );

        Map<String, Object> attributes = Map.of(
                "price", 5,
                "available", "not a boolean", 
                "extraField", "value"
        );
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(4, exception.getValidationErrors().size());
    }

    @Test
    void validate_withBooleanType_shouldValidateCorrectly() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "inStock", "type", "boolean", "required", true)
        );

        Map<String, Object> validAttributes = Map.of("inStock", true);
        Map<String, Object> invalidAttributes = Map.of("inStock", "yes");

        assertDoesNotThrow(() -> validator.validate(validAttributes, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(invalidAttributes, definitions));
    }

    @Test
    void validate_withNumberEnumOptions_shouldHandleCorrectly() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "storage_gb", "type", "number", "required", true, "options", List.of(128, 256, 512))
        );

        Map<String, Object> validAttributes = Map.of("storage_gb", 256);
        Map<String, Object> invalidAttributes = Map.of("storage_gb", 64);

        assertDoesNotThrow(() -> validator.validate(validAttributes, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(invalidAttributes, definitions));
    }

    @Test
    void validate_withOptionalAttribute_shouldAllowMissing() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true),
                Map.of("name", "color", "type", "string", "required", false)
        );

        Map<String, Object> attributes = Map.of("size", "M");

        assertDoesNotThrow(() -> validator.validate(attributes, definitions));
    }

    @Test
    void validate_withNullValue_shouldThrowException() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "name", "type", "string", "required", true)
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", null);

        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("cannot be null"));
    }

    @Test
    void validate_withEmptyDefinitions_shouldPass() {
        List<Map<String, Object>> definitions = List.of();
        Map<String, Object> attributes = Map.of("anything", "value");

        assertDoesNotThrow(() -> validator.validate(attributes, definitions));
    }

    @Test
    void validate_withNullDefinitions_shouldPass() {
        Map<String, Object> attributes = Map.of("anything", "value");

        assertDoesNotThrow(() -> validator.validate(attributes, null));
    }

    @Test
    void validate_withDecimalNumbers_shouldValidateRangeCorrectly() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "weight", "type", "number", "required", true, "min", 0.1, "max", 100.5)
        );

        Map<String, Object> validAttributes = Map.of("weight", 50.5);
        Map<String, Object> tooLow = Map.of("weight", 0.05);
        Map<String, Object> tooHigh = Map.of("weight", 101.0);

        assertDoesNotThrow(() -> validator.validate(validAttributes, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(tooLow, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(tooHigh, definitions));
    }
}
