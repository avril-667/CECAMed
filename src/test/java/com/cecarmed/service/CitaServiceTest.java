package com.cecarmed.service;

import com.cecarmed.domain.model.BloqueoAgenda;
import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.BloqueoAgendaRepository;
import com.cecarmed.domain.repository.CitaRepository;
import com.cecarmed.domain.repository.HorarioAtencionRepository;
import com.cecarmed.infrastructure.calendar.GoogleCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock private CitaRepository citaRepository;
    @Mock private HorarioAtencionRepository horarioRepository;
    @Mock private BloqueoAgendaRepository bloqueoRepository;
    @Mock private GoogleCalendarService googleCalendarService;
    @Mock private AuditoriaRepository auditoriaRepository;

    private CitaService citaService;

    @BeforeEach
    void setUp() {
        citaService = new CitaService(citaRepository, horarioRepository, bloqueoRepository,
                googleCalendarService, auditoriaRepository);
    }

    @Test
    @DisplayName("Debe agendar exitosamente una cita sin traslapes")
    void shouldScheduleAppointmentWhenNoOverlap() {
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(10).withMinute(0);
        OffsetDateTime fin = inicio.plusMinutes(30);

        Cita nueva = Cita.nueva(1L, 2L, inicio, fin, "Consulta de rutina", "Primera vez");

        when(bloqueoRepository.existsBloqueo(2L, inicio, fin)).thenReturn(false);
        when(citaRepository.existsTraslape(2L, inicio, fin, null)).thenReturn(false);
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            return new Cita(10L, c.pacienteId(), c.medicoId(), c.fechaHoraInicio(), c.fechaHoraFin(),
                    c.motivoConsulta(), c.estado(), null, c.notas(), c.fechaCreacion(), c.fechaActualizacion());
        });

        Cita agendada = citaService.agendarCita(nueva, "Juan Pérez", "juan@correo.com");

        assertThat(agendada).isNotNull();
        assertThat(agendada.id()).isEqualTo(10L);
        assertThat(agendada.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        verify(citaRepository, times(1)).save(any());
        verify(auditoriaRepository, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Debe impedir agendar cita si existe traslape de horario con otra cita del médico")
    void shouldRejectAppointmentWhenOverlapExists() {
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(11).withMinute(0);
        OffsetDateTime fin = inicio.plusMinutes(30);

        Cita nueva = Cita.nueva(1L, 2L, inicio, fin, "Revisión", null);

        when(bloqueoRepository.existsBloqueo(2L, inicio, fin)).thenReturn(false);
        when(citaRepository.existsTraslape(2L, inicio, fin, null)).thenReturn(true); // Hay traslape!

        assertThatThrownBy(() -> citaService.agendarCita(nueva, "Paciente X", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicto de horario");

        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe impedir agendar cita si el horario coincide con un bloqueo de agenda")
    void shouldRejectAppointmentWhenBloqueoExists() {
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(9).withMinute(0);
        OffsetDateTime fin = inicio.plusMinutes(30);

        Cita nueva = Cita.nueva(1L, 2L, inicio, fin, "Chequeo", null);

        when(bloqueoRepository.existsBloqueo(2L, inicio, fin)).thenReturn(true); // Médico tiene bloqueo

        assertThatThrownBy(() -> citaService.agendarCita(nueva, "Paciente Y", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueo de agenda");

        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe cancelar una cita existente y actualizar su estado")
    void shouldCancelAppointmentSuccessfully() {
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        Cita existente = new Cita(5L, 1L, 2L, inicio, inicio.plusMinutes(30),
                "Consulta", EstadoCita.PROGRAMADA, "gcal_123", "Notas", inicio, inicio);

        when(citaRepository.findById(5L)).thenReturn(Optional.of(existente));

        citaService.cancelarCita(5L, "Paciente solicitó cancelación");

        verify(citaRepository, times(1)).updateEstado(5L, EstadoCita.CANCELADA);
        verify(auditoriaRepository, times(1)).registrar(any());
    }
}
