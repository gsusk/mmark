package org.adso.minimarket.unit.service;

import org.adso.minimarket.dto.BasicUser;
import org.adso.minimarket.dto.RegisterRequest;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.mappers.UserMapper;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.UserRepository;
import org.adso.minimarket.service.UserService;
import org.adso.minimarket.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UserServiceImplTest {

    @TestConfiguration
    static class UserServiceImplTestContextConfiguration {
        @Bean
        public UserService userService(UserRepository userRepository, UserMapper userMapper) {
            return new UserServiceImpl(userRepository, userMapper);
        }
    }

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    public UserService userService;

    @MockitoBean
    public UserMapper userMapper;

    @Test
    @DisplayName("Crear usuario con solicitud válida guarda y retorna el usuario")
    void crearUsuario_conSolicitudValida_guardaYRetornaUsuario() {
        RegisterRequest req = new RegisterRequest("Jorge", "Contreras", "jorge@test.com", "password123");
        User mockUser = new User("Jorge", "Contreras", "jorge@test.com", "password123");
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = userService.createUser(req);

        assertEquals("Jorge", result.getName());
        assertEquals("jorge@test.com", result.getEmail());
        assertEquals(1L, result.getId());
        verify(userRepository).save(any(User.class));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Obtener usuario por email cuando se encuentra retorna el usuario mapeado")
    void obtenerUsuarioPorEmail_cuandoSeEncuentra_retornaUsuarioMapeado() {
        User user = new User("Test", "User", "test@test.com", "pass");
        BasicUser basicUser = BasicUser.builder()
                .id(1L).firstName("Test").lastName("User").email("test@test.com").build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(basicUser);

        BasicUser result = userService.getUserByEmail("test@test.com");

        assertEquals("test@test.com", result.getEmail());
        assertEquals("Test", result.getFirstName());
        verify(userRepository).findByEmail("test@test.com");
        verify(userMapper).toResponseDto(user);
    }

    @Test
    @DisplayName("Obtener usuario por email cuando no se encuentra lanza NotFoundException")
    void obtenerUsuarioPorEmail_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserByEmail("missing@test.com"));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Obtener usuario por ID cuando se encuentra retorna el usuario")
    void obtenerUsuarioPorId_cuandoSeEncuentra_retornaUsuario() {
        User user = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Test", result.getName());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Obtener usuario por ID cuando no se encuentra lanza NotFoundException")
    void obtenerUsuarioPorId_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserById(99L));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Obtener usuario interno por email cuando se encuentra retorna la entidad de usuario")
    void obtenerUsuarioInternoPorEmail_cuandoSeEncuentra_retornaEntidadUsuario() {
        User user = new User("Test", "User", "test@test.com", "pass");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = userService.getUserInternalByEmail("test@test.com");

        assertEquals("test@test.com", result.getEmail());
        verify(userRepository).findByEmail("test@test.com");
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Obtener usuario interno por email cuando no se encuentra lanza NotFoundException")
    void obtenerUsuarioInternoPorEmail_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getUserInternalByEmail("missing@test.com"));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Eliminar usuario llama al repositorio para remover por ID")
    void eliminarUsuario_llamaARepositorioRemoverPorId() {
        doNothing().when(userRepository).removeUserById(1L);

        userService.deleteUser(1L);

        verify(userRepository).removeUserById(1L);
    }
}
