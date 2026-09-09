package com.cecarmed.service;

import com.cecarmed.domain.model.AtencionSala;
import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.model.EstadoSala;
import com.cecarmed.domain.repository.AtencionSalaRepository;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaEsperaServiceTest {

    @Mock private AtencionSalaRepository atencionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private AuditoriaRepository auditoriaRepository;

    private SalaEsperaService salaEsperaService;

    @BeforeEach
    void setUp() {
        salaEsperaService = new SalaEsperaService(atencionRepository, citaRepository, auditoriaRepository);
    }

    @Test
    @DisplayName("Debe registrar check-in exitoso asignando turno y actualizando estado de la cita")
    void shouldRegisterCheckInSuccessfully() {
        Long citaId = 1L;
        Long pacienteId = 100L;
        OffsetDateTime ahora = OffsetDateTime.now();

        Cita cita = new Cita(citaId, pacienteId, 2L, ahora, ahora.plusMinutes(30),
                "Consulta de control", EstadoCita.PROGRAMADA, null, null, ahora, ahora);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(atencionRepository.findByCitaId(citaId)).thenReturn(Optional.empty());
        when(atencionRepository.generateNextTurnoCodigo()).thenReturn("T-01");
        when(atencionRepository.save(any())).thenAnswer(inv -> {
            AtencionSala a = inv.getArgument(0);
            return new AtencionSala(10L, a.citaId(), a.pacienteId(), a.turnoCodigo(),
                    a.estadoSala(), a.horaLlegada(), null, null, null, a.observaciones());
        });

        AtencionSala atencion = salaEsperaService.registrarLlegada(citaId, "Llegó puntual");

        assertThat(atencion).isNotNull();
        assertThat(atencion.turnoCodigo()).isEqualTo("T-01");
        assertThat(atencion.estadoSala()).isEqualTo(EstadoSala.ESPERANDO);

        verify(citaRepository, times(1)).updateEstado(citaId, EstadoCita.EN_SALA);
        verify(auditoriaRepository, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Debe rechazar check-in si la cita ya fue registrada en sala previamente")
    void shouldRejectDuplicateCheckIn() {
        Long citaId = 2L;
        OffsetDateTime ahora = OffsetDateTime.now();
        Cita cita = new Cita(citaId, 100L, 2L, ahora, ahora.plusMinutes(30),
                "Consulta", EstadoCita.EN_SALA, null, null, ahora, ahora);

        AtencionSala previa = new AtencionSala(5L, citaId, 100L, "T-02", EstadoSala.ESPERANDO, ahora, null, null, null, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(atencionRepository.findByCitaId(citaId)).thenReturn(Optional.of(previa));

        assertThatThrownBy(() -> salaEsperaService.registrarLlegada(citaId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya cuenta con un turno registrado");

        verify(atencionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar check-in si la cita se encuentra cancelada")
    void shouldRejectCheckInForCancelledAppointment() {
        Long citaId = 3L;
        OffsetDateTime ahora = OffsetDateTime.now();
        Cita cita = new Cita(citaId, 100L, 2L, ahora, ahora.plusMinutes(30),
                "Consulta", EstadoCita.CANCELADA, null, null, ahora, ahora);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> salaEsperaService.registrarLlegada(citaId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelada");

        verify(atencionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe gestionar el ciclo de atención: llamar paciente, iniciar consulta y finalizar")
    void shouldHandleFullWaitingRoomCycle() {
        Long atencionId = 5L;
        Long citaId = 1L;
        OffsetDateTime ahora = OffsetDateTime.now();
        AtencionSala atencion = new AtencionSala(atencionId, citaId, 100L, "T-03", EstadoSala.ESPERANDO, ahora, null, null, null, null);

        when(atencionRepository.findById(atencionId)).thenReturn(Optional.of(atencion));

        // 1. Llamar
        salaEsperaService.llamarPaciente(atencionId);
        verify(atencionRepository, times(1)).registrarLlamado(eq(atencionId), any());

        // 2. Iniciar consulta
        salaEsperaService.iniciarConsulta(atencionId);
        verify(atencionRepository, times(1)).registrarInicioConsulta(eq(atencionId), any());
        verify(citaRepository, times(1)).updateEstado(citaId, EstadoCita.EN_CONSULTA);

        // 3. Finalizar
        salaEsperaService.finalizarAtencion(atencionId);
        verify(atencionRepository, times(1)).registrarFinConsulta(eq(atencionId), any());
        verify(citaRepository, times(1)).updateEstado(citaId, EstadoCita.ATENDIDA);
    }
}
