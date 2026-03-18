package org.adso.minimarket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.adso.minimarket.models.user.Role;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicUser {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
