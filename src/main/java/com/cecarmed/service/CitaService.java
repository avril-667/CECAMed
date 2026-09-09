package com.cecarmed.service;

import com.cecarmed.domain.model.*;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.BloqueoAgendaRepository;
import com.cecarmed.domain.repository.CitaRepository;
import com.cecarmed.domain.repository.HorarioAtencionRepository;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.infrastructure.calendar.GoogleCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    private final CitaRepository citaRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final BloqueoAgendaRepository bloqueoRepository;
    private final GoogleCalendarService googleCalendarService;
    private final AuditoriaRepository auditoriaRepository;

    public CitaService(CitaRepository citaRepository,
                       HorarioAtencionRepository horarioRepository,
                       BloqueoAgendaRepository bloqueoRepository,
                       GoogleCalendarService googleCalendarService,
                       AuditoriaRepository auditoriaRepository) {
        this.citaRepository = Objects.requireNonNull(citaRepository);
        this.horarioRepository = Objects.requireNonNull(horarioRepository);
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
        this.googleCalendarService = Objects.requireNonNull(googleCalendarService);
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository);
    }

    /**
     * Agenda una nueva cita médica aplicando control estricto de traslapes y sincronización asíncrona.
     */
    public Cita agendarCita(Cita cita, String pacienteNombre, String pacienteEmail) {
        validarReglasCita(cita);

        // 1. Control de Bloqueos de Agenda
        if (bloqueoRepository.existsBloqueo(cita.medicoId(), cita.fechaHoraInicio(), cita.fechaHoraFin())) {
            throw new IllegalStateException("El horario solicitado coincide con un bloqueo de agenda del médico.");
        }

        // 2. Control Estricto de Traslapes de Citas
        if (citaRepository.existsTraslape(cita.medicoId(), cita.fechaHoraInicio(), cita.fechaHoraFin(), null)) {
            throw new IllegalStateException("Conflicto de horario: El médico ya tiene una cita programada o activa en ese horario.");
        }

        // 3. Persistir la Cita
        Cita guardada = citaRepository.save(cita);
        log.info("Cita agendada exitosamente ID {} para médico ID {} en {}",
                guardada.id(), guardada.medicoId(), guardada.fechaHoraInicio());

        // 4. Sincronización asíncrona con Google Calendar mediante Virtual Threads
        Thread.ofVirtual().start(() -> {
            try {
                String summary = "Consulta CECAMed - " + (pacienteNombre != null ? pacienteNombre : "Paciente");
                String gcalId = googleCalendarService.createEvent(
                        summary,
                        cita.motivoConsulta(),
                        cita.fechaHoraInicio(),
                        cita.fechaHoraFin(),
                        pacienteEmail
                );
                if (gcalId != null) {
                    citaRepository.updateGoogleCalendarEventId(guardada.id(), gcalId);
                    log.info("Cita ID {} vinculada a Google Calendar con ID: {}", guardada.id(), gcalId);
                }
            } catch (Exception e) {
                log.warn("No se pudo sincronizar la cita ID {} con Google Calendar: {}", guardada.id(), e.getMessage());
            }
        });

        // 5. Auditoría
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "cita",
                guardada.id(),
                TipoAccionAuditoria.INSERT,
                null,
                "{\"medicoId\": " + guardada.medicoId() + ", \"inicio\": \"" + guardada.fechaHoraInicio() + "\"}",
                "localhost"
        ));

        return guardada;
    }

    /**
     * Cancela una cita y remueve el evento de Google Calendar de forma asíncrona.
     */
    public void cancelarCita(Long citaId, String motivoCancelacion) {
        Optional<Cita> optCita = citaRepository.findById(citaId);
        if (optCita.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la cita con ID: " + citaId);
        }

        Cita cita = optCita.get();
        if (cita.estado() == EstadoCita.CANCELADA) {
            throw new IllegalStateException("La cita ya se encuentra cancelada.");
        }

        citaRepository.updateEstado(citaId, EstadoCita.CANCELADA);
        log.info("Cita ID {} cancelada.", citaId);

        // Cancelar en Google Calendar si existe
        if (cita.googleCalendarEventId() != null) {
            Thread.ofVirtual().start(() -> googleCalendarService.deleteEvent(cita.googleCalendarEventId()));
        }

        // Auditoría
        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "cita",
                citaId,
                TipoAccionAuditoria.UPDATE,
                "{\"estado\": \"" + cita.estado() + "\"}",
                "{\"estado\": \"CANCELADA\", \"motivo\": \"" + motivoCancelacion + "\"}",
                "localhost"
        ));
    }

    public void cambiarEstado(Long citaId, EstadoCita nuevoEstado) {
        citaRepository.updateEstado(citaId, nuevoEstado);
        log.info("Cita ID {} cambió su estado a: {}", citaId, nuevoEstado);

        auditoriaRepository.registrar(RegistroAuditoria.nuevo(
                UserSession.getCurrentUserId(),
                "cita",
                citaId,
                TipoAccionAuditoria.UPDATE,
                null,
                "{\"nuevoEstado\": \"" + nuevoEstado + "\"}",
                "localhost"
        ));
    }

    public List<Cita> obtenerCitasPorFecha(LocalDate fecha) {
        return citaRepository.findByFecha(fecha);
    }

    public List<Cita> obtenerCitasPorFechaYMedico(LocalDate fecha, Long medicoId) {
        return citaRepository.findByFechaAndMedico(fecha, medicoId);
    }

    public BloqueoAgenda registrarBloqueo(BloqueoAgenda bloqueo) {
        if (!bloqueo.fechaHoraInicio().isBefore(bloqueo.fechaHoraFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la de fin");
        }
        BloqueoAgenda guardado = bloqueoRepository.save(bloqueo);
        log.info("Bloqueo de agenda registrado para médico ID {}: {}", bloqueo.medicoId(), bloqueo.motivo());
        return guardado;
    }

    public List<BloqueoAgenda> obtenerBloqueos(Long medicoId, OffsetDateTime inicio, OffsetDateTime fin) {
        return bloqueoRepository.findByMedicoIdAndRango(medicoId, inicio, fin);
    }

    public HorarioAtencion guardarHorario(HorarioAtencion horario) {
        return horarioRepository.save(horario);
    }

    public List<HorarioAtencion> obtenerHorariosPorMedico(Long medicoId) {
        return horarioRepository.findByMedicoId(medicoId);
    }

    public long contarCitasHoy() {
        return citaRepository.countCitasHoy();
    }

    private void validarReglasCita(Cita c) {
        if (c.pacienteId() == null) {
            throw new IllegalArgumentException("El paciente es obligatorio.");
        }
        if (c.medicoId() == null) {
            throw new IllegalArgumentException("El médico es obligatorio.");
        }
        if (c.fechaHoraInicio() == null || c.fechaHoraFin() == null) {
            throw new IllegalArgumentException("El horario de inicio y fin son obligatorios.");
        }
        if (!c.fechaHoraInicio().isBefore(c.fechaHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        if (c.motivoConsulta() == null || c.motivoConsulta().isBlank()) {
            throw new IllegalArgumentException("El motivo de consulta es obligatorio.");
        }
    }
}
