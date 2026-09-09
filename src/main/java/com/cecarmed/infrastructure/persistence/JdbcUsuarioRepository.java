package com.cecarmed.infrastructure.persistence;

import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia JDBC para la entidad Usuario con consultas preparadas (Prepared Statements).
 */
public class JdbcUsuarioRepository implements UsuarioRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUsuarioRepository.class);
    private final DataSource dataSource;

    public JdbcUsuarioRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar usuario por ID: {}", id, e);
            throw new RuntimeException("Error en base de datos al buscar usuario por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                WHERE LOWER(username) = LOWER(?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar usuario por username: {}", username, e);
            throw new RuntimeException("Error en base de datos al buscar usuario por username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> findAll() {
        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                ORDER BY nombre_completo ASC
                """;

        List<Usuario> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar usuarios", e);
            throw new RuntimeException("Error en base de datos al listar usuarios", e);
        }
        return list;
    }

    @Override
    public List<Usuario> findByRol(Rol rol) {
        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                WHERE rol = ? AND activo = TRUE
                ORDER BY nombre_completo ASC
                """;

        List<Usuario> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rol.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar usuarios por rol: {}", rol, e);
            throw new RuntimeException("Error en base de datos al buscar usuarios por rol", e);
        }
        return list;
    }

    @Override
    public List<Usuario> findAllMedicos() {
        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                WHERE rol = 'MEDICO'
                ORDER BY activo DESC, nombre_completo ASC
                """;

        List<Usuario> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar todos los médicos", e);
            throw new RuntimeException("Error en base de datos al listar médicos", e);
        }
        return list;
    }

    @Override
    public List<Usuario> searchMedicos(String query) {
        if (query == null || query.isBlank()) {
            return findAllMedicos();
        }

        String sql = """
                SELECT id, nombre_completo, username, password_hash, rol, cedula_profesional,
                       especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion
                FROM usuario
                WHERE rol = 'MEDICO'
                  AND (
                      LOWER(nombre_completo) LIKE ?
                      OR LOWER(COALESCE(cedula_profesional, '')) LIKE ?
                      OR LOWER(COALESCE(especialidad, '')) LIKE ?
                      OR LOWER(username) LIKE ?
                      OR LOWER(COALESCE(email, '')) LIKE ?
                      OR COALESCE(telefono, '') LIKE ?
                  )
                ORDER BY activo DESC, nombre_completo ASC
                """;

        String pattern = "%" + query.trim().toLowerCase() + "%";
        List<Usuario> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            ps.setString(6, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar médicos con filtro: {}", query, e);
            throw new RuntimeException("Error en base de datos al buscar médicos", e);
        }
        return list;
    }

    @Override
    public Usuario save(Usuario usuario) {
        String sql = """
                INSERT INTO usuario (nombre_completo, username, password_hash, rol, cedula_profesional,
                                     especialidad, email, telefono, activo, fecha_creacion, fecha_actualizacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id, fecha_creacion, fecha_actualizacion
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.nombreCompleto());
            ps.setString(2, usuario.username());
            ps.setString(3, usuario.passwordHash());
            ps.setString(4, usuario.rol().name());
            ps.setString(5, usuario.cedulaProfesional());
            ps.setString(6, usuario.especialidad());
            ps.setString(7, usuario.email());
            ps.setString(8, usuario.telefono());
            ps.setBoolean(9, usuario.activo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    OffsetDateTime created = rs.getObject("fecha_creacion", OffsetDateTime.class);
                    OffsetDateTime updated = rs.getObject("fecha_actualizacion", OffsetDateTime.class);
                    return new Usuario(
                            generatedId,
                            usuario.nombreCompleto(),
                            usuario.username(),
                            usuario.passwordHash(),
                            usuario.rol(),
                            usuario.cedulaProfesional(),
                            usuario.especialidad(),
                            usuario.email(),
                            usuario.telefono(),
                            usuario.activo(),
                            created,
                            updated
                    );
                }
            }
            throw new SQLException("No se pudo obtener el ID generado para el usuario.");
        } catch (SQLException e) {
            log.error("Error al guardar usuario: {}", usuario.username(), e);
            throw new RuntimeException("Error en base de datos al guardar usuario", e);
        }
    }

    @Override
    public void update(Usuario usuario) {
        String sql = """
                UPDATE usuario
                SET nombre_completo = ?, rol = ?, cedula_profesional = ?,
                    especialidad = ?, email = ?, telefono = ?, activo = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.nombreCompleto());
            ps.setString(2, usuario.rol().name());
            ps.setString(3, usuario.cedulaProfesional());
            ps.setString(4, usuario.especialidad());
            ps.setString(5, usuario.email());
            ps.setString(6, usuario.telefono());
            ps.setBoolean(7, usuario.activo());
            ps.setLong(8, usuario.id());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("No se encontró el usuario con ID " + usuario.id() + " para actualizar.");
            }
        } catch (SQLException e) {
            log.error("Error al actualizar usuario: {}", usuario.id(), e);
            throw new RuntimeException("Error en base de datos al actualizar usuario", e);
        }
    }

    @Override
    public void updatePassword(Long usuarioId, String newPasswordHash) {
        String sql = """
                UPDATE usuario
                SET password_hash = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar contraseña del usuario ID: {}", usuarioId, e);
            throw new RuntimeException("Error en base de datos al actualizar contraseña", e);
        }
    }

    @Override
    public void setActivo(Long usuarioId, boolean activo) {
        String sql = """
                UPDATE usuario
                SET activo = ?, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al cambiar estado activo del usuario ID: {}", usuarioId, e);
            throw new RuntimeException("Error en base de datos al modificar estado del usuario", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM usuario WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error al verificar existencia de username: {}", username, e);
            throw new RuntimeException("Error en base de datos al verificar username", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM usuario";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            log.error("Error al contar usuarios", e);
            throw new RuntimeException("Error en base de datos al contar usuarios", e);
        }
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                rs.getString("nombre_completo"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Rol.fromString(rs.getString("rol")),
                rs.getString("cedula_profesional"),
                rs.getString("especialidad"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getBoolean("activo"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
