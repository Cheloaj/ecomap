package com.ecomap.socio.presentation.ui.upgrade

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit,
    onUpgradeToPro: () -> Unit
) {
    var showPaymentDialog by remember { mutableStateOf(false) }

    // Animación de rotación para la estrella
    val infiniteTransition = rememberInfiniteTransition(label = "star")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = NuColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header con estrella animada
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                Color(0xFFFFD700).copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(rotation),
                    tint = Color(0xFFFFB300)
                )
            }

            Text(
                text = "Desbloquea Todo el Potencial",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Lleva tu negocio al siguiente nivel con funciones avanzadas",
                fontSize = 16.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Comparación de planes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlanCard(
                    title = "Básico",
                    price = "Gratis",
                    features = listOf(
                        PlanFeature("Hasta 5 productos", true),
                        PlanFeature("Publicación manual", true),
                        PlanFeature("Programación", false)
                    ),
                    isRecommended = false,
                    modifier = Modifier.weight(1f)
                )

                PlanCard(
                    title = "Pro",
                    price = "$99/mes",
                    features = listOf(
                        PlanFeature("Productos ilimitados", true),
                        PlanFeature("Publicación manual", true),
                        PlanFeature("Programación automática", true)
                    ),
                    isRecommended = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de beneficios destacados
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "¿Qué obtienes con Pro?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )

                    BenefitItem(
                        icon = Icons.Default.Inventory,
                        title = "Productos Ilimitados",
                        description = "Publica todos los productos que necesites sin restricciones"
                    )

                    HorizontalDivider(color = NuColors.Background)

                    BenefitItem(
                        icon = Icons.Default.Schedule,
                        title = "Programación Automática",
                        description = "Programa tus productos para que se publiquen automáticamente"
                    )

                    HorizontalDivider(color = NuColors.Background)

                    BenefitItem(
                        icon = Icons.Default.Notifications,
                        title = "Notificaciones Semanales",
                        description = "Recibe reportes automáticos de tu actividad"
                    )

                    HorizontalDivider(color = NuColors.Background)

                    BenefitItem(
                        icon = Icons.Default.SupportAgent,
                        title = "Soporte Prioritario",
                        description = "Atención rápida y personalizada cuando lo necesites"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de upgrade
            Button(
                onClick = { showPaymentDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                )
            ) {
                Icon(
                    Icons.Default.Upgrade,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Actualizar a Plan Pro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Cancela cuando quieras • Sin compromisos",
                fontSize = 13.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPaymentDialog) {
        PaymentOptionsDialog(
            onDismiss = { showPaymentDialog = false },
            onPaymentSelected = { method ->
                showPaymentDialog = false
                // TODO: Procesar pago según el método
                onUpgradeToPro()
            }
        )
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    features: List<PlanFeature>,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isRecommended) Color(0xFFFFD700) else NuColors.Background
    val backgroundColor = if (isRecommended) Color(0xFFFFF9E6) else Color.White

    Card(
        modifier = modifier
            .then(
                if (isRecommended) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRecommended) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFB300)
                ) {
                    Text(
                        text = "Recomendado",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )

            Text(
                text = price,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRecommended) Color(0xFFFFB300) else NuColors.Primary
            )

            HorizontalDivider(color = NuColors.Background)

            features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (feature.included) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (feature.included) NuColors.Success else NuColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = feature.name,
                        fontSize = 12.sp,
                        color = if (feature.included) NuColors.TextPrimary else NuColors.TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

data class PlanFeature(
    val name: String,
    val included: Boolean
)

@Composable
fun BenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NuColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NuColors.TextPrimary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = NuColors.TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PaymentOptionsDialog(
    onDismiss: () -> Unit,
    onPaymentSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Método de Pago",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecciona tu método de pago preferido",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentMethodOption(
                    icon = Icons.Default.CreditCard,
                    name = "Tarjeta de Crédito/Débito",
                    onClick = { onPaymentSelected("card") }
                )

                PaymentMethodOption(
                    icon = Icons.Default.AccountBalance,
                    name = "Transferencia Bancaria",
                    onClick = { onPaymentSelected("transfer") }
                )

                PaymentMethodOption(
                    icon = Icons.Default.QrCode,
                    name = "Pago con QR (OXXO)",
                    onClick = { onPaymentSelected("oxxo") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = NuColors.TextSecondary)
            }
        }
    )
}

@Composable
fun PaymentMethodOption(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = NuColors.Background
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = NuColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NuColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
