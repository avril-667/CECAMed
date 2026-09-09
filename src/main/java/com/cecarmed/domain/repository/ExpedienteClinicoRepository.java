package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.ExpedienteClinico;

import java.util.List;
import java.util.Optional;

public interface ExpedienteClinicoRepository {

    Optional<ExpedienteClinico> findById(Long id);

    List<ExpedienteClinico> findByPacienteId(Long pacienteId);

    Optional<ExpedienteClinico> findByCitaId(Long citaId);

    ExpedienteClinico save(ExpedienteClinico expediente);

    long countTotal();
}
