package org.adso.minimarket.unit;

import org.adso.minimarket.constant.UserRoutes;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.service.AppUserDetailsServiceImpl;
import org.adso.minimarket.service.JwtService;
import org.adso.minimarket.service.UserService;
import org.adso.minimarket.unit.api.UserControllerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TODO:
 *      -  Cambiar el CommandLineRunner para no depender de perfiles:
 *      <a href="https://www.baeldung.com/spring-junit-prevent-runner-beans-testing-execution">Guia</a>
 */

@ActiveProfiles("test")
@WebMvcTest(UserControllerImpl.class)
@ExtendWith(SpringExtension.class)
public class UserControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsServiceImpl userDetailsService;

    @Test
    void getUserById_withValidId_returns201() throws Exception {
        Long userId = 1L;
        User response = new User(
                "Santiago",
                "Atehortua",
                "test@test.com",
                "12345"
        );


        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get(UserRoutes.GET_USER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Santiago"))
                .andExpect(jsonPath("$.last_name").value("Atehortua"))
                .andExpect(jsonPath("$.email").value("test@test.com"));

        verify(userService).getUserById(any(Long.class));
    }

    @Test
    void getUserById_shouldReturn400_whenIdIsNotNumber() throws Exception {
        mockMvc.perform(get(UserRoutes.GET_USER, "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_shouldReturn404_whenIdIsLessThan1() throws Exception {
        mockMvc.perform(get(UserRoutes.GET_USER, -1))
                .andExpect(status().isBadRequest());
    }
}
