package org.adso.minimarket.unit.validation;

import org.adso.minimarket.exception.AttributeValidationException;
import org.adso.minimarket.validation.ProductAttributeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Validar con atributos válidos debería pasar")
    void validar_conAtributosValidos_deberiaPasar() {
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
    @DisplayName("Validar con atributo requerido faltante debería lanzar excepción")
    void validar_conAtributoRequeridoFaltante_deberiaLanzarExcepcion() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true)
        );

        Map<String, Object> attributes = Map.of();
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("is required"));
    }

    @Test
    @DisplayName("Validar con tipo incorrecto debería lanzar excepción")
    void validar_conTipoIncorrecto_deberiaLanzarExcepcion() {
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
    @DisplayName("Validar con string en blanco debería lanzar excepción")
    void validar_conStringEnBlanco_deberiaLanzarExcepcion() {
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
    @DisplayName("Validar con valor enum inválido debería lanzar excepción")
    void validar_conValorEnumInvalido_deberiaLanzarExcepcion() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true, "options", List.of("S", "M", "L"))
        );

        Map<String, Object> attributes = Map.of("size", "XL");
        AttributeValidationException exception = assertThrows(
                AttributeValidationException.class,
                () -> validator.validate(attributes, definitions)
        );

        assertEquals(1, exception.getValidationErrors().size());
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("must be one of"));
    }

    @Test
    @DisplayName("Validar con número por debajo del mínimo debería lanzar excepción")
    void validar_conNumeroPorDebajoDelMinimo_deberiaLanzarExcepcion() {
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
    @DisplayName("Validar con número por encima del máximo debería lanzar excepción")
    void validar_conNumeroPorEncimaDelMaximo_deberiaLanzarExcepcion() {
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
    @DisplayName("Validar con atributo desconocido debería lanzar excepción")
    void validar_conAtributoDesconocido_deberiaLanzarExcepcion() {
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
        assertTrue(exception.getValidationErrors().get(0).getErrorMessage().contains("not defined in the category schema"));
    }

    @Test
    @DisplayName("Validar con múltiples errores debería acumular todos")
    void validar_conMultiplesErrores_deberiaAcumularTodos() {
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
    @DisplayName("Validar con tipo booleano debería validar correctamente")
    void validar_conTipoBooleano_deberiaValidarCorrectamente() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "inStock", "type", "boolean", "required", true)
        );

        Map<String, Object> validAttributes = Map.of("inStock", true);
        Map<String, Object> invalidAttributes = Map.of("inStock", "yes");

        assertDoesNotThrow(() -> validator.validate(validAttributes, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(invalidAttributes, definitions));
    }

    @Test
    @DisplayName("Validar con opciones enum numéricas debería manejar correctamente")
    void validar_conOpcionesEnumNumericas_deberiaManejarCorrectamente() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "storage_gb", "type", "number", "required", true, "options", List.of(128, 256, 512))
        );

        Map<String, Object> validAttributes = Map.of("storage_gb", 256);
        Map<String, Object> invalidAttributes = Map.of("storage_gb", 64);

        assertDoesNotThrow(() -> validator.validate(validAttributes, definitions));
        assertThrows(AttributeValidationException.class, () -> validator.validate(invalidAttributes, definitions));
    }

    @Test
    @DisplayName("Validar con atributo opcional debería permitir que falte")
    void validar_conAtributoOpcional_deberiaPermitirQueFalte() {
        List<Map<String, Object>> definitions = List.of(
                Map.of("name", "size", "type", "string", "required", true),
                Map.of("name", "color", "type", "string", "required", false)
        );

        Map<String, Object> attributes = Map.of("size", "M");

        assertDoesNotThrow(() -> validator.validate(attributes, definitions));
    }

    @Test
    @DisplayName("Validar con valor nulo debería lanzar excepción")
    void validar_conValorNulo_deberiaLanzarExcepcion() {
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
    @DisplayName("Validar con definiciones vacías debería pasar")
    void validar_conDefinicionesVacias_deberiaPasar() {
        List<Map<String, Object>> definitions = List.of();
        Map<String, Object> attributes = Map.of("anything", "value");

        assertDoesNotThrow(() -> validator.validate(attributes, definitions));
    }

    @Test
    @DisplayName("Validar con definiciones nulas debería pasar")
    void validar_conDefinicionesNulas_deberiaPasar() {
        Map<String, Object> attributes = Map.of("anything", "value");

        assertDoesNotThrow(() -> validator.validate(attributes, null));
    }

    @Test
    @DisplayName("Validar con números decimales debería validar el rango correctamente")
    void validar_conNumerosDecimales_deberiaValidarRangoCorrectamente() {
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
