package com.cecarmed.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Bloqueo de agenda (vacaciones, ausencias, congresos) de un médico (Java 21 Record).
 */
public record BloqueoAgenda(
        Long id,
        Long medicoId,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String motivo,
        OffsetDateTime fechaCreacion
) {
    public BloqueoAgenda {
        Objects.requireNonNull(medicoId, "El médico es obligatorio");
        Objects.requireNonNull(fechaHoraInicio, "La fecha y hora de inicio es obligatoria");
        Objects.requireNonNull(fechaHoraFin, "La fecha y hora de fin es obligatoria");
        Objects.requireNonNull(motivo, "El motivo del bloqueo es obligatorio");

        if (!fechaHoraInicio.isBefore(fechaHoraFin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin del bloqueo");
        }
    }

    public static BloqueoAgenda nuevo(Long medicoId, OffsetDateTime fechaHoraInicio,
                                      OffsetDateTime fechaHoraFin, String motivo) {
        return new BloqueoAgenda(null, medicoId, fechaHoraInicio, fechaHoraFin, motivo, OffsetDateTime.now());
    }
}
