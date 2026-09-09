package com.cecarmed.domain.model;

public enum EstadoCita {
    PROGRAMADA("Programada"),
    CONFIRMADA("Confirmada"),
    EN_SALA("En Sala de Espera"),
    EN_CONSULTA("En Consulta"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada"),
    NO_ASISTIO("No Asistió");

    private final String descripcion;

    EstadoCita(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esActiva() {
        return this == PROGRAMADA || this == CONFIRMADA || this == EN_SALA || this == EN_CONSULTA;
    }

    public static EstadoCita fromString(String str) {
        for (EstadoCita e : values()) {
            if (e.name().equalsIgnoreCase(str)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de cita no reconocido: " + str);
    }
}
