package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.BloqueoAgenda;
import com.cecarmed.domain.repository.BloqueoAgendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcBloqueoAgendaRepository implements BloqueoAgendaRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBloqueoAgendaRepository.class);
    private final DataSource dataSource;

    public JdbcBloqueoAgendaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<BloqueoAgenda> findByMedicoIdAndRango(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin) {
        String sql = """
                SELECT id, medico_id, fecha_hora_inicio, fecha_hora_fin, motivo, fecha_creacion
                FROM bloqueo_agenda
                WHERE medico_id = ?
                  AND fecha_hora_inicio < ?
                  AND fecha_hora_fin > ?
                ORDER BY fecha_hora_inicio ASC
                """;

        List<BloqueoAgenda> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setObject(2, fin);
            ps.setObject(3, inicio);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBloqueo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar bloqueos de agenda para médico ID: {}", medicoId, e);
            throw new RuntimeException("Error en base de datos al buscar bloqueos", e);
        }
        return list;
    }

    @Override
    public boolean existsBloqueo(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin) {
        String sql = """
                SELECT 1
                FROM bloqueo_agenda
                WHERE medico_id = ?
                  AND fecha_hora_inicio < ?
                  AND fecha_hora_fin > ?
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setObject(2, fin);
            ps.setObject(3, inicio);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar bloqueo de agenda", e);
            throw new RuntimeException("Error en base de datos al verificar bloqueo", e);
        }
    }

    @Override
    public BloqueoAgenda save(BloqueoAgenda b) {
        String sql = """
                INSERT INTO bloqueo_agenda (medico_id, fecha_hora_inicio, fecha_hora_fin, motivo, fecha_creacion)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id, fecha_creacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, b.medicoId());
            ps.setObject(2, b.fechaHoraInicio());
            ps.setObject(3, b.fechaHoraFin());
            ps.setString(4, b.motivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime created = rs.getObject("fecha_creacion", OffsetDateTime.class);
                    return new BloqueoAgenda(
                            generatedId,
                            b.medicoId(),
                            b.fechaHoraInicio(),
                            b.fechaHoraFin(),
                            b.motivo(),
                            created
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID del bloqueo guardado.");
        } catch (SQLException e) {
            log.error("Error al guardar bloqueo de agenda", e);
            throw new RuntimeException("Error en base de datos al guardar bloqueo", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM bloqueo_agenda WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al eliminar bloqueo ID: {}", id, e);
            throw new RuntimeException("Error al eliminar bloqueo", e);
        }
    }

    private BloqueoAgenda mapResultSetToBloqueo(ResultSet rs) throws SQLException {
        return new BloqueoAgenda(
                rs.getLong("id"),
                rs.getLong("medico_id"),
                rs.getObject("fecha_hora_inicio", OffsetDateTime.class),
                rs.getObject("fecha_hora_fin", OffsetDateTime.class),
                rs.getString("motivo"),
                rs.getObject("fecha_creacion", OffsetDateTime.class)
        );
    }
}
