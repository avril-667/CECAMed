package com.cecarmed.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Representación inmutable de una Cita Médica en CECAMed (Java 21 Record).
 */
public record Cita(
        Long id,
        Long pacienteId,
        Long medicoId,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String motivoConsulta,
        EstadoCita estado,
        String googleCalendarEventId,
        String notas,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
    public Cita {
        Objects.requireNonNull(pacienteId, "El paciente es obligatorio");
        Objects.requireNonNull(medicoId, "El médico es obligatorio");
        Objects.requireNonNull(fechaHoraInicio, "La fecha y hora de inicio es obligatoria");
        Objects.requireNonNull(fechaHoraFin, "La fecha y hora de fin es obligatoria");
        Objects.requireNonNull(motivoConsulta, "El motivo de la consulta es obligatorio");
        Objects.requireNonNull(estado, "El estado de la cita es obligatorio");

        if (!fechaHoraInicio.isBefore(fechaHoraFin)) {
            throw new IllegalArgumentException("La fecha y hora de inicio debe ser anterior a la de fin");
        }
    }

    public static Cita nueva(Long pacienteId, Long medicoId, OffsetDateTime fechaHoraInicio,
                             OffsetDateTime fechaHoraFin, String motivoConsulta, String notas) {
        return new Cita(
                null,
                pacienteId,
                medicoId,
                fechaHoraInicio,
                fechaHoraFin,
                motivoConsulta,
                EstadoCita.PROGRAMADA,
                null,
                notas,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
