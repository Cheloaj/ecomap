package com.ecomap.socio.presentation.ui.products

import androidx.compose.ui.graphics.Color

// 🤖 SISTEMA DE CATEGORIZACIÓN INTELIGENTE CON VALIDACIÓN ESPECÍFICA POR PRODUCTO
data class CategoryInfo(
    val name: String,
    val icon: String,
    val keywords: List<String>,
    val color: Color,
    val relatedUnits: List<String>
)

// Unidades de medida comunes
data class UnitOption(val short: String, val long: String, val emoji: String)

// 🛡️ Sistema de validación inteligente
data class ValidationResult(
    val isValid: Boolean,
    val confidence: Float, // 0.0 a 1.0
    val message: String?
)

// 📋 CANASTA BÁSICA OFICIAL MÉXICO - Unidades EXACTAS según estándar
val productSpecificUnits = mapOf(
    // ========== GRASAS ==========
    "aceite" to listOf("Lt", "Ml", "Bot"), // #1 - Aceite vegetal (Botella 946ml)

    // ========== CEREALES ==========
    "arroz" to listOf("Kg", "Gr"), // #2 - Arroz en grano (Kg)
    "pan" to listOf("Paq", "Kg", "Pza"), // #17 - Pan blanco de caja (Paquete 680g)
    "pasta" to listOf("Paq", "Gr", "Kg"), // #20 - Pasta para sopa (Paquete 220g)
    "tortilla" to listOf("Kg", "Pza"), // #23 - Tortilla de maíz (Kg)

    // ========== PROTEÍNA - ENLATADOS ==========
    "atún" to listOf("Lata", "Gr"), // #3 - Atún en hojuela (Latas 140g)
    "sardina" to listOf("Lata", "Gr"), // #22 - Sardina en tomate (Lata 425g)

    // ========== PROTEÍNA - CARNES (SIEMPRE Kg) ==========
    "res" to listOf("Kg", "Gr"), // #5 - Carne de res (Kg)
    "cerdo" to listOf("Kg", "Gr"), // #8 - Carne de cerdo (Kg)
    "puerco" to listOf("Kg", "Gr"), // #8 - Carne de cerdo (Kg)
    "pollo" to listOf("Kg", "Gr"), // #21 - Carne de pollo (Kg)
    "carne" to listOf("Kg", "Gr"), // Genérico carnes
    "bistec" to listOf("Kg", "Gr"),
    "molida" to listOf("Kg", "Gr"),
    "costilla" to listOf("Kg", "Gr"),
    "chuleta" to listOf("Kg", "Gr"),

    // ========== PROTEÍNA - HUEVO ==========
    "huevo" to listOf("Pza", "Docena", "Paq"), // #10 - Huevo gallina (Paquete 18 piezas)

    // ========== LEGUMINOSAS ==========
    "frijol" to listOf("Paq", "Kg", "Gr"), // #9 - Frijol negro (Paquete 900g)
    "lentejas" to listOf("Paq", "Kg", "Gr"),
    "garbanzo" to listOf("Paq", "Kg", "Gr"),

    // ========== LÁCTEOS ==========
    "leche" to listOf("Lt", "Ml"), // #13 - Leche de vaca (Litros)
    "queso" to listOf("Kg", "Gr"),
    "yogurt" to listOf("Pza", "Lt", "Ml"),
    "yogur" to listOf("Pza", "Lt", "Ml"),
    "crema" to listOf("Ml", "Lt", "Pza"),
    "mantequilla" to listOf("Gr", "Kg", "Pza"),

    // ========== VERDURAS (SIEMPRE Kg) ==========
    "cebolla" to listOf("Kg", "Gr"), // #6 - Cebolla blanca (Kg)
    "chile" to listOf("Kg", "Gr"), // #7 - Chile jalapeño (Kg)
    "jalapeño" to listOf("Kg", "Gr"), // #7 - Chile jalapeño (Kg)
    "jitomate" to listOf("Kg", "Gr"), // #12 - Jitomate Saladet (Kg)
    "tomate" to listOf("Kg", "Gr"), // #12 - Jitomate Saladet (Kg)
    "zanahoria" to listOf("Kg", "Gr"), // #24 - Zanahoria (Kg)

    // ========== TUBÉRCULOS ==========
    "papa" to listOf("Kg", "Gr"), // #18 - Papa blanca (Kg)

    // ========== FRUTAS (Kg) ==========
    "limón" to listOf("Kg", "Gr"), // #14 - Limón (Kg)
    "manzana" to listOf("Kg", "Gr"), // #15 - Manzana (Kg)
    "plátano" to listOf("Kg", "Gr"), // #16 - Plátano (Kg)
    "banana" to listOf("Kg", "Gr"),
    "naranja" to listOf("Kg", "Gr"),
    "mango" to listOf("Kg", "Gr"),
    "uva" to listOf("Kg", "Gr"),
    "fresa" to listOf("Kg", "Gr"),
    "sandía" to listOf("Kg", "Pza"),
    "melón" to listOf("Kg", "Pza"),
    "piña" to listOf("Kg", "Pza"),
    "papaya" to listOf("Kg", "Pza"),

    // ========== OTROS ==========
    "azúcar" to listOf("Kg", "Gr"), // #4 - Azúcar estándar (Kg)
    "sal" to listOf("Kg", "Gr"),
    "harina" to listOf("Kg", "Gr"),

    // ========== HIGIENE ==========
    "jabón" to listOf("Pza", "Paq"), // #11 - Jabón de tocador (Pieza)
    "papel" to listOf("Paq", "Pza"), // #19 - Papel higiénico (Bolsa 4 rollos)
    "higiénico" to listOf("Paq", "Pza"),
    "shampoo" to listOf("Lt", "Ml", "Pza"),
    "detergente" to listOf("Kg", "Lt", "Pza"),
    "cloro" to listOf("Lt", "Ml"),

    // ========== VERDURAS ADICIONALES ==========
    "lechuga" to listOf("Pza", "Kg"),
    "cilantro" to listOf("Man", "Pza"),
    "perejil" to listOf("Man", "Pza"),
    "aguacate" to listOf("Kg", "Pza"),
    "calabaza" to listOf("Kg", "Pza"),
    "pepino" to listOf("Kg", "Pza"),
    "elote" to listOf("Pza", "Kg"),

    // ========== BEBIDAS ==========
    "agua" to listOf("Lt", "Ml", "Pza"),
    "refresco" to listOf("Lt", "Ml", "Pza"),
    "jugo" to listOf("Lt", "Ml", "Pza"),
    "coca" to listOf("Lt", "Ml", "Pza"),
    "pepsi" to listOf("Lt", "Ml", "Pza")
)

val smartCategories = listOf(
    CategoryInfo("Lácteos", "🥛",
        listOf("leche", "queso", "yogurt", "yogur", "crema", "mantequilla", "panela", "oaxaca", "manchego"),
        Color(0xFF2196F3),
        listOf("Lt", "Ml", "Kg", "Gr", "Pza")),

    CategoryInfo("Carnes", "🥩",
        listOf("pollo", "res", "bistec", "puerco", "cerdo", "chuleta", "jamón", "salchicha", "tocino", "carne", "milanesa", "costilla", "molida"),
        Color(0xFFE91E63),
        listOf("Kg", "Gr")),

    CategoryInfo("Pescados", "🐟",
        listOf("pescado", "salmón", "camarón", "marisco", "mojarra", "tilapia", "huachinango"),
        Color(0xFF00BCD4),
        listOf("Kg", "Gr", "Pza")),

    CategoryInfo("Frutas", "🍎",
        listOf("manzana", "plátano", "banana", "naranja", "uva", "limón", "mango", "fresa", "piña", "sandía", "pera", "kiwi", "melón", "papaya", "guayaba", "durazno"),
        Color(0xFF4CAF50),
        listOf("Kg", "Gr", "Pza")),

    CategoryInfo("Verduras", "🥬",
        listOf("tomate", "jitomate", "cebolla", "papa", "lechuga", "zanahoria", "chile", "aguacate", "ajo", "brócoli", "cilantro", "espinaca", "calabaza", "chayote", "elote", "pepino", "rábano"),
        Color(0xFF8BC34A),
        listOf("Kg", "Gr", "Man", "Pza")),

    CategoryInfo("Abarrotes", "🛒",
        listOf("arroz", "frijol", "aceite", "huevo", "lata", "sopa", "pasta", "azúcar", "sal", "harina", "atún", "galleta", "cereal", "avena", "maíz", "lentejas", "garbanzo"),
        Color(0xFFFF9800),
        listOf("Kg", "Gr", "Paq", "Lata", "Bot", "Lt", "Ml", "Pza", "Docena")),

    CategoryInfo("Panadería", "🍞",
        listOf("pan", "tortilla", "bolillo", "telera", "pastel", "donas", "galleta", "bollo", "concha", "birote"),
        Color(0xFFFFEB3B),
        listOf("Pza", "Kg", "Docena")),

    CategoryInfo("Bebidas", "🥤",
        listOf("agua", "refresco", "coca", "pepsi", "jugo", "cerveza", "vino", "licor", "té", "café", "sprite", "fanta"),
        Color(0xFF9C27B0),
        listOf("Lt", "Ml", "Pza")),

    CategoryInfo("Limpieza", "🧼",
        listOf("jabón", "cloro", "detergente", "shampoo", "papel", "suavitel", "pinol", "fabuloso", "ariel"),
        Color(0xFF03A9F4),
        listOf("Lt", "Ml", "Kg", "Pza")),

    CategoryInfo("Otros", "📦",
        listOf("otro", "varios"),
        Color(0xFF757575),
        listOf("Pza", "Kg", "Lt", "Gr", "Ml"))
)

val commonUnits = listOf(
    UnitOption("Pza", "Pieza", "📦"),
    UnitOption("Kg", "Kilogramo", "⚖️"),
    UnitOption("Gr", "Gramo", "⚖️"),
    UnitOption("Lt", "Litro", "🥛"),
    UnitOption("Ml", "Mililitro", "💧"),
    UnitOption("Caja", "Caja", "📦"),
    UnitOption("Paq", "Paquete", "📦"),
    UnitOption("Bot", "Botella", "🍾"),
    UnitOption("Man", "Manojo", "🌿"),
    UnitOption("Lata", "Lata", "🥫"),
    UnitOption("Docena", "Docena", "🔢"),
    UnitOption("Otro", "Personalizado", "✏️")
)

fun validateCategoryAndUnit(
    productName: String,
    category: CategoryInfo?,
    selectedUnit: String
): ValidationResult {
    if (category == null) {
        return ValidationResult(true, 1.0f, null)
    }

    val lowerName = productName.lowercase().trim()

    // 1. PRIMERO: Buscar coincidencia EXACTA en el mapeo de productos específicos
    val specificUnits = productSpecificUnits.entries.firstOrNull { (keyword, _) ->
        lowerName.contains(keyword)
    }?.value

    // Si hay unidades específicas para este producto, usarlas
    val validUnits = specificUnits ?: category.relatedUnits
    val unitIsValid = validUnits.contains(selectedUnit)

    // 2. Calcular confianza
    val exactMatch = category.keywords.any { keyword ->
        lowerName.contains(keyword)
    }
    val hasSpecificMapping = specificUnits != null

    val confidence = when {
        hasSpecificMapping && exactMatch -> 1.0f  // 100% si hay mapeo específico
        exactMatch -> 0.9f
        else -> 0.6f
    }

    // 3. Mensajes ESPECÍFICOS basados en CANASTA BÁSICA OFICIAL
    val specificMessage = if (!unitIsValid) {
        when {
            // LÁCTEOS
            lowerName.contains("leche") ->
                "⚠️ Leche: Líquido que se mide en Litros (Lt) o Mililitros (Ml)"
            lowerName.contains("queso") ->
                "⚠️ Queso: Sólido que se vende por Kilogramo (Kg) o Gramos (Gr)"

            // CARNES (Canasta básica: res, cerdo, pollo - SIEMPRE Kg)
            lowerName.contains("res") || lowerName.contains("bistec") ->
                "⚠️ Carne de res: SIEMPRE se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("cerdo") || lowerName.contains("puerco") ->
                "⚠️ Carne de cerdo: SIEMPRE se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("pollo") ->
                "⚠️ Carne de pollo: SIEMPRE se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("carne") ->
                "⚠️ Carnes: SIEMPRE se venden por Kilogramo (Kg) o Gramos (Gr)"

            // VERDURAS (Canasta básica: cebolla, chile, jitomate, zanahoria - SIEMPRE Kg)
            lowerName.contains("cebolla") ->
                "⚠️ Cebolla: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("chile") || lowerName.contains("jalapeño") ->
                "⚠️ Chile: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("jitomate") || lowerName.contains("tomate") ->
                "⚠️ Jitomate/Tomate: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("zanahoria") ->
                "⚠️ Zanahoria: Se vende por Kilogramo (Kg) o Gramos (Gr)"

            // TUBÉRCULOS (Canasta básica: papa - Kg)
            lowerName.contains("papa") ->
                "⚠️ Papa: Se vende por Kilogramo (Kg) o Gramos (Gr)"

            // FRUTAS (Canasta básica: limón, manzana, plátano - Kg)
            lowerName.contains("limón") ->
                "⚠️ Limón: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("manzana") ->
                "⚠️ Manzana: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("plátano") || lowerName.contains("banana") ->
                "⚠️ Plátano: Se vende por Kilogramo (Kg) o Gramos (Gr)"

            // CEREALES (Canasta básica: arroz Kg, pan/pasta Paq, tortilla Kg)
            lowerName.contains("arroz") ->
                "⚠️ Arroz: Se vende por Kilogramo (Kg) o Gramos (Gr)"
            lowerName.contains("pan") && lowerName.contains("caja") ->
                "⚠️ Pan de caja: Se vende por Paquete (Paq) o Kilogramo (Kg)"
            lowerName.contains("pasta") ->
                "⚠️ Pasta: Se vende por Paquete (Paq), Gramos (Gr) o Kilogramo (Kg)"
            lowerName.contains("tortilla") ->
                "⚠️ Tortilla: Se vende por Kilogramo (Kg) o Pieza (Pza)"

            // LEGUMINOSAS (Canasta básica: frijol - Paquete 900g)
            lowerName.contains("frijol") || lowerName.contains("lentejas") || lowerName.contains("garbanzo") ->
                "⚠️ Leguminosas: Se venden por Paquete (Paq), Kilogramo (Kg) o Gramos (Gr)"

            // ENLATADOS (Canasta básica: atún Lata 140g, sardina Lata 425g)
            lowerName.contains("atún") ->
                "⚠️ Atún: Se vende en Lata o por Gramos (Gr)"
            lowerName.contains("sardina") ->
                "⚠️ Sardina: Se vende en Lata o por Gramos (Gr)"

            // ACEITE (Canasta básica: botella 946ml)
            lowerName.contains("aceite") ->
                "⚠️ Aceite: Líquido que se vende en Litros (Lt), Mililitros (Ml) o Botella (Bot)"

            // HUEVO (Canasta básica: paquete 18 piezas)
            lowerName.contains("huevo") ->
                "⚠️ Huevo: Se vende por Pieza (Pza), Docena o Paquete (Paq)"

            // OTROS (Canasta básica: azúcar Kg)
            lowerName.contains("azúcar") || lowerName.contains("sal") || lowerName.contains("harina") ->
                "⚠️ ${lowerName.capitalize()}: Se vende por Kilogramo (Kg) o Gramos (Gr)"

            // HIGIENE (Canasta básica: jabón Pza, papel Paq 4 rollos)
            lowerName.contains("jabón") ->
                "⚠️ Jabón: Se vende por Pieza (Pza) o Paquete (Paq)"
            lowerName.contains("papel") || lowerName.contains("higiénico") ->
                "⚠️ Papel higiénico: Se vende por Paquete (Paq) o Pieza (Pza)"

            // GENÉRICOS
            category.name == "Carnes" ->
                "⚠️ Todas las carnes se venden por Kilogramo (Kg) o Gramos (Gr)"
            category.name == "Verduras" ->
                "⚠️ Las verduras se venden por Kilogramo (Kg) o Gramos (Gr)"
            category.name == "Frutas" ->
                "⚠️ Las frutas se venden por Kilogramo (Kg) o Gramos (Gr)"
            category.name == "Bebidas" ->
                "⚠️ Las bebidas se miden en Litros (Lt) o Mililitros (Ml)"

            else ->
                "⚠️ Unidades válidas: ${validUnits.take(3).joinToString(", ")}"
        }
    } else null

    return ValidationResult(
        isValid = unitIsValid,
        confidence = confidence,
        message = specificMessage
    )
}
