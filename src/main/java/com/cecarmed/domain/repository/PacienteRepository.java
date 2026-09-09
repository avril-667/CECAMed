package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.Paciente;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository {

    Optional<Paciente> findById(Long id);

    Optional<Paciente> findByExpedienteNumero(String expedienteNumero);

    Optional<Paciente> findByCurp(String curp);

    List<Paciente> search(String query, int limit, int offset);

    long countSearch(String query);

    Paciente save(Paciente paciente);

    void update(Paciente paciente);

    void setActivo(Long id, boolean activo);

    String generateNextExpedienteNumero();

    long countTotal();
}
