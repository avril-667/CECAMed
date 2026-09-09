package com.cecarmed.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Objects;

/**
 * Representación inmutable del Paciente en CECAMed (Java 21 Record).
 */
public record Paciente(
        Long id,
        String expedienteNumero,
        String nombre,
        String primerApellido,
        String segundoApellido,
        LocalDate fechaNacimiento,
        Genero genero,
        String curp,
        String telefono,
        String email,
        String direccion,
        String alergias,
        String antecedentesPatologicos,
        String antecedentesNoPatologicos,
        String antecedentesHeredofamiliares,
        boolean activo,
        OffsetDateTime fechaRegistro,
        OffsetDateTime fechaActualizacion
) {
    public Paciente {
        Objects.requireNonNull(nombre, "El nombre no puede ser nulo");
        Objects.requireNonNull(primerApellido, "El primer apellido no puede ser nulo");
        Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento no puede ser nula");
        Objects.requireNonNull(genero, "El género no puede ser nulo");
        Objects.requireNonNull(telefono, "El teléfono no puede ser nulo");
    }

    public String getNombreCompleto() {
        if (segundoApellido != null && !segundoApellido.isBlank()) {
            return nombre + " " + primerApellido + " " + segundoApellido;
        }
        return nombre + " " + primerApellido;
    }

    public int getEdad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public static Paciente nuevo(
            String expedienteNumero,
            String nombre,
            String primerApellido,
            String segundoApellido,
            LocalDate fechaNacimiento,
            Genero genero,
            String curp,
            String telefono,
            String email,
            String direccion,
            String alergias,
            String antecedentesPatologicos,
            String antecedentesNoPatologicos,
            String antecedentesHeredofamiliares
    ) {
        return new Paciente(
                null,
                expedienteNumero,
                nombre,
                primerApellido,
                segundoApellido,
                fechaNacimiento,
                genero,
                curp,
                telefono,
                email,
                direccion,
                alergias,
                antecedentesPatologicos,
                antecedentesNoPatologicos,
                antecedentesHeredofamiliares,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
