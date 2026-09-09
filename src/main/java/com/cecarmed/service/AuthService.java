package com.cecarmed.service;

import com.cecarmed.domain.exception.AuthenticationException;
import com.cecarmed.domain.model.RegistroAuditoria;
import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.TipoAccionAuditoria;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.infrastructure.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de autenticación y seguridad para CECAMed.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public AuthService(UsuarioRepository usuarioRepository, AuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository);
    }

    /**
     * Autentica un usuario verificando credenciales con BCrypt y asegurando que esté activo.
     *
     * @param username       Nombre de usuario
     * @param plainPassword  Contraseña en texto claro
     * @param ipOrigen       Dirección IP o identificador del equipo cliente
     * @return El usuario autenticado
     * @throws AuthenticationException si las credenciales son inválidas o el usuario está inactivo
     */
    public Usuario login(String username, String plainPassword, String ipOrigen) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            throw new AuthenticationException("El usuario y la contraseña son obligatorios.");
        }

        Optional<Usuario> optUsuario = usuarioRepository.findByUsername(username.trim());
        if (optUsuario.isEmpty()) {
            registrarFalloAuditoria(null, username, "Usuario no existe", ipOrigen);
            throw new AuthenticationException("Credenciales inválidas.");
        }

        Usuario usuario = optUsuario.get();

        if (!usuario.activo()) {
            registrarFalloAuditoria(usuario.id(), username, "Usuario inactivo", ipOrigen);
            throw new AuthenticationException("La cuenta de usuario está desactivada. Contacte al administrador.");
        }

        boolean passwordValida = PasswordService.verifyPassword(plainPassword, usuario.passwordHash());
        if (!passwordValida) {
            registrarFalloAuditoria(usuario.id(), username, "Contraseña incorrecta", ipOrigen);
            throw new AuthenticationException("Credenciales inválidas.");
        }

        // Login exitoso: Registrar en auditoría e iniciar sesión
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                usuario.id(),
                "usuario",
                usuario.id(),
                TipoAccionAuditoria.LOGIN,
                null,
                "{\"username\": \"" + usuario.username() + "\", \"rol\": \"" + usuario.rol() + "\"}",
                ipOrigen
        ));

        UserSession.login(usuario);
        log.info("Inicio de sesión exitoso: {} (Rol: {})", usuario.username(), usuario.rol());
        return usuario;
    }

    /**
     * Cierra la sesión activa en el sistema.
     */
    public void logout(String ipOrigen) {
        Usuario user = UserSession.getCurrentUser();
        if (user != null) {
            auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                    user.id(),
                    "usuario",
                    user.id(),
                    TipoAccionAuditoria.LOGOUT,
                    null,
                    null,
                    ipOrigen
            ));
            log.info("Cierre de sesión para: {}", user.username());
            UserSession.logout();
        }
    }

    /**
     * Inicializa un usuario Administrador por defecto si el sistema no tiene usuarios registrados.
     */
    public void seedDefaultAdminIfEmpty() {
        if (usuarioRepository.count() == 0) {
            log.info("Base de datos sin usuarios detectada. Sembrando administrador por defecto...");
            String passHash = PasswordService.hashPassword("Admin123*");
            Usuario admin = Usuario.nuevo(
                    "Administrador CECAMed",
                    "admin",
                    passHash,
                    Rol.ADMINISTRADOR,
                    null,
                    "Dirección Médica",
                    "admin@cecarmed.com",
                    "000-000-0000"
            );
            usuarioRepository.save(admin);
            log.info("Administrador por defecto creado con éxito (Usuario: admin)");
        }
    }

    private void registrarFalloAuditoria(Long usuarioId, String username, String motivo, String ipOrigen) {
        log.warn("Fallo de autenticación para '{}': {}", username, motivo);
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                usuarioId,
                "usuario",
                usuarioId,
                TipoAccionAuditoria.ACCESO_DENEGADO,
                null,
                "{\"intento_usuario\": \"" + username + "\", \"motivo\": \"" + motivo + "\"}",
                ipOrigen
        ));
    }
}
