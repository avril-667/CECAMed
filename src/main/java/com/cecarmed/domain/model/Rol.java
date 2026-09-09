package com.cecarmed.domain.model;

/**
 * Roles del sistema CECAMed para Control de Acceso Basado en Roles (RBAC).
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    MEDICO("Médico"),
    RECEPCIONISTA("Recepcionista");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static Rol fromString(String rolStr) {
        for (Rol r : values()) {
            if (r.name().equalsIgnoreCase(rolStr)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rol no reconocido: " + rolStr);
    }
}
