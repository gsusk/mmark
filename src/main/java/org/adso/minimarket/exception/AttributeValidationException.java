package org.adso.minimarket.exception;

import lombok.Getter;
import org.adso.minimarket.validation.ValidationError;

import java.util.List;

@Getter
public class AttributeValidationException extends BadRequestException {
    private final List<ValidationError> validationErrors;

    public AttributeValidationException(List<ValidationError> validationErrors) {
        super(buildMessage(validationErrors), ErrorCode.VALIDATION_ATTRIBUTE_FAILED);
        this.validationErrors = validationErrors;
    }

    private static String buildMessage(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Attribute validation failed";
        }
        if (errors.size() == 1) {
            ValidationError err = errors.get(0);
            return String.format("%s %s", err.getAttributeName(), err.getErrorMessage());
        }
        return "Attribute validation failed: " + errors.stream()
                .map(err -> String.format("%s %s", err.getAttributeName(), err.getErrorMessage()))
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
