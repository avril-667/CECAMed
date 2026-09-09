package com.cecarmed.domain.model;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Configuración de horario de atención médica semanal (Java 21 Record).
 */
public record HorarioAtencion(
        Long id,
        Long medicoId,
        int diaSemana, // 1 = Lunes ... 7 = Domingo (ISO-8601 / DayOfWeek)
        LocalTime horaInicio,
        LocalTime horaFin,
        int duracionCitaMinutos,
        boolean activo,
        OffsetDateTime fechaCreacion
) {
    public HorarioAtencion {
        Objects.requireNonNull(medicoId, "El médico es obligatorio");
        Objects.requireNonNull(horaInicio, "La hora de inicio es obligatoria");
        Objects.requireNonNull(horaFin, "La hora de fin es obligatoria");
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("El día de la semana debe estar entre 1 (Lunes) y 7 (Domingo)");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (duracionCitaMinutos <= 0) {
            throw new IllegalArgumentException("La duración de la cita debe ser mayor a 0 minutos");
        }
    }

    public static HorarioAtencion nuevo(Long medicoId, int diaSemana, LocalTime horaInicio,
                                       LocalTime horaFin, int duracionCitaMinutos) {
        return new HorarioAtencion(null, medicoId, diaSemana, horaInicio, horaFin,
                duracionCitaMinutos, true, OffsetDateTime.now());
    }
}
