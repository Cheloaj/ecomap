package com.ecomap.socio.data.model

enum class BusinessType(val displayName: String) {
    ABARROTES("Abarrotes"),
    CARNICERIA("Carnicería"),
    FRUTERIA("Frutería"),
    VERDULERIA("Verdulería"),
    PANADERIA("Panadería"),
    TORTILLERIA("Tortillería"),
    FARMACIA("Farmacia"),
    PAPELERIA("Papelería"),
    FERRETERIA("Ferretería"),
    OTROS("Otros");

    companion object {
        fun fromString(value: String): BusinessType {
            return entries.find { it.name.lowercase() == value.lowercase() } ?: OTROS
        }
    }
}
