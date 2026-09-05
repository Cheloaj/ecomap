package com.ecomap.socio.presentation.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.viewmodel.StatisticsViewModel
import com.ecomap.socio.utils.UiState
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.datetime.toLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val statistics by viewModel.statistics.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") }
            )
        }
    ) { paddingValues ->
        when (val state = statisticsState) {
            is UiState.Loading, UiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                if (state.message.contains("Plan Pro", ignoreCase = true)) {
                    UpgradeToProForStatistics(modifier = Modifier.fillMaxSize().padding(paddingValues))
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { viewModel.loadStatistics() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total",
                            value = statistics.totalOffers.toString(),
                            icon = Icons.Default.ShoppingBag,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Activas",
                            value = statistics.activeOffers.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Expiradas",
                            value = statistics.expiredOffers.toString(),
                            icon = Icons.Default.Cancel,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Precio Promedio",
                            value = "$${String.format("%.2f", statistics.averagePrice)}",
                            icon = Icons.Default.AttachMoney,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider()

                    if (statistics.mostExpensiveOffer != null && statistics.mostCheapOffer != null) {
                        Text(
                            text = "Precios Extremos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                                    Column {
                                        Text(text = "Más Cara", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = "${statistics.mostExpensiveOffer!!.productName} - $${statistics.mostExpensiveOffer!!.price}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                HorizontalDivider()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null)
                                    Column {
                                        Text(text = "Más Barata", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = "${statistics.mostCheapOffer!!.productName} - $${statistics.mostCheapOffer!!.price}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    if (statistics.dailyOffers.isNotEmpty()) {
                        Text(text = "Ofertas por Día (Últimos 7 días)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        DailyOffersChart(dailyData = statistics.dailyOffers)
                    }

                    HorizontalDivider()

                    if (statistics.offersByCategory.isNotEmpty()) {
                        Text(text = "Top 5 Productos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                statistics.offersByCategory.entries.forEachIndexed { index, entry ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                                Text(text = "${index + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                            }
                                            Text(text = entry.key, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Text(text = "${entry.value} ofertas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                    if (index < statistics.offersByCategory.size - 1) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun DailyOffersChart(dailyData: Map<String, Int>) {
    val sortedData = dailyData.toList().sortedBy { it.first }.takeLast(7)
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(sortedData) {
        val entries = sortedData.mapIndexed { index, (_, count) ->
            entryOf(index, count)
        }
        chartEntryModelProducer.setEntries(entries)
    }

    Card {
        Column(modifier = Modifier.padding(16.dp).height(250.dp)) {
            if (sortedData.isNotEmpty()) {
                ProvideChartStyle {
                    Chart(
                        chart = columnChart(),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}


@Composable
fun UpgradeToProForStatistics(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Estadísticas Avanzadas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analiza el rendimiento de tus ofertas con gráficos detallados y métricas avanzadas.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Con Plan Pro obtienes:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProStatFeature("Gráficos de ofertas por día")
                ProStatFeature("Top 5 productos más ofertados")
                ProStatFeature("Análisis de precios")
                ProStatFeature("Comparativa semanal")
                ProStatFeature("Exportación de reportes")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* TODO: Navigate to upgrade */ }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Upgrade, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar a Plan Pro", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ProStatFeature(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text)
    }
}

