package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.RegistroAuditoria;

import java.util.List;

public interface AuditoriaRepository {
    void registrar(RegistroAuditoria auditoria);
    List<RegistroAuditoria> findRecientes(int limite);
}
