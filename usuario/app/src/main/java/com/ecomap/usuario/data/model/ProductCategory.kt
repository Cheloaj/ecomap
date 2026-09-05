package com.ecomap.usuario.data.model

data class ProductCategory(
    val name: String,
    val icon: String,
    val color: String, // Hex color for the card
    val units: List<MeasurementUnit>,
    val keywords: List<String> // Para detectar la categoría automáticamente
)

data class MeasurementUnit(
    val name: String,
    val abbreviation: String,
    val emoji: String
)

object ProductCategories {
    val categories = listOf(
        ProductCategory(
            name = "Bebidas",
            icon = "🥤",
            color = "#E1BEE7", // Purple
            units = listOf(
                MeasurementUnit("Litro", "Lt", "🥛"),
                MeasurementUnit("Mililitro", "Ml", "💧"),
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Botella", "Bot", "🍾")
            ),
            keywords = listOf("agua", "refresco", "jugo", "bebida", "soda", "coca", "pepsi", "gaseosa", "té", "cafe")
        ),
        ProductCategory(
            name = "Panadería",
            icon = "🍞",
            color = "#FFF9C4", // Yellow
            units = listOf(
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Paquete", "Paq", "📦"),
                MeasurementUnit("Docena", "Docena", "🔢")
            ),
            keywords = listOf("pan", "bolillo", "telera", "baguette", "tortilla", "pastel", "galleta", "dona", "croissant")
        ),
        ProductCategory(
            name = "Frutas",
            icon = "🍎",
            color = "#FFCDD2", // Red
            units = listOf(
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Gramo", "Gr", "⚖️"),
                MeasurementUnit("Pieza", "Pza", "📦")
            ),
            keywords = listOf("manzana", "plátano", "naranja", "uva", "sandía", "melón", "pera", "fresa", "mango", "piña", "fruta")
        ),
        ProductCategory(
            name = "Verduras",
            icon = "🥬",
            color = "#C8E6C9", // Green
            units = listOf(
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Gramo", "Gr", "⚖️"),
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Manojo", "Man", "🥬")
            ),
            keywords = listOf("lechuga", "tomate", "cebolla", "zanahoria", "papa", "brócoli", "espinaca", "chile", "verdura", "vegetal")
        ),
        ProductCategory(
            name = "Lácteos",
            icon = "🥛",
            color = "#BBDEFB", // Blue
            units = listOf(
                MeasurementUnit("Litro", "Lt", "🥛"),
                MeasurementUnit("Mililitro", "Ml", "💧"),
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Gramo", "Gr", "⚖️"),
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Lata", "Lata", "🥫")
            ),
            keywords = listOf("leche", "queso", "yogurt", "crema", "mantequilla", "lácteo")
        ),
        ProductCategory(
            name = "Carnes",
            icon = "🥩",
            color = "#FFCCBC", // Orange
            units = listOf(
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Gramo", "Gr", "⚖️"),
                MeasurementUnit("Pieza", "Pza", "📦")
            ),
            keywords = listOf("carne", "pollo", "res", "cerdo", "pescado", "bistec", "chuleta", "filete")
        ),
        ProductCategory(
            name = "Abarrotes",
            icon = "🌾",
            color = "#D7CCC8", // Brown
            units = listOf(
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Gramo", "Gr", "⚖️"),
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Paquete", "Paq", "📦"),
                MeasurementUnit("Caja", "Caja", "📦")
            ),
            keywords = listOf("arroz", "frijol", "harina", "azúcar", "sal", "aceite", "pasta", "cereal", "lenteja", "avena", "grano")
        ),
        ProductCategory(
            name = "Otro",
            icon = "✏️",
            color = "#F5F5F5", // Gray
            units = listOf(
                MeasurementUnit("Pieza", "Pza", "📦"),
                MeasurementUnit("Kilogramo", "Kg", "⚖️"),
                MeasurementUnit("Litro", "Lt", "🥛"),
                MeasurementUnit("Paquete", "Paq", "📦")
            ),
            keywords = listOf()
        )
    )

    fun detectCategory(productName: String): ProductCategory? {
        val normalizedName = productName.lowercase().trim()

        // Buscar en las keywords de cada categoría
        return categories.firstOrNull { category ->
            category.keywords.any { keyword ->
                normalizedName.contains(keyword)
            }
        }
    }
}
