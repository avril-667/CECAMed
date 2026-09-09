package com.cecarmed.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Consulta / Nota médica del expediente clínico con somatometría, IMC y signos vitales (Java 21 Record).
 */
public record ExpedienteClinico(
        Long id,
        Long pacienteId,
        Long medicoId,
        Long citaId,
        OffsetDateTime fechaConsulta,
        String motivoConsulta,
        String subjetivo,
        String objetivo,
        Double pesoKg,
        Double tallaCm,
        Double imc,
        String presionArterial,
        Integer frecuenciaCardiaca,
        Integer frecuenciaRespiratoria,
        Double temperaturaC,
        Double saturacionOxigeno,
        Double glucosaMgDl,
        String diagnostico,
        String planTratamiento,
        String recetaMedica,
        String notasAdicionales,
        OffsetDateTime fechaCreacion
) {
    public ExpedienteClinico {
        Objects.requireNonNull(pacienteId, "El ID de paciente no puede ser nulo");
        Objects.requireNonNull(medicoId, "El ID de médico no puede ser nulo");
        Objects.requireNonNull(motivoConsulta, "El motivo de consulta no puede ser nulo");
        Objects.requireNonNull(diagnostico, "El diagnóstico no puede ser nulo");
        Objects.requireNonNull(planTratamiento, "El plan de tratamiento no puede ser nulo");
    }

    /**
     * Calcula automáticamente el Índice de Masa Corporal (IMC).
     * Fórmula: peso (kg) / [estatura (m)]^2
     */
    public static Double calcularImc(Double pesoKg, Double tallaCm) {
        if (pesoKg == null || tallaCm == null || pesoKg <= 0 || tallaCm <= 0) {
            return null;
        }
        double tallaMetros = tallaCm / 100.0;
        double valorImc = pesoKg / (tallaMetros * tallaMetros);
        return Math.round(valorImc * 100.0) / 100.0;
    }

    /**
     * Clasificación de la Organización Mundial de la Salud (OMS) según el IMC.
     */
    public String getClasificacionImc() {
        if (imc == null) {
            return "No determinado";
        }
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25.0) return "Peso normal";
        if (imc < 30.0) return "Sobrepeso";
        if (imc < 35.0) return "Obesidad Grado I";
        if (imc < 40.0) return "Obesidad Grado II";
        return "Obesidad Grado III (Mórbida)";
    }
}
