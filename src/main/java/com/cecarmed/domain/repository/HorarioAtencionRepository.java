package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.HorarioAtencion;

import java.util.List;
import java.util.Optional;

public interface HorarioAtencionRepository {
    List<HorarioAtencion> findByMedicoId(Long medicoId);
    Optional<HorarioAtencion> findByMedicoIdAndDiaSemana(Long medicoId, int diaSemana);
    HorarioAtencion save(HorarioAtencion horario);
    void delete(Long id);
}
