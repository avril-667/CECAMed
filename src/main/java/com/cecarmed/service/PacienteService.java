package com.cecarmed.service;

import com.cecarmed.domain.model.ExpedienteClinico;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.model.RegistroAuditoria;
import com.cecarmed.domain.model.TipoAccionAuditoria;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.ExpedienteClinicoRepository;
import com.cecarmed.domain.repository.PacienteRepository;
import com.cecarmed.domain.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PacienteService {

    private static final Logger log = LoggerFactory.getLogger(PacienteService.class);

    private final PacienteRepository pacienteRepository;
    private final ExpedienteClinicoRepository expedienteClinicoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public PacienteService(PacienteRepository pacienteRepository,
                           ExpedienteClinicoRepository expedienteClinicoRepository,
                           AuditoriaRepository auditoriaRepository) {
        this.pacienteRepository = Objects.requireNonNull(pacienteRepository);
        this.expedienteClinicoRepository = Objects.requireNonNull(expedienteClinicoRepository);
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository);
    }

    public List<Paciente> buscarPacientes(String filtro, int limite, int pagina) {
        int offset = Math.max(0, (pagina - 1) * limite);
        return pacienteRepository.search(filtro, limite, offset);
    }

    public long contarBusqueda(String filtro) {
        return pacienteRepository.countSearch(filtro);
    }

    public Optional<Paciente> obtenerPorId(Long id) {
        return pacienteRepository.findById(id);
    }

    public Optional<Paciente> obtenerPorExpediente(String numeroExpediente) {
        return pacienteRepository.findByExpedienteNumero(numeroExpediente);
    }

    public Paciente registrarPaciente(Paciente paciente) {
        validarDatosPaciente(paciente);

        // Si no trae número de expediente, generar el correlativo anual
        String numeroExp = paciente.expedienteNumero();
        if (numeroExp == null || numeroExp.isBlank()) {
            numeroExp = pacienteRepository.generateNextExpedienteNumero();
        }

        // Validar CURP único si fue provisto
        if (paciente.curp() != null && !paciente.curp().isBlank()) {
            Optional<Paciente> existente = pacienteRepository.findByCurp(paciente.curp().trim().toUpperCase());
            if (existente.isPresent()) {
                throw new IllegalArgumentException("Ya existe un paciente registrado con la CURP: " + paciente.curp());
            }
        }

        Paciente pacienteAGuardar = new Paciente(
                null,
                numeroExp,
                paciente.nombre().trim(),
                paciente.primerApellido().trim(),
                paciente.segundoApellido() != null ? paciente.segundoApellido().trim() : null,
                paciente.fechaNacimiento(),
                paciente.genero(),
                paciente.curp() != null && !paciente.curp().isBlank() ? paciente.curp().trim().toUpperCase() : null,
                paciente.telefono().trim(),
                paciente.email() != null && !paciente.email().isBlank() ? paciente.email().trim().toLowerCase() : null,
                paciente.direccion(),
                paciente.alergias(),
                paciente.antecedentesPatologicos(),
                paciente.antecedentesNoPatologicos(),
                paciente.antecedentesHeredofamiliares(),
                true,
                null,
                null
        );

        Paciente guardado = pacienteRepository.save(pacienteAGuardar);
        log.info("Paciente registrado: {} con expediente {}", guardado.getNombreCompleto(), guardado.expedienteNumero());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "paciente",
                guardado.id(),
                TipoAccionAuditoria.INSERT,
                null,
                "{\"expediente\": \"" + guardado.expedienteNumero() + "\", \"nombre\": \"" + guardado.getNombreCompleto() + "\"}",
                "localhost"
        ));

        return guardado;
    }

    public void actualizarPaciente(Paciente paciente) {
        Objects.requireNonNull(paciente.id(), "El ID del paciente no puede ser nulo para actualizar");
        validarDatosPaciente(paciente);

        // Validar CURP no colisione con otro paciente
        if (paciente.curp() != null && !paciente.curp().isBlank()) {
            Optional<Paciente> existente = pacienteRepository.findByCurp(paciente.curp().trim().toUpperCase());
            if (existente.isPresent() && !existente.get().id().equals(paciente.id())) {
                throw new IllegalArgumentException("La CURP " + paciente.curp() + " pertenece a otro paciente.");
            }
        }

        pacienteRepository.update(paciente);
        log.info("Paciente actualizado ID {}: {}", paciente.id(), paciente.getNombreCompleto());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "paciente",
                paciente.id(),
                TipoAccionAuditoria.UPDATE,
                null,
                "{\"nombre\": \"" + paciente.getNombreCompleto() + "\"}",
                "localhost"
        ));
    }

    public void toggleActivoPaciente(Long id, boolean nuevoEstado) {
        pacienteRepository.setActivo(id, nuevoEstado);
        log.info("Paciente ID {} cambiado a activo={}", id, nuevoEstado);
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "paciente",
                id,
                TipoAccionAuditoria.UPDATE,
                null,
                "{\"activo\": " + nuevoEstado + "}",
                "localhost"
        ));
    }

    public ExpedienteClinico registrarConsulta(ExpedienteClinico consulta) {
        Objects.requireNonNull(consulta.pacienteId(), "El ID del paciente es obligatorio");
        Objects.requireNonNull(consulta.motivoConsulta(), "El motivo de consulta es obligatorio");
        Objects.requireNonNull(consulta.diagnostico(), "El diagnóstico es obligatorio");
        Objects.requireNonNull(consulta.planTratamiento(), "El plan de tratamiento es obligatorio");

        Long medicoId = consulta.medicoId();
        if (medicoId == null) {
            medicoId = UserSession.getCurrentUserId();
            if (medicoId == null) {
                throw new IllegalStateException("No hay un médico autenticado en sesión para registrar la consulta.");
            }
        }

        // Cálculo preventivo de IMC en el dominio
        Double imcCalculado = ExpedienteClinico.calcularImc(consulta.pesoKg(), consulta.tallaCm());

        ExpedienteClinico consultaAGuardar = new ExpedienteClinico(
                null,
                consulta.pacienteId(),
                medicoId,
                consulta.citaId(),
                null,
                consulta.motivoConsulta().trim(),
                consulta.subjetivo(),
                consulta.objetivo(),
                consulta.pesoKg(),
                consulta.tallaCm(),
                imcCalculado,
                consulta.presionArterial(),
                consulta.frecuenciaCardiaca(),
                consulta.frecuenciaRespiratoria(),
                consulta.temperaturaC(),
                consulta.saturacionOxigeno(),
                consulta.glucosaMgDl(),
                consulta.diagnostico().trim(),
                consulta.planTratamiento().trim(),
                consulta.recetaMedica(),
                consulta.notasAdicionales(),
                null
        );

        ExpedienteClinico guardada = expedienteClinicoRepository.save(consultaAGuardar);
        log.info("Consulta clínica registrada con ID {} para paciente ID {}", guardada.id(), guardada.pacienteId());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                medicoId,
                "expediente_clinico",
                guardada.id(),
                TipoAccionAuditoria.INSERT,
                null,
                "{\"pacienteId\": " + guardada.pacienteId() + ", \"diagnostico\": \"" + guardada.diagnostico() + "\"}",
                "localhost"
        ));

        return guardada;
    }

    public List<ExpedienteClinico> obtenerHistorialClinico(Long pacienteId) {
        return expedienteClinicoRepository.findByPacienteId(pacienteId);
    }

    public long contarTotalPacientes() {
        return pacienteRepository.countTotal();
    }

    private void validarDatosPaciente(Paciente p) {
        if (p.nombre() == null || p.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del paciente es obligatorio.");
        }
        if (p.primerApellido() == null || p.primerApellido().isBlank()) {
            throw new IllegalArgumentException("El primer apellido es obligatorio.");
        }
        if (p.fechaNacimiento() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
        }
        if (p.fechaNacimiento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser una fecha futura.");
        }
        if (p.genero() == null) {
            throw new IllegalArgumentException("El género es obligatorio.");
        }
        if (p.telefono() == null || p.telefono().isBlank()) {
            throw new IllegalArgumentException("El número de teléfono es obligatorio.");
        }
    }
}
