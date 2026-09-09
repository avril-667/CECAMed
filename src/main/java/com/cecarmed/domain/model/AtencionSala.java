package com.cecarmed.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Representación inmutable del turno y atención en sala de espera (Java 21 Record).
 */
public record AtencionSala(
        Long id,
        Long citaId,
        Long pacienteId,
        String turnoCodigo,
        EstadoSala estadoSala,
        OffsetDateTime horaLlegada,
        OffsetDateTime horaLlamado,
        OffsetDateTime horaInicioAtencion,
        OffsetDateTime horaFinAtencion,
        String observaciones
) {
    public AtencionSala {
        Objects.requireNonNull(citaId, "El ID de cita es obligatorio");
        Objects.requireNonNull(pacienteId, "El ID de paciente es obligatorio");
        Objects.requireNonNull(turnoCodigo, "El código de turno es obligatorio");
        Objects.requireNonNull(estadoSala, "El estado de sala es obligatorio");
        Objects.requireNonNull(horaLlegada, "La hora de llegada es obligatoria");
    }

    public long getMinutosEspera() {
        OffsetDateTime finEspera = horaLlamado != null ? horaLlamado :
                (horaInicioAtencion != null ? horaInicioAtencion : OffsetDateTime.now());
        return Math.max(0, Duration.between(horaLlegada, finEspera).toMinutes());
    }

    public long getMinutosConsulta() {
        if (horaInicioAtencion == null) {
            return 0;
        }
        OffsetDateTime fin = horaFinAtencion != null ? horaFinAtencion : OffsetDateTime.now();
        return Math.max(0, Duration.between(horaInicioAtencion, fin).toMinutes());
    }

    public static AtencionSala nuevo(Long citaId, Long pacienteId, String turnoCodigo, String observaciones) {
        return new AtencionSala(
                null,
                citaId,
                pacienteId,
                turnoCodigo,
                EstadoSala.ESPERANDO,
                OffsetDateTime.now(),
                null,
                null,
                null,
                observaciones
        );
    }
}
