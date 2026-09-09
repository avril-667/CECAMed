package com.cecarmed.domain.model;

import java.time.OffsetDateTime;

/**
 * Representación inmutable de un evento de auditoría en CECAMed (Java 21 Record).
 */
public record RegistroAuditoria(
        Long id,
        Long usuarioId,
        String tablaAfectada,
        Long registroId,
        TipoAccionAuditoria accion,
        String datosAnteriores,
        String datosNuevos,
        String direccionIp,
        OffsetDateTime fechaRegistro
) {
    public static RegistroAuditoria nuevo(Long usuarioId, String tablaAfectada, Long registroId,
                                          TipoAccionAuditoria accion, String datosAnteriores,
                                          String datosNuevos, String direccionIp) {
        return new RegistroAuditoria(null, usuarioId, tablaAfectada, registroId, accion,
                datosAnteriores, datosNuevos, direccionIp, OffsetDateTime.now());
    }
}
