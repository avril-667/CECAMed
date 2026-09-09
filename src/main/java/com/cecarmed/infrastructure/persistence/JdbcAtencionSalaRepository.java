package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.AtencionSala;
import com.cecarmed.domain.model.EstadoSala;
import com.cecarmed.domain.repository.AtencionSalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAtencionSalaRepository implements AtencionSalaRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAtencionSalaRepository.class);
    private final DataSource dataSource;

    public JdbcAtencionSalaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<AtencionSala> findById(Long id) {
        String sql = """
                SELECT id, cita_id, paciente_id, turno_codigo, estado_sala,
                       hora_llegada, hora_llamado, hora_inicio_atencion,
                       hora_fin_atencion, observaciones
                FROM atencion_sala
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAtencion(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar atención en sala por ID: {}", id, e);
            throw new RuntimeException("Error en base de datos al buscar atención en sala", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AtencionSala> findByCitaId(Long citaId) {
        String sql = """
                SELECT id, cita_id, paciente_id, turno_codigo, estado_sala,
                       hora_llegada, hora_llamado, hora_inicio_atencion,
                       hora_fin_atencion, observaciones
                FROM atencion_sala
                WHERE cita_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAtencion(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar atención en sala por cita ID: {}", citaId, e);
            throw new RuntimeException("Error en base de datos al buscar atención por cita", e);
        }
        return Optional.empty();
    }

    @Override
    public List<AtencionSala> findActivosHoy() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = """
                SELECT id, cita_id, paciente_id, turno_codigo, estado_sala,
                       hora_llegada, hora_llamado, hora_inicio_atencion,
                       hora_fin_atencion, observaciones
                FROM atencion_sala
                WHERE estado_sala IN ('ESPERANDO', 'LLAMADO', 'EN_CONSULTA')
                  AND hora_llegada >= ? AND hora_llegada < ?
                ORDER BY hora_llegada ASC
                """;

        List<AtencionSala> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAtencion(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al consultar pacientes activos en sala de espera", e);
            throw new RuntimeException("Error al consultar pacientes en sala", e);
        }
        return list;
    }

    @Override
    public List<AtencionSala> findHistorialHoy() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = """
                SELECT id, cita_id, paciente_id, turno_codigo, estado_sala,
                       hora_llegada, hora_llamado, hora_inicio_atencion,
                       hora_fin_atencion, observaciones
                FROM atencion_sala
                WHERE hora_llegada >= ? AND hora_llegada < ?
                ORDER BY hora_llegada DESC
                """;

        List<AtencionSala> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAtencion(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al consultar historial de atención en sala de hoy", e);
            throw new RuntimeException("Error al consultar historial de atención", e);
        }
        return list;
    }

    @Override
    public Optional<AtencionSala> findUltimoLlamado() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = """
                SELECT id, cita_id, paciente_id, turno_codigo, estado_sala,
                       hora_llegada, hora_llamado, hora_inicio_atencion,
                       hora_fin_atencion, observaciones
                FROM atencion_sala
                WHERE estado_sala IN ('LLAMADO', 'EN_CONSULTA')
                  AND hora_llamado IS NOT NULL
                  AND hora_llegada >= ?
                ORDER BY hora_llamado DESC
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAtencion(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al consultar último llamado", e);
        }
        return Optional.empty();
    }

    @Override
    public String generateNextTurnoCodigo() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = "SELECT COUNT(*) FROM atencion_sala WHERE hora_llegada >= ? AND hora_llegada < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            try (ResultSet rs = ps.executeQuery()) {
                long next = 1;
                if (rs.next()) {
                    next = rs.getLong(1) + 1;
                }
                return String.format("T-%02d", next);
            }
        } catch (SQLException e) {
            log.error("Error al generar correlativo de turno", e);
            return "T-" + (System.currentTimeMillis() % 100);
        }
    }

    @Override
    public AtencionSala save(AtencionSala a) {
        String sql = """
                INSERT INTO atencion_sala (cita_id, paciente_id, turno_codigo, estado_sala,
                                           hora_llegada, hora_llamado, hora_inicio_atencion,
                                           hora_fin_atencion, observaciones)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)
                RETURNING id, hora_llegada
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, a.citaId());
            ps.setLong(2, a.pacienteId());
            ps.setString(3, a.turnoCodigo());
            ps.setString(4, a.estadoSala().name());
            ps.setObject(5, a.horaLlamado());
            ps.setObject(6, a.horaInicioAtencion());
            ps.setObject(7, a.horaFinAtencion());
            ps.setString(8, a.observaciones());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime llegada = rs.getObject("hora_llegada", OffsetDateTime.class);
                    return new AtencionSala(
                            generatedId,
                            a.citaId(),
                            a.pacienteId(),
                            a.turnoCodigo(),
                            a.estadoSala(),
                            llegada,
                            a.horaLlamado(),
                            a.horaInicioAtencion(),
                            a.horaFinAtencion(),
                            a.observaciones()
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID generado para la atención en sala.");
        } catch (SQLException e) {
            log.error("Error al registrar atención en sala", e);
            throw new RuntimeException("Error en base de datos al guardar atención en sala", e);
        }
    }

    @Override
    public void updateEstado(Long id, EstadoSala nuevoEstado) {
        String sql = "UPDATE atencion_sala SET estado_sala = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar estado en sala ID {}: {}", id, nuevoEstado, e);
            throw new RuntimeException("Error al actualizar estado en sala", e);
        }
    }

    @Override
    public void registrarLlamado(Long id, OffsetDateTime horaLlamado) {
        String sql = "UPDATE atencion_sala SET estado_sala = 'LLAMADO', hora_llamado = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, horaLlamado);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al registrar llamado en sala ID: {}", id, e);
            throw new RuntimeException("Error al registrar llamado en sala", e);
        }
    }

    @Override
    public void registrarInicioConsulta(Long id, OffsetDateTime horaInicio) {
        String sql = "UPDATE atencion_sala SET estado_sala = 'EN_CONSULTA', hora_inicio_atencion = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, horaInicio);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al registrar inicio de consulta en sala ID: {}", id, e);
            throw new RuntimeException("Error al registrar inicio de consulta", e);
        }
    }

    @Override
    public void registrarFinConsulta(Long id, OffsetDateTime horaFin) {
        String sql = "UPDATE atencion_sala SET estado_sala = 'FINALIZADO', hora_fin_atencion = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, horaFin);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al registrar fin de consulta en sala ID: {}", id, e);
            throw new RuntimeException("Error al finalizar atención en sala", e);
        }
    }

    @Override
    public long countEnEsperaHoy() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = "SELECT COUNT(*) FROM atencion_sala WHERE estado_sala = 'ESPERANDO' AND hora_llegada >= ? AND hora_llegada < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error al contar en espera hoy", e);
        }
        return 0;
    }

    private AtencionSala mapResultSetToAtencion(ResultSet rs) throws SQLException {
        return new AtencionSala(
                rs.getLong("id"),
                rs.getLong("cita_id"),
                rs.getLong("paciente_id"),
                rs.getString("turno_codigo"),
                EstadoSala.fromString(rs.getString("estado_sala")),
                rs.getObject("hora_llegada", OffsetDateTime.class),
                rs.getObject("hora_llamado", OffsetDateTime.class),
                rs.getObject("hora_inicio_atencion", OffsetDateTime.class),
                rs.getObject("hora_fin_atencion", OffsetDateTime.class),
                rs.getString("observaciones")
        );
    }
}
