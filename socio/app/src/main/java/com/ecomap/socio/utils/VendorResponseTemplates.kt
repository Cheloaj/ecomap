package com.ecomap.socio.utils

/**
 * Quick message templates for vendors to respond to product reviews
 * These are predefined messages that vendors can select to quickly respond to customers
 */
object VendorResponseTemplates {

    data class Template(
        val id: String,
        val label: String,
        val message: String
    )

    /**
     * List of predefined response templates
     */
    val templates = listOf(
        Template(
            id = "thanks_preference",
            label = "Agradecer",
            message = "¡Gracias por tu preferencia! Nos alegra que hayas disfrutado del producto."
        ),
        Template(
            id = "excellent_arrived",
            label = "Llegó bien",
            message = "¡Excelente! Nos da gusto que el producto llegó en perfectas condiciones."
        ),
        Template(
            id = "thanks_feedback",
            label = "Gracias por opinión",
            message = "Gracias por compartir tu experiencia. Tu opinión es muy valiosa para nosotros."
        ),
        Template(
            id = "will_improve",
            label = "Mejoraremos",
            message = "Agradecemos tus comentarios. Trabajaremos para mejorar tu experiencia en futuras compras."
        ),
        Template(
            id = "sorry_issue",
            label = "Disculpas",
            message = "Lamentamos los inconvenientes. Por favor contáctanos para resolver cualquier problema."
        ),
        Template(
            id = "thanks_support",
            label = "Gracias por apoyo",
            message = "¡Muchas gracias por tu apoyo! Esperamos verte pronto de nuevo."
        ),
        Template(
            id = "quality_product",
            label = "Calidad garantizada",
            message = "Nos esforzamos por ofrecer productos de la mejor calidad. ¡Gracias por tu confianza!"
        ),
        Template(
            id = "value_opinion",
            label = "Valoramos opinión",
            message = "Valoramos mucho tu opinión. Seguiremos trabajando para brindarte el mejor servicio."
        )
    )

    /**
     * Get a template by its ID
     */
    fun getTemplateById(id: String): Template? {
        return templates.find { it.id == id }
    }

    /**
     * Get template message by ID
     */
    fun getMessageById(id: String): String {
        return templates.find { it.id == id }?.message ?: ""
    }
}
