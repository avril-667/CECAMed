package com.cecarmed.service;

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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Servicio de negocio para la gestión integral de médicos y especialistas en CECAMed.
 */
public class MedicoService {

    private static final Logger log = LoggerFactory.getLogger(MedicoService.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public MedicoService(UsuarioRepository usuarioRepository, AuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository no puede ser nulo");
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository, "auditoriaRepository no puede ser nulo");
    }

    /**
     * Retorna todos los médicos registrados.
     */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllMedicos();
    }

    /**
     * Busca médicos por término de búsqueda (nombre, especialidad, cédula, usuario o teléfono).
     */
    public List<Usuario> buscarMedicos(String query) {
        return usuarioRepository.searchMedicos(query);
    }

    /**
     * Retorna un médico por su ID.
     */
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id).filter(u -> u.rol() == Rol.MEDICO);
    }

    /**
     * Registra un nuevo médico en el sistema.
     */
    public Usuario registrarMedico(String nombreCompleto,
                                   String username,
                                   String password,
                                   String cedulaProfesional,
                                   String especialidad,
                                   String email,
                                   String telefono) {
        validarDatosRegistro(nombreCompleto, username, password, cedulaProfesional, especialidad, email, telefono);

        String usernameLimpio = username.trim().toLowerCase();
        if (usuarioRepository.existsByUsername(usernameLimpio)) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el nombre de usuario: " + usernameLimpio);
        }

        String passwordHash = PasswordService.hashPassword(password.trim());
        String emailLimpio = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;
        String telLimpio = (telefono != null && !telefono.isBlank()) ? telefono.trim() : null;
        String cedulaLimpia = (cedulaProfesional != null && !cedulaProfesional.isBlank()) ? cedulaProfesional.trim().toUpperCase() : null;
        String espLimpia = especialidad.trim();

        Usuario nuevoMedico = Usuario.nuevo(
                nombreCompleto.trim(),
                usernameLimpio,
                passwordHash,
                Rol.MEDICO,
                cedulaLimpia,
                espLimpia,
                emailLimpio,
                telLimpio
        );

        Usuario guardado = usuarioRepository.save(nuevoMedico);
        log.info("Médico registrado exitosamente: Dr(a). {} (ID: {}, Especialidad: {})",
                guardado.nombreCompleto(), guardado.id(), guardado.especialidad());

        // Auditoría
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "usuario",
                guardado.id(),
                TipoAccionAuditoria.INSERT,
                null,
                "{\"accion\": \"REGISTRO_MEDICO\", \"medico\": \"" + guardado.nombreCompleto() + "\", \"especialidad\": \"" + guardado.especialidad() + "\"}",
                "localhost"
        ));

        return guardado;
    }

    /**
     * Actualiza la información de un médico existente.
     */
    public void actualizarMedico(Long id,
                                 String nombreCompleto,
                                 String nuevaPasswordOpcional,
                                 String cedulaProfesional,
                                 String especialidad,
                                 String email,
                                 String telefono,
                                 boolean activo) {
        Objects.requireNonNull(id, "El ID del médico es obligatorio");

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el médico con ID: " + id));

        if (existente.rol() != Rol.MEDICO) {
            throw new IllegalArgumentException("El usuario con ID " + id + " no es un médico.");
        }

        validarDatosActualizacion(nombreCompleto, cedulaProfesional, especialidad, email, telefono);

        // Si se especificó una nueva contraseña, actualizarla
        if (nuevaPasswordOpcional != null && !nuevaPasswordOpcional.isBlank()) {
            if (nuevaPasswordOpcional.trim().length() < 6) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            String nuevaPasswordHash = PasswordService.hashPassword(nuevaPasswordOpcional.trim());
            usuarioRepository.updatePassword(id, nuevaPasswordHash);
            log.info("Contraseña actualizada para médico ID: {}", id);
        }

        String emailLimpio = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;
        String telLimpio = (telefono != null && !telefono.isBlank()) ? telefono.trim() : null;
        String cedulaLimpia = (cedulaProfesional != null && !cedulaProfesional.isBlank()) ? cedulaProfesional.trim().toUpperCase() : null;

        Usuario actualizado = new Usuario(
                existente.id(),
                nombreCompleto.trim(),
                existente.username(),
                existente.passwordHash(),
                Rol.MEDICO,
                cedulaLimpia,
                especialidad.trim(),
                emailLimpio,
                telLimpio,
                activo,
                existente.fechaCreacion(),
                existente.fechaActualizacion()
        );

        usuarioRepository.update(actualizado);
        log.info("Médico actualizado: {} (ID: {})", actualizado.nombreCompleto(), actualizado.id());

        // Auditoría
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "usuario",
                id,
                TipoAccionAuditoria.UPDATE,
                "{\"nombre\": \"" + existente.nombreCompleto() + "\", \"activo\": " + existente.activo() + "}",
                "{\"nombre\": \"" + actualizado.nombreCompleto() + "\", \"activo\": " + actualizado.activo() + "}",
                "localhost"
        ));
    }

    /**
     * Activa o desactiva el acceso de un médico al sistema.
     */
    public void toggleActivoMedico(Long id, boolean activo) {
        Objects.requireNonNull(id, "El ID del médico es obligatorio");
        Usuario medico = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado con ID: " + id));

        usuarioRepository.setActivo(id, activo);
        log.info("Estado del médico {} cambiado a activo={}", medico.nombreCompleto(), activo);

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "usuario",
                id,
                TipoAccionAuditoria.UPDATE,
                "{\"activo\": " + medico.activo() + "}",
                "{\"activo\": " + activo + "}",
                "localhost"
        ));
    }

    private void validarDatosRegistro(String nombreCompleto, String username, String password,
                                      String cedulaProfesional, String especialidad, String email, String telefono) {
        if (nombreCompleto == null || nombreCompleto.trim().length() < 3) {
            throw new IllegalArgumentException("El nombre completo del médico es obligatorio (mínimo 3 caracteres).");
        }
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 3 y 30 caracteres alfanuméricos (sin espacios).");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (cedulaProfesional == null || cedulaProfesional.trim().isBlank()) {
            throw new IllegalArgumentException("La cédula profesional es obligatoria para el registro médico.");
        }
        if (especialidad == null || especialidad.trim().isBlank()) {
            throw new IllegalArgumentException("La especialidad médica es obligatoria.");
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("El formato del correo electrónico es inválido.");
        }
        if (telefono != null && !telefono.isBlank() && telefono.trim().length() < 7) {
            throw new IllegalArgumentException("El número de teléfono debe tener al menos 7 dígitos.");
        }
    }

    private void validarDatosActualizacion(String nombreCompleto, String cedulaProfesional,
                                           String especialidad, String email, String telefono) {
        if (nombreCompleto == null || nombreCompleto.trim().length() < 3) {
            throw new IllegalArgumentException("El nombre completo del médico es obligatorio (mínimo 3 caracteres).");
        }
        if (cedulaProfesional == null || cedulaProfesional.trim().isBlank()) {
            throw new IllegalArgumentException("La cédula profesional es obligatoria para el personal médico.");
        }
        if (especialidad == null || especialidad.trim().isBlank()) {
            throw new IllegalArgumentException("La especialidad médica es obligatoria.");
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("El formato del correo electrónico es inválido.");
        }
        if (telefono != null && !telefono.isBlank() && telefono.trim().length() < 7) {
            throw new IllegalArgumentException("El número de teléfono debe tener al menos 7 dígitos.");
        }
    }
}