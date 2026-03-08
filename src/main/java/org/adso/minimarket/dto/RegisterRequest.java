package org.adso.minimarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
        @NotBlank(message = "required")
        @Size(max = 255, message = "too long")
        String firstName,

        @NotBlank(message = "required")
        String lastName,

        @Email(message = "Must be a valid email")
        @NotBlank(message = "required")
        String email,

        @Size(min = 6, message = "should have at least 6 characters")
        @NotBlank(message = "required")
        String password
) {
    @Override
    public String firstName() {
        return firstName;
    }

    @Override
    public String lastName() {
        return lastName;
    }

    @Override
    public String email() {
        return email;
    }

    @Override
    public String password() {
        return password;
    }
}

