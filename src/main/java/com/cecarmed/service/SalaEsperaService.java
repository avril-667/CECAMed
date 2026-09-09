package com.cecarmed.service;

import com.cecarmed.domain.model.AtencionSala;
import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.model.EstadoSala;
import com.cecarmed.domain.model.RegistroAuditoria;
import com.cecarmed.domain.model.TipoAccionAuditoria;
import com.cecarmed.domain.repository.AtencionSalaRepository;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.CitaRepository;
import com.cecarmed.domain.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SalaEsperaService {

    private static final Logger log = LoggerFactory.getLogger(SalaEsperaService.class);

    private final AtencionSalaRepository atencionRepository;
    private final CitaRepository citaRepository;
    private final AuditoriaRepository auditoriaRepository;

    public SalaEsperaService(AtencionSalaRepository atencionRepository,
                             CitaRepository citaRepository,
                             AuditoriaRepository auditoriaRepository) {
        this.atencionRepository = Objects.requireNonNull(atencionRepository);
        this.citaRepository = Objects.requireNonNull(citaRepository);
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository);
    }

    /**
     * Realiza el Check-In de un paciente con cita previa en la sala de espera.
     */
    public AtencionSala registrarLlegada(Long citaId, String observaciones) {
        Optional<Cita> optCita = citaRepository.findById(citaId);
        if (optCita.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + citaId);
        }

        Cita cita = optCita.get();
        if (cita.estado() == EstadoCita.CANCELADA) {
            throw new IllegalStateException("No se puede registrar en sala una cita cancelada.");
        }
        if (cita.estado() == EstadoCita.ATENDIDA) {
            throw new IllegalStateException("La cita ya fue atendida previamente.");
        }

        // Validar que no se haya realizado check-in previamente
        Optional<AtencionSala> previa = atencionRepository.findByCitaId(citaId);
        if (previa.isPresent()) {
            throw new IllegalStateException("El paciente ya cuenta con un turno registrado para esta cita: " + previa.get().turnoCodigo());
        }

        String turnoCodigo = atencionRepository.generateNextTurnoCodigo();
        AtencionSala nuevaAtencion = AtencionSala.nuevo(citaId, cita.pacienteId(), turnoCodigo, observaciones);
        AtencionSala guardada = atencionRepository.save(nuevaAtencion);

        // Actualizar estado de la cita
        citaRepository.updateEstado(citaId, EstadoCita.EN_SALA);
        log.info("Check-in completado: Turno {} para paciente ID {} en cita ID {}",
                turnoCodigo, cita.pacienteId(), citaId);

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "atencion_sala",
                guardada.id(),
                TipoAccionAuditoria.INSERT,
                null,
                "{\"turno\": \"" + turnoCodigo + "\", \"citaId\": " + citaId + "}",
                "localhost"
        ));

        return guardada;
    }

    /**
     * Realiza el llamado sonoro/visual del paciente para pasar al consultorio.
     */
    public void llamarPaciente(Long atencionId) {
        AtencionSala atencion = obtenerPorIdOError(atencionId);
        OffsetDateTime ahora = OffsetDateTime.now();
        atencionRepository.registrarLlamado(atencionId, ahora);
        log.info("Llamando a paciente con Turno: {}", atencion.turnoCodigo());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "atencion_sala",
                atencionId,
                TipoAccionAuditoria.UPDATE,
                "{\"estado\": \"" + atencion.estadoSala() + "\"}",
                "{\"estado\": \"LLAMADO\"}",
                "localhost"
        ));
    }

    /**
     * Inicia formalmente la consulta médica.
     */
    public void iniciarConsulta(Long atencionId) {
        AtencionSala atencion = obtenerPorIdOError(atencionId);
        OffsetDateTime ahora = OffsetDateTime.now();
        atencionRepository.registrarInicioConsulta(atencionId, ahora);
        citaRepository.updateEstado(atencion.citaId(), EstadoCita.EN_CONSULTA);
        log.info("Consulta iniciada para Turno: {}", atencion.turnoCodigo());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "atencion_sala",
                atencionId,
                TipoAccionAuditoria.UPDATE,
                "{\"estado\": \"" + atencion.estadoSala() + "\"}",
                "{\"estado\": \"EN_CONSULTA\"}",
                "localhost"
        ));
    }

    /**
     * Finaliza la atención médica del paciente.
     */
    public void finalizarAtencion(Long atencionId) {
        AtencionSala atencion = obtenerPorIdOError(atencionId);
        OffsetDateTime ahora = OffsetDateTime.now();
        atencionRepository.registrarFinConsulta(atencionId, ahora);
        citaRepository.updateEstado(atencion.citaId(), EstadoCita.ATENDIDA);
        log.info("Atención finalizada para Turno: {}", atencion.turnoCodigo());

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "atencion_sala",
                atencionId,
                TipoAccionAuditoria.UPDATE,
                "{\"estado\": \"" + atencion.estadoSala() + "\"}",
                "{\"estado\": \"FINALIZADO\"}",
                "localhost"
        ));
    }

    /**
     * Cancela o abandona el turno de sala de espera.
     */
    public void cancelarTurno(Long atencionId, String motivo) {
        AtencionSala atencion = obtenerPorIdOError(atencionId);
        atencionRepository.updateEstado(atencionId, EstadoSala.CANCELADO);
        log.info("Turno {} cancelado por: {}", atencion.turnoCodigo(), motivo);

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "atencion_sala",
                atencionId,
                TipoAccionAuditoria.UPDATE,
                null,
                "{\"estado\": \"CANCELADO\", \"motivo\": \"" + motivo + "\"}",
                "localhost"
        ));
    }

    public List<AtencionSala> obtenerActivosHoy() {
        return atencionRepository.findActivosHoy();
    }

    public List<AtencionSala> obtenerHistorialHoy() {
        return atencionRepository.findHistorialHoy();
    }

    public Optional<AtencionSala> obtenerUltimoLlamado() {
        return atencionRepository.findUltimoLlamado();
    }

    public long contarEnEsperaHoy() {
        return atencionRepository.countEnEsperaHoy();
    }

    private AtencionSala obtenerPorIdOError(Long atencionId) {
        return atencionRepository.findById(atencionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el registro de sala con ID: " + atencionId));
    }
}
