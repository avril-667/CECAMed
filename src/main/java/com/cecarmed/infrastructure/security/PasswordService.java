package com.cecarmed.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Servicio criptográfico para hashing y verificación de contraseñas con BCrypt.
 * Cumple con el estándar de seguridad de CECAMed con factor de costo >= 12.
 */
public final class PasswordService {

    public static final int BCRYPT_COST_FACTOR = 12;

    private PasswordService() {
    }

    /**
     * Genera un hash seguro BCrypt con factor de costo 12.
     *
     * @param plainPassword Contraseña en texto plano
     * @return Cadena con el hash BCrypt
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST_FACTOR, plainPassword.toCharArray());
    }

    /**
     * Verifica una contraseña en texto plano contra su hash BCrypt.
     *
     * @param plainPassword Contraseña en texto plano a verificar
     * @param bcryptHash     Hash BCrypt almacenado en la base de datos
     * @return true si la contraseña coincide, false en caso contrario
     */
    public static boolean verifyPassword(String plainPassword, String bcryptHash) {
        if (plainPassword == null || bcryptHash == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), bcryptHash);
        return result.verified;
    }
}
