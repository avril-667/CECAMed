package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.repository.CitaRepository;
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

public class JdbcCitaRepository implements CitaRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcCitaRepository.class);
    private final DataSource dataSource;

    public JdbcCitaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Cita> findById(Long id) {
        String sql = """
                SELECT id, paciente_id, medico_id, fecha_hora_inicio, fecha_hora_fin,
                       motivo_consulta, estado, google_calendar_event_id, notas,
                       fecha_creacion, fecha_actualizacion
                FROM cita
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCita(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar cita por ID: {}", id, e);
            throw new RuntimeException("Error en base de datos al buscar cita", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cita> findByMedicoAndRango(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin) {
        String sql = """
                SELECT id, paciente_id, medico_id, fecha_hora_inicio, fecha_hora_fin,
                       motivo_consulta, estado, google_calendar_event_id, notas,
                       fecha_creacion, fecha_actualizacion
                FROM cita
                WHERE medico_id = ?
                  AND fecha_hora_inicio >= ?
                  AND fecha_hora_inicio < ?
                ORDER BY fecha_hora_inicio ASC
                """;

        List<Cita> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setObject(2, inicio);
            ps.setObject(3, fin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCita(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas por médico y rango", e);
            throw new RuntimeException("Error al consultar citas", e);
        }
        return list;
    }

    @Override
    public List<Cita> findByFecha(LocalDate fecha) {
        OffsetDateTime inicio = fecha.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = fecha.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = """
                SELECT id, paciente_id, medico_id, fecha_hora_inicio, fecha_hora_fin,
                       motivo_consulta, estado, google_calendar_event_id, notas,
                       fecha_creacion, fecha_actualizacion
                FROM cita
                WHERE fecha_hora_inicio >= ?
                  AND fecha_hora_inicio < ?
                ORDER BY fecha_hora_inicio ASC
                """;

        List<Cita> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCita(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas por fecha: {}", fecha, e);
            throw new RuntimeException("Error al consultar citas por fecha", e);
        }
        return list;
    }

    @Override
    public List<Cita> findByFechaAndMedico(LocalDate fecha, Long medicoId) {
        OffsetDateTime inicio = fecha.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = fecha.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = """
                SELECT id, paciente_id, medico_id, fecha_hora_inicio, fecha_hora_fin,
                       motivo_consulta, estado, google_calendar_event_id, notas,
                       fecha_creacion, fecha_actualizacion
                FROM cita
                WHERE medico_id = ?
                  AND fecha_hora_inicio >= ?
                  AND fecha_hora_inicio < ?
                ORDER BY fecha_hora_inicio ASC
                """;

        List<Cita> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setObject(2, inicio);
            ps.setObject(3, fin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCita(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas por fecha y médico", e);
            throw new RuntimeException("Error al consultar citas por médico", e);
        }
        return list;
    }

    @Override
    public boolean existsTraslape(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin, Long excluirCitaId) {
        String sql = """
                SELECT 1
                FROM cita
                WHERE medico_id = ?
                  AND estado IN ('PROGRAMADA', 'CONFIRMADA', 'EN_SALA', 'EN_CONSULTA')
                  AND fecha_hora_inicio < ?
                  AND fecha_hora_fin > ?
                  AND (? IS NULL OR id != ?)
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setObject(2, fin);
            ps.setObject(3, inicio);

            if (excluirCitaId != null) {
                ps.setLong(4, excluirCitaId);
                ps.setLong(5, excluirCitaId);
            } else {
                ps.setNull(4, Types.BIGINT);
                ps.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al comprobar traslapes de cita", e);
            throw new RuntimeException("Error al verificar traslapes de cita", e);
        }
    }

    @Override
    public Cita save(Cita c) {
        String sql = """
                INSERT INTO cita (paciente_id, medico_id, fecha_hora_inicio, fecha_hora_fin,
                                  motivo_consulta, estado, google_calendar_event_id, notas,
                                  fecha_creacion, fecha_actualizacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id, fecha_creacion, fecha_actualizacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, c.pacienteId());
            ps.setLong(2, c.medicoId());
            ps.setObject(3, c.fechaHoraInicio());
            ps.setObject(4, c.fechaHoraFin());
            ps.setString(5, c.motivoConsulta());
            ps.setString(6, c.estado().name());
            ps.setString(7, c.googleCalendarEventId());
            ps.setString(8, c.notas());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime created = rs.getObject("fecha_creacion", OffsetDateTime.class);
                    OffsetDateTime updated = rs.getObject("fecha_actualizacion", OffsetDateTime.class);
                    return new Cita(
                            generatedId,
                            c.pacienteId(),
                            c.medicoId(),
                            c.fechaHoraInicio(),
                            c.fechaHoraFin(),
                            c.motivoConsulta(),
                            c.estado(),
                            c.googleCalendarEventId(),
                            c.notas(),
                            created,
                            updated
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID de la cita guardada.");
        } catch (SQLException e) {
            log.error("Error al guardar cita", e);
            throw new RuntimeException("Error en base de datos al guardar cita", e);
        }
    }

    @Override
    public void update(Cita c) {
        String sql = """
                UPDATE cita
                SET fecha_hora_inicio = ?, fecha_hora_fin = ?, motivo_consulta = ?,
                    estado = ?, google_calendar_event_id = ?, notas = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, c.fechaHoraInicio());
            ps.setObject(2, c.fechaHoraFin());
            ps.setString(3, c.motivoConsulta());
            ps.setString(4, c.estado().name());
            ps.setString(5, c.googleCalendarEventId());
            ps.setString(6, c.notas());
            ps.setLong(7, c.id());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar cita ID: {}", c.id(), e);
            throw new RuntimeException("Error al actualizar cita", e);
        }
    }

    @Override
    public void updateEstado(Long citaId, EstadoCita nuevoEstado) {
        String sql = "UPDATE cita SET estado = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setLong(2, citaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar estado de cita ID {}: {}", citaId, nuevoEstado, e);
            throw new RuntimeException("Error al cambiar estado de la cita", e);
        }
    }

    @Override
    public void updateGoogleCalendarEventId(Long citaId, String eventId) {
        String sql = "UPDATE cita SET google_calendar_event_id = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setLong(2, citaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar Google Calendar event ID", e);
        }
    }

    @Override
    public long countCitasHoy() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime inicio = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String sql = "SELECT COUNT(*) FROM cita WHERE fecha_hora_inicio >= ? AND fecha_hora_inicio < ?";
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
            log.error("Error al contar citas de hoy", e);
        }
        return 0;
    }

    private Cita mapResultSetToCita(ResultSet rs) throws SQLException {
        return new Cita(
                rs.getLong("id"),
                rs.getLong("paciente_id"),
                rs.getLong("medico_id"),
                rs.getObject("fecha_hora_inicio", OffsetDateTime.class),
                rs.getObject("fecha_hora_fin", OffsetDateTime.class),
                rs.getString("motivo_consulta"),
                EstadoCita.fromString(rs.getString("estado")),
                rs.getString("google_calendar_event_id"),
                rs.getString("notas"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
