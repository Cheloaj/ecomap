package com.ecomap.socio.presentation.ui.main

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de Dashboard del Negocio - Diseño Minimalista Nu
 * Muestra información detallada y opciones de administración del negocio aprobado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDashboardScreen(
    businessId: String,
    onNavigateBack: () -> Unit,
    viewModel: BusinessViewModel = hiltViewModel()
) {
    val businesses by viewModel.businesses.collectAsState()
    val business = remember(businesses) {
        businesses.firstOrNull { it.id == businessId }
    }
    val view = LocalView.current

    // ✅ Animación de entrada
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = business?.businessName ?: "Mi Negocio",
                        color = NuColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NuColors.Background)
            )
        },
        containerColor = NuColors.Background
    ) { paddingValues ->
        if (business == null) {
            // Estado de carga o negocio no encontrado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NuColors.Primary)
            }
        } else {
            // Dashboard del negocio
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ✅ Header con animación
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                        initialOffsetY = { -30 },
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                ) {
                    DashboardHeader(business = business)
                }

                // ✅ Estadísticas rápidas con animación
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { 30 },
                        animationSpec = tween(600, delayMillis = 100, easing = FastOutSlowInEasing)
                    )
                ) {
                    DashboardStatsSection()
                }

                // ✅ Opciones de administración con animación
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { 30 },
                        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                    )
                ) {
                    DashboardOptionsSection(view = view)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(business: Business) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Bienvenido a",
            fontSize = 16.sp,
            color = NuColors.TextSecondary
        )

        Text(
            text = business.businessName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            lineHeight = 38.sp
        )

        // ✅ Badge de estado según verificationStatus
        val (badgeColor, badgeText) = when (business.verificationStatus) {
            "approved" -> Pair(NuColors.Success, "Activo")
            "pending" -> Pair(Color(0xFFFFB300), "Pendiente")
            "rejected" -> Pair(NuColors.Error, "Rechazado")
            else -> Pair(NuColors.TextSecondary, "Desconocido")
        }

        Surface(
            color = badgeColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = badgeText,
                    fontSize = 13.sp,
                    color = badgeColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DashboardOptionsSection(view: android.view.View) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "¿Qué quieres hacer?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary
        )

        // ✅ Cards minimalistas sin bordes
        DashboardOptionCard(
            icon = Icons.Default.LocalOffer,
            title = "Mis Ofertas",
            description = "Publicar ofertas especiales",
            view = view,
            onClick = { /* TODO: Navegar a ofertas */ }
        )

        DashboardOptionCard(
            icon = Icons.Default.Schedule,
            title = "Programar Ofertas",
            description = "Agendar publicaciones futuras",
            view = view,
            onClick = { /* TODO: Navegar a programadas */ }
        )

        DashboardOptionCard(
            icon = Icons.Default.BarChart,
            title = "Estadísticas",
            description = "Ver rendimiento",
            view = view,
            onClick = { /* TODO: Navegar a estadísticas */ }
        )

        DashboardOptionCard(
            icon = Icons.Default.Settings,
            title = "Configuración",
            description = "Editar tu negocio",
            view = view,
            onClick = { /* TODO: Navegar a configuración */ }
        )
    }
}

@Composable
private fun DashboardOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    view: android.view.View,
    onClick: () -> Unit
) {
    // ✅ Card minimalista sin elevación, bordes suaves
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NuColors.Surface),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✅ Icono en círculo suave
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NuColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NuColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DashboardStatsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Resumen",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary
        )

        // ✅ Grid de estadísticas minimalista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Ofertas",
                value = "0",
                icon = Icons.Default.LocalOffer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Vistas",
                value = "0",
                icon = Icons.Default.Visibility,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NuColors.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )

            Text(
                text = label,
                fontSize = 13.sp,
                color = NuColors.TextSecondary
            )
        }
    }
}
