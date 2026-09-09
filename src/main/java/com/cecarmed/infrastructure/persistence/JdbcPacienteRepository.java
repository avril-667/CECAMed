package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.Genero;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPacienteRepository implements PacienteRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcPacienteRepository.class);
    private final DataSource dataSource;

    public JdbcPacienteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Paciente> findById(Long id) {
        String sql = """
                SELECT id, expediente_numero, nombre, primer_apellido, segundo_apellido,
                       fecha_nacimiento, genero, curp, telefono, email, direccion,
                       alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                       antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                FROM paciente
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPaciente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar paciente por ID: {}", id, e);
            throw new RuntimeException("Error en base de datos al buscar paciente por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Paciente> findByExpedienteNumero(String expedienteNumero) {
        String sql = """
                SELECT id, expediente_numero, nombre, primer_apellido, segundo_apellido,
                       fecha_nacimiento, genero, curp, telefono, email, direccion,
                       alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                       antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                FROM paciente
                WHERE expediente_numero = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, expedienteNumero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPaciente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar paciente por expediente: {}", expedienteNumero, e);
            throw new RuntimeException("Error en base de datos al buscar paciente por expediente", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Paciente> findByCurp(String curp) {
        String sql = """
                SELECT id, expediente_numero, nombre, primer_apellido, segundo_apellido,
                       fecha_nacimiento, genero, curp, telefono, email, direccion,
                       alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                       antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                FROM paciente
                WHERE curp = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, curp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPaciente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar paciente por CURP: {}", curp, e);
            throw new RuntimeException("Error en base de datos al buscar paciente por CURP", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Paciente> search(String query, int limit, int offset) {
        boolean hasFilter = query != null && !query.isBlank();
        String sql;

        if (hasFilter) {
            sql = """
                    SELECT id, expediente_numero, nombre, primer_apellido, segundo_apellido,
                           fecha_nacimiento, genero, curp, telefono, email, direccion,
                           alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                           antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                    FROM paciente
                    WHERE expediente_numero ILIKE ?
                       OR nombre ILIKE ?
                       OR primer_apellido ILIKE ?
                       OR segundo_apellido ILIKE ?
                       OR curp ILIKE ?
                       OR telefono ILIKE ?
                    ORDER BY primer_apellido ASC, nombre ASC
                    LIMIT ? OFFSET ?
                    """;
        } else {
            sql = """
                    SELECT id, expediente_numero, nombre, primer_apellido, segundo_apellido,
                           fecha_nacimiento, genero, curp, telefono, email, direccion,
                           alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                           antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                    FROM paciente
                    ORDER BY fecha_registro DESC
                    LIMIT ? OFFSET ?
                    """;
        }

        List<Paciente> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                String pattern = "%" + query.trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                ps.setString(4, pattern);
                ps.setString(5, pattern);
                ps.setString(6, pattern);
                ps.setInt(7, limit);
                ps.setInt(8, offset);
            } else {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPaciente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pacientes con filtro '{}'", query, e);
            throw new RuntimeException("Error en base de datos al buscar pacientes", e);
        }
        return list;
    }

    @Override
    public long countSearch(String query) {
        boolean hasFilter = query != null && !query.isBlank();
        String sql;

        if (hasFilter) {
            sql = """
                    SELECT COUNT(*)
                    FROM paciente
                    WHERE expediente_numero ILIKE ?
                       OR nombre ILIKE ?
                       OR primer_apellido ILIKE ?
                       OR segundo_apellido ILIKE ?
                       OR curp ILIKE ?
                       OR telefono ILIKE ?
                    """;
        } else {
            sql = "SELECT COUNT(*) FROM paciente";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                String pattern = "%" + query.trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                ps.setString(4, pattern);
                ps.setString(5, pattern);
                ps.setString(6, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error al contar búsqueda de pacientes", e);
            throw new RuntimeException("Error en base de datos al contar pacientes", e);
        }
        return 0;
    }

    @Override
    public Paciente save(Paciente p) {
        String sql = """
                INSERT INTO paciente (
                    expediente_numero, nombre, primer_apellido, segundo_apellido,
                    fecha_nacimiento, genero, curp, telefono, email, direccion,
                    alergias, antecedentes_patologicos, antecedentes_no_patologicos,
                    antecedentes_heredofamiliares, activo, fecha_registro, fecha_actualizacion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id, fecha_registro, fecha_actualizacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.expedienteNumero());
            ps.setString(2, p.nombre());
            ps.setString(3, p.primerApellido());
            ps.setString(4, p.segundoApellido());
            ps.setDate(5, Date.valueOf(p.fechaNacimiento()));
            ps.setString(6, p.genero().name());
            ps.setString(7, p.curp());
            ps.setString(8, p.telefono());
            ps.setString(9, p.email());
            ps.setString(10, p.direccion());
            ps.setString(11, p.alergias());
            ps.setString(12, p.antecedentesPatologicos());
            ps.setString(13, p.antecedentesNoPatologicos());
            ps.setString(14, p.antecedentesHeredofamiliares());
            ps.setBoolean(15, p.activo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime reg = rs.getObject("fecha_registro", OffsetDateTime.class);
                    OffsetDateTime act = rs.getObject("fecha_actualizacion", OffsetDateTime.class);
                    return new Paciente(
                            generatedId,
                            p.expedienteNumero(),
                            p.nombre(),
                            p.primerApellido(),
                            p.segundoApellido(),
                            p.fechaNacimiento(),
                            p.genero(),
                            p.curp(),
                            p.telefono(),
                            p.email(),
                            p.direccion(),
                            p.alergias(),
                            p.antecedentesPatologicos(),
                            p.antecedentesNoPatologicos(),
                            p.antecedentesHeredofamiliares(),
                            p.activo(),
                            reg,
                            act
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID generado del paciente.");
        } catch (SQLException e) {
            log.error("Error al guardar paciente: {}", p.getNombreCompleto(), e);
            throw new RuntimeException("Error en base de datos al guardar paciente", e);
        }
    }

    @Override
    public void update(Paciente p) {
        String sql = """
                UPDATE paciente
                SET nombre = ?, primer_apellido = ?, segundo_apellido = ?,
                    fecha_nacimiento = ?, genero = ?, curp = ?, telefono = ?,
                    email = ?, direccion = ?, alergias = ?, antecedentes_patologicos = ?,
                    antecedentes_no_patologicos = ?, antecedentes_heredofamiliares = ?,
                    activo = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.nombre());
            ps.setString(2, p.primerApellido());
            ps.setString(3, p.segundoApellido());
            ps.setDate(4, Date.valueOf(p.fechaNacimiento()));
            ps.setString(5, p.genero().name());
            ps.setString(6, p.curp());
            ps.setString(7, p.telefono());
            ps.setString(8, p.email());
            ps.setString(9, p.direccion());
            ps.setString(10, p.alergias());
            ps.setString(11, p.antecedentesPatologicos());
            ps.setString(12, p.antecedentesNoPatologicos());
            ps.setString(13, p.antecedentesHeredofamiliares());
            ps.setBoolean(14, p.activo());
            ps.setLong(15, p.id());

            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar paciente ID {}: {}", p.id(), e.getMessage(), e);
            throw new RuntimeException("Error en base de datos al actualizar paciente", e);
        }
    }

    @Override
    public void setActivo(Long id, boolean activo) {
        String sql = "UPDATE paciente SET activo = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar estado activo de paciente ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error en base de datos al actualizar estado del paciente", e);
        }
    }

    @Override
    public String generateNextExpedienteNumero() {
        int currentYear = Year.now().getValue();
        String prefix = "EXP-" + currentYear + "-";
        String sql = "SELECT COUNT(*) FROM paciente WHERE expediente_numero LIKE ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                long nextSeq = 1;
                if (rs.next()) {
                    nextSeq = rs.getLong(1) + 1;
                }
                return String.format("EXP-%d-%05d", currentYear, nextSeq);
            }
        } catch (SQLException e) {
            log.error("Error al generar número de expediente", e);
            throw new RuntimeException("Error al generar número de expediente", e);
        }
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM paciente";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar total de pacientes", e);
            throw new RuntimeException("Error al contar total de pacientes", e);
        }
        return 0;
    }

    private Paciente mapResultSetToPaciente(ResultSet rs) throws SQLException {
        Date fechaNac = rs.getDate("fecha_nacimiento");
        LocalDate localFechaNac = fechaNac != null ? fechaNac.toLocalDate() : null;

        return new Paciente(
                rs.getLong("id"),
                rs.getString("expediente_numero"),
                rs.getString("nombre"),
                rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"),
                localFechaNac,
                Genero.fromString(rs.getString("genero")),
                rs.getString("curp"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("direccion"),
                rs.getString("alergias"),
                rs.getString("antecedentes_patologicos"),
                rs.getString("antecedentes_no_patologicos"),
                rs.getString("antecedentes_heredofamiliares"),
                rs.getBoolean("activo"),
                rs.getObject("fecha_registro", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
