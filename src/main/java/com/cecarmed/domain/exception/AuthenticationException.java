package com.cecarmed.domain.exception;

/**
 * Excepción lanzada cuando la autenticación falla (credenciales incorrectas, usuario inactivo, etc.).
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
