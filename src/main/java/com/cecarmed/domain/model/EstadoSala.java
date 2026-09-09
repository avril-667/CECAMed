package com.cecarmed.domain.model;

public enum EstadoSala {
    ESPERANDO("En Espera"),
    LLAMADO("Llamado a Consulta"),
    EN_CONSULTA("En Consulta"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descripcion;

    EstadoSala(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esActivo() {
        return this == ESPERANDO || this == LLAMADO || this == EN_CONSULTA;
    }

    public static EstadoSala fromString(String str) {
        for (EstadoSala e : values()) {
            if (e.name().equalsIgnoreCase(str)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de sala no reconocido: " + str);
    }
}
