package com.cecarmed.domain.model;

public enum Genero {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro");

    private final String descripcion;

    Genero(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static Genero fromString(String str) {
        for (Genero g : values()) {
            if (g.name().equalsIgnoreCase(str) || g.descripcion.equalsIgnoreCase(str)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Género no reconocido: " + str);
    }
}
