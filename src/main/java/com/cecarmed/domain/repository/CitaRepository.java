package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository {

    Optional<Cita> findById(Long id);

    List<Cita> findByMedicoAndRango(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin);

    List<Cita> findByFecha(LocalDate fecha);

    List<Cita> findByFechaAndMedico(LocalDate fecha, Long medicoId);

    /**
     * Verifica si existe alguna cita activa (PROGRAMADA, CONFIRMADA, EN_SALA, EN_CONSULTA)
     * para el médico en el rango [inicio, fin).
     * Excluye la cita con ID excluirCitaId (si no es null, útil al editar).
     */
    boolean existsTraslape(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin, Long excluirCitaId);

    Cita save(Cita cita);

    void update(Cita cita);

    void updateEstado(Long citaId, EstadoCita nuevoEstado);

    void updateGoogleCalendarEventId(Long citaId, String eventId);

    long countCitasHoy();
}
