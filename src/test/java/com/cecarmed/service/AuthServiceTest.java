package com.cecarmed.service;

import com.cecarmed.domain.exception.AuthenticationException;
import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.infrastructure.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, auditoriaRepository);
        UserSession.logout();
    }

    @Test
    @DisplayName("Debe autenticar correctamente con credenciales válidas")
    void shouldAuthenticateSuccessfullyWithValidCredentials() {
        String username = "doctor1";
        String password = "DoctorPassword123!";
        String hash = PasswordService.hashPassword(password);

        Usuario mockUser = new Usuario(
                1L,
                "Dr. Roberto Gómez",
                username,
                hash,
                Rol.MEDICO,
                "CED123456",
                "Medicina General",
                "roberto@cecarmed.com",
                "555-1234",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        Usuario result = authService.login(username, password, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(username);
        assertThat(UserSession.isAuthenticated()).isTrue();
        assertThat(UserSession.getCurrentUser()).isEqualTo(mockUser);
        verify(auditoriaRepository, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Debe fallar autenticación si el usuario no existe")
    void shouldFailWhenUserDoesNotExist() {
        when(usuarioRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("desconocido", "somePass", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Credenciales inválidas");

        assertThat(UserSession.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Debe fallar autenticación si la contraseña es incorrecta")
    void shouldFailWhenPasswordIsWrong() {
        String username = "recepcionista1";
        String correctPassword = "CorrectPass123!";
        String hash = PasswordService.hashPassword(correctPassword);

        Usuario mockUser = new Usuario(
                2L,
                "Laura Flores",
                username,
                hash,
                Rol.RECEPCIONISTA,
                null,
                null,
                "laura@cecarmed.com",
                "555-9876",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.login(username, "WrongPass123!", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Credenciales inválidas");

        assertThat(UserSession.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Debe fallar autenticación si el usuario está inactivo")
    void shouldFailWhenUserIsInactive() {
        String username = "usuarioInactivo";
        String password = "Password123!";
        String hash = PasswordService.hashPassword(password);

        Usuario inactiveUser = new Usuario(
                3L,
                "Usuario Inactivo",
                username,
                hash,
                Rol.MEDICO,
                null,
                null,
                "inactivo@cecarmed.com",
                "555-0000",
                false, // inactivo
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(username, password, "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("desactivada");

        assertThat(UserSession.isAuthenticated()).isFalse();
    }
}
