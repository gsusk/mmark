package org.adso.minimarket.unit.controller.api;

import org.adso.minimarket.constant.UserRoutes;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.service.AppUserDetailsServiceImpl;
import org.adso.minimarket.service.JwtService;
import org.adso.minimarket.mappers.UserMapper;
import org.adso.minimarket.service.UserService;
import org.adso.minimarket.controller.api.UserControllerImpl;
import org.junit.jupiter.api.DisplayName;
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

    @MockitoBean
    private UserMapper userMapper;

    @Test
    @DisplayName("obtenerYo sin autenticación retorna No Autorizado o Prohibido")
    void obtenerYo_sinAutenticacion_retornaNoAutorizadoOProhibido() throws Exception {
        mockMvc.perform(get(UserRoutes.GET_USER))
                .andExpect(status().is4xxClientError());
    }
}
