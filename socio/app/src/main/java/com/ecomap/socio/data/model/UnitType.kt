package com.ecomap.socio.data.model

enum class UnitType(val displayName: String) {
    KILOGRAMO("Kilogramo"),
    PIEZA("Pieza"),
    LITRO("Litro"),
    MANOJO("Manojo"),
    DOCENA("Docena"),
    PAQUETE("Paquete");

    companion object {
        fun fromString(value: String): UnitType {
            return entries.find { it.name.lowercase() == value.lowercase() } ?: PIEZA
        }
    }
}

