package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.AtencionSala;
import com.cecarmed.domain.model.EstadoSala;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AtencionSalaRepository {

    Optional<AtencionSala> findById(Long id);

    Optional<AtencionSala> findByCitaId(Long citaId);

    List<AtencionSala> findActivosHoy();

    List<AtencionSala> findHistorialHoy();

    Optional<AtencionSala> findUltimoLlamado();

    String generateNextTurnoCodigo();

    AtencionSala save(AtencionSala atencion);

    void updateEstado(Long id, EstadoSala nuevoEstado);

    void registrarLlamado(Long id, OffsetDateTime horaLlamado);

    void registrarInicioConsulta(Long id, OffsetDateTime horaInicio);

    void registrarFinConsulta(Long id, OffsetDateTime horaFin);

    long countEnEsperaHoy();
}
