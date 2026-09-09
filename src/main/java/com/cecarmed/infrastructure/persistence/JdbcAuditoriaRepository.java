package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.RegistroAuditoria;
import com.cecarmed.domain.model.TipoAccionAuditoria;
import com.cecarmed.domain.repository.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuditoriaRepository implements AuditoriaRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAuditoriaRepository.class);
    private final DataSource dataSource;

    public JdbcAuditoriaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void registrar(RegistroAuditoria auditoria) {
        String sql = """
                INSERT INTO registro_auditoria (usuario_id, tabla_afectada, registro_id, accion,
                                                datos_anteriores, datos_nuevos, direccion_ip, fecha_registro)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, CURRENT_TIMESTAMP)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (auditoria.usuarioId() != null) {
                ps.setLong(1, auditoria.usuarioId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            ps.setString(2, auditoria.tablaAfectada());

            if (auditoria.registroId() != null) {
                ps.setLong(3, auditoria.registroId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setString(4, auditoria.accion().name());
            ps.setString(5, auditoria.datosAnteriores());
            ps.setString(6, auditoria.datosNuevos());
            ps.setString(7, auditoria.direccionIp());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al registrar auditoría: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<RegistroAuditoria> findRecientes(int limite) {
        String sql = """
                SELECT id, usuario_id, tabla_afectada, registro_id, accion,
                       datos_anteriores, datos_nuevos, direccion_ip, fecha_registro
                FROM registro_auditoria
                ORDER BY fecha_registro DESC
                LIMIT ?
                """;

        List<RegistroAuditoria> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long usuarioId = rs.getObject("usuario_id", Long.class);
                    Long registroId = rs.getObject("registro_id", Long.class);
                    list.add(new RegistroAuditoria(
                            rs.getLong("id"),
                            usuarioId,
                            rs.getString("tabla_afectada"),
                            registroId,
                            TipoAccionAuditoria.valueOf(rs.getString("accion")),
                            rs.getString("datos_anteriores"),
                            rs.getString("datos_nuevos"),
                            rs.getString("direccion_ip"),
                            rs.getObject("fecha_registro", OffsetDateTime.class)
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Error al consultar auditoría reciente", e);
        }
        return list;
    }
}
