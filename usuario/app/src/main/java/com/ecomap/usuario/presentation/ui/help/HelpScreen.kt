package com.ecomap.usuario.presentation.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecomap.usuario.ui.theme.AppleColors

data class FAQItem(
    val question: String,
    val answer: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val faqList = remember {
        listOf(
            FAQItem(
                question = "¿Cómo busco negocios ecológicos?",
                answer = "Puedes buscar negocios de tres formas:\n\n1. En el mapa: Explora los marcadores verdes\n2. Búsqueda: Usa el ícono de lupa en el mapa\n3. Pantalla de búsqueda: Accede desde el menú lateral",
                icon = Icons.Default.Search
            ),
            FAQItem(
                question = "¿Cómo guardo mis favoritos?",
                answer = "En la pantalla de detalles de un negocio, toca el ícono de corazón en la parte superior. Los negocios favoritos se guardan automáticamente y puedes verlos en la sección 'Mis Favoritos' del menú.",
                icon = Icons.Default.Favorite
            ),
            FAQItem(
                question = "¿Qué son los clusters en el mapa?",
                answer = "Los círculos verdes con números representan grupos de negocios cercanos. Al tocarlos, se abre una lista para que elijas cuál visitar. Al hacer zoom, los clusters se separan automáticamente.",
                icon = Icons.Default.Place
            ),
            FAQItem(
                question = "¿Cómo reporto un producto no ecológico?",
                answer = "Toca el botón amarillo en el mapa (ícono de alerta). Completa el formulario con la información del producto y envíalo. Tu reporte ayuda a mantener la calidad de la plataforma.",
                icon = Icons.Default.Report
            ),
            FAQItem(
                question = "¿Cómo funciona el login con huella?",
                answer = "Al iniciar sesión, marca la opción 'Recordarme'. La próxima vez que abras la app, podrás usar tu huella dactilar. Para desactivarlo, ve a Perfil → Olvidar este dispositivo.",
                icon = Icons.Default.Fingerprint
            ),
            FAQItem(
                question = "¿Cómo veo ofertas especiales?",
                answer = "Accede a la sección 'Ofertas' desde el menú lateral. Allí encontrarás todos los negocios con descuentos o promociones activas.",
                icon = Icons.Default.LocalOffer
            ),
            FAQItem(
                question = "¿Puedo usar la app sin conexión?",
                answer = "Necesitas conexión a internet para cargar los datos de los negocios. Sin embargo, los favoritos guardados pueden verse sin conexión temporal.",
                icon = Icons.Default.WifiOff
            ),
            FAQItem(
                question = "¿Cómo cambio mi contraseña?",
                answer = "Ve a Perfil → Editar perfil. Allí podrás actualizar tu nombre de usuario. Para cambiar la contraseña, usa la opción 'Olvidé mi contraseña' en la pantalla de login.",
                icon = Icons.Default.Lock
            ),
            FAQItem(
                question = "¿Los negocios son verificados?",
                answer = "Sí, todos los negocios en EcoMap son verificados antes de aparecer en el mapa. Nuestro equipo confirma que cumplan con estándares ecológicos.",
                icon = Icons.Default.VerifiedUser
            ),
            FAQItem(
                question = "¿Cómo contacto soporte técnico?",
                answer = "Para soporte técnico, envía un correo a: soporte@ecomap.com\n\nIncluye capturas de pantalla si es posible y describe el problema detalladamente.",
                icon = Icons.Default.ContactSupport
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Ayuda", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppleColors.IOSBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = AppleColors.IOSBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "¿Necesitas ayuda?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preguntas frecuentes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // FAQ Items
            items(faqList) { faq ->
                FAQItemCard(faq = faq)
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¿No encontraste lo que buscabas?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Contáctanos en: soporte@ecomap.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleColors.IOSBlue
                )
            }
        }
    }
}

@Composable
private fun FAQItemCard(faq: FAQItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = faq.icon,
                    contentDescription = null,
                    tint = AppleColors.IOSGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }
            }
        }
    }
}
