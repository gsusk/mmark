package org.adso.minimarket.handler;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.adso.minimarket.dto.ErrorResponse;
import org.adso.minimarket.exception.AttributeValidationException;
import org.adso.minimarket.exception.BaseException;
import org.adso.minimarket.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "uk_user_email", "Email already in use",
            "idx_product_slug", "Ya existe un producto registrado con este nombre o slug",
            "product_slug", "Ya existe un producto registrado con este nombre o slug",
            "uk_category_name", "Ya existe una categoría con este nombre"
    );

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            WebRequest request
    ) {
        HttpStatus status = ex.getStatus();

        if (status.is5xxServerError()) {
            log.error("Server error: {}", ex.getMessage(), ex);
        } else {
            log.warn("Client error: {} - {}", ex.getErrorCode(), ex.getMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                ex.getTitle(),
                ex.getMessage(),
                ex.getErrorCode().name(),
                LocalDateTime.now(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(AttributeValidationException.class)
    public ResponseEntity<ErrorResponse> handleAttributeValidation(
            AttributeValidationException ex,
            WebRequest request
    ) {
        log.warn("Attribute validation failed: {} error(s)", ex.getValidationErrors().size());

        List<ErrorResponse.ValidationError> validationErrors = ex.getValidationErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(
                        error.getAttributeName(),
                        error.getErrorMessage(),
                        error.getProvidedValue()
                ))
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                ex.getMessage(),
                ex.getErrorCode().name(),
                LocalDateTime.now(),
                getRequestPath(request),
                validationErrors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .toList();

        String validationSummary = validationErrors.stream()
                .map(err -> err.field() + ": " + err.message())
                .collect(java.util.stream.Collectors.joining(", "));
        String message = validationSummary.isBlank() ? "Validation Failed" : validationSummary;

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message,
                ErrorCode.VALIDATION_FAILED.name(),
                LocalDateTime.now(),
                getRequestPath(request),
                validationErrors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        log.warn("Constraint violation: {} violations", ex.getConstraintViolations().size());

        List<ErrorResponse.ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.ValidationError(
                        extractFieldName(violation.getPropertyPath()),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                ex.getMessage(),
                ErrorCode.VALIDATION_CONSTRAINT_VIOLATION.name(),
                LocalDateTime.now(),
                getRequestPath(request),
                validationErrors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        String message = Optional.ofNullable(getRootCause(ex))
                .map(Throwable::getMessage)
                .orElse("")
                .toLowerCase();

        String detail = "A database constraint was violated";
        for (Map.Entry<String, String> entry : CONSTRAINT_MESSAGES.entrySet()) {
            if (message.contains(entry.getKey())) {
                detail = entry.getValue();
                break;
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                detail,
                ErrorCode.RESOURCE_CONFLICT.name(),
                LocalDateTime.now(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {
        log.warn("Type mismatch: {} for parameter '{}'", ex.getValue(), ex.getName());

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                ErrorCode.VALIDATION_FAILED.name(),
                LocalDateTime.now(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                ErrorCode.SERVER_INTERNAL_ERROR.name(),
                LocalDateTime.now(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }

    private String extractFieldName(Path propertyPath) {
        String field = null;
        for (Path.Node node : propertyPath) {
            field = node.getName();
        }
        return field != null ? field : propertyPath.toString();
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
