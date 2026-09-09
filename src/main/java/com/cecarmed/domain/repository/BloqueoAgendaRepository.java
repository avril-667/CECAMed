package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.BloqueoAgenda;

import java.time.OffsetDateTime;
import java.util.List;

public interface BloqueoAgendaRepository {
    List<BloqueoAgenda> findByMedicoIdAndRango(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin);
    boolean existsBloqueo(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin);
    BloqueoAgenda save(BloqueoAgenda bloqueo);
    void delete(Long id);
}
