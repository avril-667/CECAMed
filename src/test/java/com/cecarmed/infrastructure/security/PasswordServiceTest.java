package com.cecarmed.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordServiceTest {

    @Test
    @DisplayName("Debe generar un hash BCrypt válido y verificar la contraseña correcta")
    void shouldHashAndVerifyPasswordSuccessfully() {
        String plainPassword = "SuperSecretPassword123!";
        String hash = PasswordService.hashPassword(plainPassword);

        assertThat(hash).isNotNull();
        assertThat(hash).startsWith("$2a$12$"); // Factor de costo 12 verificado

        boolean verified = PasswordService.verifyPassword(plainPassword, hash);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Debe rechazar una contraseña incorrecta")
    void shouldRejectIncorrectPassword() {
        String plainPassword = "CorrectPassword123!";
        String hash = PasswordService.hashPassword(plainPassword);

        boolean verified = PasswordService.verifyPassword("WrongPassword123!", hash);
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta hashear contraseña vacía o nula")
    void shouldThrowExceptionWhenPasswordIsBlank() {
        assertThatThrownBy(() -> PasswordService.hashPassword(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PasswordService.hashPassword(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
