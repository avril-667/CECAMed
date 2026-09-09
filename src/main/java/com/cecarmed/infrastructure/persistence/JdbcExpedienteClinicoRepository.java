package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.ExpedienteClinico;
import com.cecarmed.domain.repository.ExpedienteClinicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcExpedienteClinicoRepository implements ExpedienteClinicoRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcExpedienteClinicoRepository.class);
    private final DataSource dataSource;

    public JdbcExpedienteClinicoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ExpedienteClinico> findById(Long id) {
        String sql = """
                SELECT id, paciente_id, medico_id, cita_id, fecha_consulta, motivo_consulta,
                       subjetivo, objetivo, peso_kg, talla_cm, imc, presion_arterial,
                       frecuencia_cardiaca, frecuencia_respiratoria, temperatura_c,
                       saturacion_oxigeno, glucosa_mg_dl, diagnostico, plan_tratamiento,
                       receta_medica, notas_adicionales, fecha_creacion
                FROM expediente_clinico
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExpediente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar expediente por ID: {}", id, e);
            throw new RuntimeException("Error al buscar expediente clínico", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ExpedienteClinico> findByPacienteId(Long pacienteId) {
        String sql = """
                SELECT id, paciente_id, medico_id, cita_id, fecha_consulta, motivo_consulta,
                       subjetivo, objetivo, peso_kg, talla_cm, imc, presion_arterial,
                       frecuencia_cardiaca, frecuencia_respiratoria, temperatura_c,
                       saturacion_oxigeno, glucosa_mg_dl, diagnostico, plan_tratamiento,
                       receta_medica, notas_adicionales, fecha_creacion
                FROM expediente_clinico
                WHERE paciente_id = ?
                ORDER BY fecha_consulta DESC
                """;

        List<ExpedienteClinico> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExpediente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar expedientes por paciente ID: {}", pacienteId, e);
            throw new RuntimeException("Error al consultar expedientes del paciente", e);
        }
        return list;
    }

    @Override
    public Optional<ExpedienteClinico> findByCitaId(Long citaId) {
        String sql = """
                SELECT id, paciente_id, medico_id, cita_id, fecha_consulta, motivo_consulta,
                       subjetivo, objetivo, peso_kg, talla_cm, imc, presion_arterial,
                       frecuencia_cardiaca, frecuencia_respiratoria, temperatura_c,
                       saturacion_oxigeno, glucosa_mg_dl, diagnostico, plan_tratamiento,
                       receta_medica, notas_adicionales, fecha_creacion
                FROM expediente_clinico
                WHERE cita_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExpediente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar expediente por cita ID: {}", citaId, e);
            throw new RuntimeException("Error al consultar expediente por cita", e);
        }
        return Optional.empty();
    }

    @Override
    public ExpedienteClinico save(ExpedienteClinico exp) {
        String sql = """
                INSERT INTO expediente_clinico (
                    paciente_id, medico_id, cita_id, fecha_consulta, motivo_consulta,
                    subjetivo, objetivo, peso_kg, talla_cm, presion_arterial,
                    frecuencia_cardiaca, frecuencia_respiratoria, temperatura_c,
                    saturacion_oxigeno, glucosa_mg_dl, diagnostico, plan_tratamiento,
                    receta_medica, notas_adicionales, fecha_creacion
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id, fecha_consulta, imc, fecha_creacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, exp.pacienteId());
            ps.setLong(2, exp.medicoId());

            if (exp.citaId() != null) {
                ps.setLong(3, exp.citaId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setString(4, exp.motivoConsulta());
            ps.setString(5, exp.subjetivo());
            ps.setString(6, exp.objetivo());

            setNullableDouble(ps, 7, exp.pesoKg());
            setNullableDouble(ps, 8, exp.tallaCm());
            ps.setString(9, exp.presionArterial());
            setNullableInteger(ps, 10, exp.frecuenciaCardiaca());
            setNullableInteger(ps, 11, exp.frecuenciaRespiratoria());
            setNullableDouble(ps, 12, exp.temperaturaC());
            setNullableDouble(ps, 13, exp.saturacionOxigeno());
            setNullableDouble(ps, 14, exp.glucosaMgDl());

            ps.setString(15, exp.diagnostico());
            ps.setString(16, exp.planTratamiento());
            ps.setString(17, exp.recetaMedica());
            ps.setString(18, exp.notasAdicionales());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime fechaConsulta = rs.getObject("fecha_consulta", OffsetDateTime.class);
                    Double imcCalculado = rs.getObject("imc", Double.class);
                    OffsetDateTime fechaCreacion = rs.getObject("fecha_creacion", OffsetDateTime.class);

                    return new ExpedienteClinico(
                            generatedId,
                            exp.pacienteId(),
                            exp.medicoId(),
                            exp.citaId(),
                            fechaConsulta,
                            exp.motivoConsulta(),
                            exp.subjetivo(),
                            exp.objetivo(),
                            exp.pesoKg(),
                            exp.tallaCm(),
                            imcCalculado,
                            exp.presionArterial(),
                            exp.frecuenciaCardiaca(),
                            exp.frecuenciaRespiratoria(),
                            exp.temperaturaC(),
                            exp.saturacionOxigeno(),
                            exp.glucosaMgDl(),
                            exp.diagnostico(),
                            exp.planTratamiento(),
                            exp.recetaMedica(),
                            exp.notasAdicionales(),
                            fechaCreacion
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID del expediente clínico guardado.");
        } catch (SQLException e) {
            log.error("Error al guardar expediente clínico: {}", e.getMessage(), e);
            throw new RuntimeException("Error en base de datos al guardar expediente", e);
        }
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM expediente_clinico";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar total de expedientes clínicos", e);
            throw new RuntimeException("Error al contar expedientes", e);
        }
        return 0;
    }

    private void setNullableDouble(PreparedStatement ps, int paramIndex, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(paramIndex, value);
        } else {
            ps.setNull(paramIndex, Types.NUMERIC);
        }
    }

    private void setNullableInteger(PreparedStatement ps, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(paramIndex, value);
        } else {
            ps.setNull(paramIndex, Types.INTEGER);
        }
    }

    private ExpedienteClinico mapResultSetToExpediente(ResultSet rs) throws SQLException {
        return new ExpedienteClinico(
                rs.getLong("id"),
                rs.getLong("paciente_id"),
                rs.getLong("medico_id"),
                rs.getObject("cita_id", Long.class),
                rs.getObject("fecha_consulta", OffsetDateTime.class),
                rs.getString("motivo_consulta"),
                rs.getString("subjetivo"),
                rs.getString("objetivo"),
                rs.getObject("peso_kg", Double.class),
                rs.getObject("talla_cm", Double.class),
                rs.getObject("imc", Double.class),
                rs.getString("presion_arterial"),
                rs.getObject("frecuencia_cardiaca", Integer.class),
                rs.getObject("frecuencia_respiratoria", Integer.class),
                rs.getObject("temperatura_c", Double.class),
                rs.getObject("saturacion_oxigeno", Double.class),
                rs.getObject("glucosa_mg_dl", Double.class),
                rs.getString("diagnostico"),
                rs.getString("plan_tratamiento"),
                rs.getString("receta_medica"),
                rs.getString("notas_adicionales"),
                rs.getObject("fecha_creacion", OffsetDateTime.class)
        );
    }
}
