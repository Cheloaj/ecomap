package com.ecomap.usuario.presentation.ui.basket

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.data.model.*
import com.ecomap.usuario.presentation.ui.components.UpgradePromptScreen
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.BasketAnalysisState
import com.ecomap.usuario.presentation.viewmodel.BasketOptimizerViewModel
import com.ecomap.usuario.presentation.viewmodel.BasketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketAnalysisScreen(
    onNavigateBack: () -> Unit,
    basketViewModel: BasketViewModel = hiltViewModel(),
    optimizerViewModel: BasketOptimizerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToSubscription: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val basketState by basketViewModel.basketState.collectAsState()
    val analysisState by optimizerViewModel.analysisState.collectAsState()
    val transportMethod by optimizerViewModel.transportMethod.collectAsState()

    // Verificar acceso Pro en tiempo real
    val hasProAccess by authViewModel.subscriptionMonitor.isProStatus.collectAsState()

    // Si no tiene acceso Pro, mostrar pantalla de upgrade
    if (!hasProAccess) {
        UpgradePromptScreen(
            feature = "Análisis completo de compras con comparación de precios y optimización de rutas",
            onUpgrade = onNavigateToSubscription,
            onDismiss = onNavigateBack
        )
        return
    }

    // Obtener ubicación del usuario
    val locationManager = remember {
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    }

    // Analizar canasta cuando se carga la pantalla
    LaunchedEffect(Unit) {
        val items = (basketState as? com.ecomap.usuario.presentation.viewmodel.BasketUiState.Success)?.items ?: emptyList()
        if (items.isNotEmpty()) {
            try {
                // Obtener ubicación GPS del usuario
                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

                val userLatitude = location?.latitude ?: 0.0
                val userLongitude = location?.longitude ?: 0.0

                optimizerViewModel.analyzeBasket(items, userLatitude, userLongitude)
            } catch (e: SecurityException) {
                // Si no hay permisos, usar ubicación por defecto
                optimizerViewModel.analyzeBasket(items, 0.0, 0.0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Compra") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when (val state = analysisState) {
            is BasketAnalysisState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is BasketAnalysisState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is BasketAnalysisState.Success -> {
                AnalysisContent(
                    analysis = state.analysis,
                    onTransportMethodChange = { method ->
                        optimizerViewModel.setTransportMethod(method)
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            is BasketAnalysisState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Analizando tu canasta...")
                }
            }
        }
    }
}

@Composable
private fun AnalysisContent(
    analysis: BasketAnalysis,
    onTransportMethodChange: (TransportMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con información general
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tu canasta tiene ${analysis.businessGroups.sumOf { it.items.size }} productos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "de ${analysis.numberOfStores} ${if (analysis.numberOfStores == 1) "tienda" else "tiendas diferentes"}",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Grupos por negocio
        items(analysis.businessGroups) { group ->
            BusinessGroupCard(group)
        }

        // Resumen total
        item {
            TotalSummaryCard(analysis)
        }

        // Sugerencia inteligente
        analysis.suggestion?.let { suggestion ->
            item {
                SmartSuggestionCard(suggestion)
            }
        }

        // Selector de método de transporte
        item {
            TransportMethodSelector(
                currentMethod = analysis.transportMethod,
                onMethodChange = onTransportMethodChange
            )
        }
    }
}

@Composable
private fun BusinessGroupCard(group: BusinessGroup) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nombre del negocio y distancia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.business.businessName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", group.distanceKm)} km de ti",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Lista de productos
            group.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.product?.name} x${item.quantity}",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${String.format("%.0f", (item.product?.effectivePrice ?: 0.0) * item.quantity)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Resumen del negocio
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", color = Color.Gray)
                    Text(
                        "$${String.format("%.0f", group.subtotal)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transporte:", color = Color.Gray)
                    Text(
                        "$${String.format("%.0f", group.transportCost)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total tienda:", fontWeight = FontWeight.Bold)
                    Text(
                        "$${String.format("%.0f", group.totalWithTransport)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalSummaryCard(analysis: BasketAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RESUMEN TOTAL",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Productos:", color = Color.White)
                Text(
                    "$${String.format("%.0f", analysis.totalProductsCost)}",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Transporte total:", color = Color.White)
                Text(
                    "$${String.format("%.0f", analysis.totalTransportCost)}",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Divider(color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "GRAN TOTAL:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "$${String.format("%.0f", analysis.grandTotal)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (analysis.transportPercentage > 0) {
                Text(
                    text = "Transporte representa ${analysis.transportPercentage}% de tu compra",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartSuggestionCard(suggestion: SmartSuggestion) {
    val backgroundColor = when (suggestion.type) {
        SuggestionType.HIGH_TRANSPORT_COST -> Color(0xFFFF9800)
        SuggestionType.CONCENTRATED_PURCHASE -> Color(0xFF2196F3)
        SuggestionType.SINGLE_STORE -> Color(0xFF4CAF50)
        else -> Color(0xFF9C27B0)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = backgroundColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = suggestion.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = suggestion.message,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            suggestion.potentialSavings?.let { savings ->
                if (savings > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ahorro potencial: $${String.format("%.0f", savings)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = backgroundColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportMethodSelector(
    currentMethod: TransportMethod,
    onMethodChange: (TransportMethod) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Método de transporte",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cálculo basado en ${currentMethod.displayName}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            // TODO: Agregar selector si necesario
        }
    }
}
