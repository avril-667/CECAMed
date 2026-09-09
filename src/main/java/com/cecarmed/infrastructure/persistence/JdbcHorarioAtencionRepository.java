package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.HorarioAtencion;
import com.cecarmed.domain.repository.HorarioAtencionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcHorarioAtencionRepository implements HorarioAtencionRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcHorarioAtencionRepository.class);
    private final DataSource dataSource;

    public JdbcHorarioAtencionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<HorarioAtencion> findByMedicoId(Long medicoId) {
        String sql = """
                SELECT id, medico_id, dia_semana, hora_inicio, hora_fin,
                       duracion_cita_minutos, activo, fecha_creacion
                FROM horario_atencion
                WHERE medico_id = ? AND activo = TRUE
                ORDER BY dia_semana ASC, hora_inicio ASC
                """;

        List<HorarioAtencion> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHorario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar horarios para médico ID: {}", medicoId, e);
            throw new RuntimeException("Error en base de datos al buscar horarios", e);
        }
        return list;
    }

    @Override
    public Optional<HorarioAtencion> findByMedicoIdAndDiaSemana(Long medicoId, int diaSemana) {
        String sql = """
                SELECT id, medico_id, dia_semana, hora_inicio, hora_fin,
                       duracion_cita_minutos, activo, fecha_creacion
                FROM horario_atencion
                WHERE medico_id = ? AND dia_semana = ? AND activo = TRUE
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicoId);
            ps.setInt(2, diaSemana);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHorario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar horario para médico {} en día {}", medicoId, diaSemana, e);
            throw new RuntimeException("Error en base de datos al buscar horario", e);
        }
        return Optional.empty();
    }

    @Override
    public HorarioAtencion save(HorarioAtencion h) {
        String sql = """
                INSERT INTO horario_atencion (medico_id, dia_semana, hora_inicio, hora_fin,
                                              duracion_cita_minutos, activo, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id, fecha_creacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, h.medicoId());
            ps.setInt(2, h.diaSemana());
            ps.setTime(3, Time.valueOf(h.horaInicio()));
            ps.setTime(4, Time.valueOf(h.horaFin()));
            ps.setInt(5, h.duracionCitaMinutos());
            ps.setBoolean(6, h.activo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime created = rs.getObject("fecha_creacion", OffsetDateTime.class);
                    return new HorarioAtencion(
                            generatedId,
                            h.medicoId(),
                            h.diaSemana(),
                            h.horaInicio(),
                            h.horaFin(),
                            h.duracionCitaMinutos(),
                            h.activo(),
                            created
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID del horario guardado.");
        } catch (SQLException e) {
            log.error("Error al guardar horario de atención", e);
            throw new RuntimeException("Error en base de datos al guardar horario", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM horario_atencion WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al eliminar horario ID: {}", id, e);
            throw new RuntimeException("Error al eliminar horario", e);
        }
    }

    private HorarioAtencion mapResultSetToHorario(ResultSet rs) throws SQLException {
        Time inicio = rs.getTime("hora_inicio");
        Time fin = rs.getTime("hora_fin");
        return new HorarioAtencion(
                rs.getLong("id"),
                rs.getLong("medico_id"),
                rs.getInt("dia_semana"),
                inicio != null ? inicio.toLocalTime() : LocalTime.of(8, 0),
                fin != null ? fin.toLocalTime() : LocalTime.of(17, 0),
                rs.getInt("duracion_cita_minutos"),
                rs.getBoolean("activo"),
                rs.getObject("fecha_creacion", OffsetDateTime.class)
        );
    }
}
