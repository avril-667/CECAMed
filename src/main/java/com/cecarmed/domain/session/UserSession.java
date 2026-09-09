package com.cecarmed.domain.session;

import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;

/**
 * Gestor de la sesión del usuario actual en memoria.
 * Permite a cualquier capa consultar de forma segura quién está operando en la aplicación.
 */
public final class UserSession {

    private static volatile Usuario currentUser;

    private UserSession() {
    }

    public static void login(Usuario usuario) {
        currentUser = usuario;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static Usuario getCurrentUser() {
        return currentUser;
    }

    public static Long getCurrentUserId() {
        return currentUser != null ? currentUser.id() : null;
    }

    public static Rol getCurrentUserRole() {
        return currentUser != null ? currentUser.rol() : null;
    }

    public static boolean hasRole(Rol rol) {
        return currentUser != null && currentUser.rol() == rol;
    }
}
