package com.cecarmed.service;

import com.cecarmed.domain.model.ExpedienteClinico;
import com.cecarmed.domain.model.Genero;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.ExpedienteClinicoRepository;
import com.cecarmed.domain.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private ExpedienteClinicoRepository expedienteClinicoRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    private PacienteService pacienteService;

    @BeforeEach
    void setUp() {
        pacienteService = new PacienteService(pacienteRepository, expedienteClinicoRepository, auditoriaRepository);
    }

    @Test
    @DisplayName("Debe calcular el IMC correctamente y clasificarlo según la OMS")
    void shouldCalculateImcCorrectly() {
        // Peso: 70 kg, Talla: 175 cm -> IMC = 70 / (1.75 * 1.75) = 22.86
        Double imc = ExpedienteClinico.calcularImc(70.0, 175.0);
        assertThat(imc).isEqualTo(22.86);

        ExpedienteClinico ec = new ExpedienteClinico(
                1L, 1L, 1L, null, null,
                "Consulta general", null, null,
                70.0, 175.0, imc, "120/80", 72, 16, 36.5, 98.0, 90.0,
                "Paciente sano", "Continuar dieta balanceada", null, null, null
        );

        assertThat(ec.getClasificacionImc()).isEqualTo("Peso normal");
    }

    @Test
    @DisplayName("Debe clasificar sobrepeso y obesidad correctamente")
    void shouldClassifyOverweightAndObesity() {
        Double imcSobrepeso = ExpedienteClinico.calcularImc(85.0, 170.0); // ~29.41
        Double imcObesidad = ExpedienteClinico.calcularImc(100.0, 170.0); // ~34.60

        assertThat(imcSobrepeso).isNotNull();
        assertThat(imcSobrepeso).isBetween(25.0, 29.99);

        ExpedienteClinico ecSobrepeso = new ExpedienteClinico(
                1L, 1L, 1L, null, null,
                "Control", null, null,
                85.0, 170.0, imcSobrepeso, null, null, null, null, null, null,
                "Sobrepeso", "Plan nutricional", null, null, null
        );
        assertThat(ecSobrepeso.getClasificacionImc()).isEqualTo("Sobrepeso");

        ExpedienteClinico ecObesidad = new ExpedienteClinico(
                2L, 1L, 1L, null, null,
                "Control", null, null,
                100.0, 170.0, imcObesidad, null, null, null, null, null, null,
                "Obesidad", "Plan integral", null, null, null
        );
        assertThat(ecObesidad.getClasificacionImc()).isEqualTo("Obesidad Grado I");
    }

    @Test
    @DisplayName("Debe rechazar el registro de paciente con fecha de nacimiento futura")
    void shouldRejectFutureBirthDate() {
        Paciente invalid = Paciente.nuevo(
                null,
                "Carlos",
                "Mendoza",
                null,
                LocalDate.now().plusDays(1), // Fecha futura
                Genero.MASCULINO,
                null,
                "555-1111",
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> pacienteService.registrarPaciente(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("Debe generar número de expediente si viene nulo o vacío")
    void shouldAutoGenerateExpedienteNumberWhenMissing() {
        when(pacienteRepository.generateNextExpedienteNumero()).thenReturn("EXP-2026-00001");
        when(pacienteRepository.save(any())).thenAnswer(invocation -> {
            Paciente p = invocation.getArgument(0);
            return new Paciente(1L, p.expedienteNumero(), p.nombre(), p.primerApellido(), p.segundoApellido(),
                    p.fechaNacimiento(), p.genero(), p.curp(), p.telefono(), p.email(), p.direccion(),
                    p.alergias(), p.antecedentesPatologicos(), p.antecedentesNoPatologicos(),
                    p.antecedentesHeredofamiliares(), true, null, null);
        });

        Paciente p = Paciente.nuevo(
                null, // expediente nulo
                "Ana",
                "López",
                "García",
                LocalDate.of(1995, 5, 20),
                Genero.FEMENINO,
                null,
                "555-9988",
                "ana@email.com",
                "Calle 123",
                "Penicilina",
                null, null, null
        );

        Paciente saved = pacienteService.registrarPaciente(p);

        assertThat(saved.expedienteNumero()).isEqualTo("EXP-2026-00001");
        verify(pacienteRepository, times(1)).generateNextExpedienteNumero();
        verify(auditoriaRepository, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Debe rechazar CURP duplicada")
    void shouldRejectDuplicateCurp() {
        String curp = "LOPA950520MDFRNC01";
        when(pacienteRepository.findByCurp(curp)).thenReturn(Optional.of(mock(Paciente.class)));

        Paciente p = Paciente.nuevo(
                null,
                "Ana",
                "López",
                null,
                LocalDate.of(1995, 5, 20),
                Genero.FEMENINO,
                curp,
                "555-9988",
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> pacienteService.registrarPaciente(p))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un paciente");
    }
}
