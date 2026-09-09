package com.cecarmed.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Representación inmutable del Usuario en el dominio CECAMed (Java 21 Record).
 */
public record Usuario(
        Long id,
        String nombreCompleto,
        String username,
        String passwordHash,
        Rol rol,
        String cedulaProfesional,
        String especialidad,
        String email,
        String telefono,
        boolean activo,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
    public Usuario {
        Objects.requireNonNull(nombreCompleto, "nombreCompleto no puede ser nulo");
        Objects.requireNonNull(username, "username no puede ser nulo");
        Objects.requireNonNull(passwordHash, "passwordHash no puede ser nulo");
        Objects.requireNonNull(rol, "rol no puede ser nulo");
    }

    /**
     * Constructor de conveniencia para crear un nuevo usuario antes de persistir (sin ID ni fechas).
     */
    public static Usuario nuevo(String nombreCompleto, String username, String passwordHash, Rol rol,
                                String cedulaProfesional, String especialidad, String email, String telefono) {
        return new Usuario(null, nombreCompleto, username, passwordHash, rol,
                cedulaProfesional, especialidad, email, telefono, true, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
