package com.ecomap.socio.presentation.ui.scheduled

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.data.model.ScheduledOffer
import com.ecomap.socio.data.model.ScheduledOfferStatus
import com.ecomap.socio.data.model.UnitType
import com.ecomap.socio.presentation.viewmodel.ScheduledOfferViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledScreen(
    onNavigateToUpgrade: () -> Unit,
    viewModel: ScheduledOfferViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scheduledOffers by viewModel.scheduledOffers.collectAsState()
    val scheduledOffersState by viewModel.scheduledOffersState.collectAsState()
    val userBusiness by viewModel.userBusiness.collectAsState()

    var selectedBusinessId by remember { mutableStateOf<String?>(null) } // null = "Todos"
    var showBusinessSelector by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadScheduledOffers()
        viewModel.loadUserBusiness()
    }

    // Filtrar ofertas según el negocio seleccionado
    val filteredOffers = remember(scheduledOffers, selectedBusinessId) {
        if (selectedBusinessId == null) {
            scheduledOffers // Mostrar todos
        } else {
            scheduledOffers.filter { it.businessId == selectedBusinessId }
        }
    }

    // Agrupar ofertas por negocio
    val offersByBusiness = remember(filteredOffers) {
        filteredOffers.groupBy { it.businessId }
    }

    Scaffold(
        containerColor = NuColors.Background
    ) { paddingValues ->
        when (val state = scheduledOffersState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NuColors.Primary)
                }
            }

            is UiState.Error -> {
                if (state.message.contains("Plan Pro", ignoreCase = true)) {
                    ModernProLockScreen(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        onUpgrade = onNavigateToUpgrade
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = NuColors.Error
                            )
                            Text(
                                text = state.message,
                                color = NuColors.Error,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                            Button(
                                onClick = { viewModel.loadScheduledOffers() },
                                colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            else -> {
                if (scheduledOffers.isEmpty()) {
                    EmptyScheduledState(modifier = Modifier.fillMaxSize().padding(paddingValues))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Productos Programados",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuColors.TextPrimary
                                )
                                Text(
                                    text = "${filteredOffers.size} ${if (filteredOffers.size == 1) "producto programado" else "productos programados"}",
                                    fontSize = 14.sp,
                                    color = NuColors.TextSecondary
                                )

                                // Filtro de negocio
                                if (userBusiness.size > 1) {
                                    BusinessFilterChip(
                                        businesses = userBusiness,
                                        selectedBusinessId = selectedBusinessId,
                                        onBusinessSelected = { businessId ->
                                            selectedBusinessId = businessId
                                        }
                                    )
                                }
                            }
                        }

                        // Ofertas agrupadas por negocio
                        offersByBusiness.forEach { (businessId, offers) ->
                            item(key = "business_$businessId") {
                                val business = userBusiness.find { it.id == businessId }
                                BusinessScheduledGroup(
                                    business = business,
                                    offers = offers,
                                    onCancelOffer = { viewModel.cancelScheduledOffer(it) },
                                    onDeleteOffer = { viewModel.deleteScheduledOffer(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernProLockScreen(
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NuColors.Background,
                        NuColors.Primary.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de estrella dorada
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.2f),
                                Color(0xFFFFD700).copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFFB300)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Función Exclusiva",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Plan Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFB300),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Programa tus productos con anticipación y publícalos automáticamente en el horario que elijas.",
                fontSize = 16.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tarjeta de beneficios
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Con Plan Pro obtienes:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )

                    ProFeature("Productos ilimitados", Icons.Default.Inventory)
                    ProFeature("Programación automática", Icons.Default.Schedule)
                    ProFeature("Notificaciones semanales", Icons.Default.Notifications)
                    ProFeature("Soporte prioritario", Icons.Default.SupportAgent)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                )
            ) {
                Icon(
                    Icons.Default.Upgrade,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Actualizar a Plan Pro",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ProFeature(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NuColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = NuColors.TextPrimary
        )
    }
}

@Composable
fun EmptyScheduledState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = NuColors.TextSecondary.copy(alpha = 0.3f)
            )
            Text(
                text = "No hay productos programados",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Los productos que programes aparecerán aquí",
                fontSize = 14.sp,
                color = NuColors.TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BusinessScheduledGroup(
    business: Business?,
    offers: List<ScheduledOffer>,
    onCancelOffer: (String) -> Unit,
    onDeleteOffer: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Header del negocio (clickeable para expandir/colapsar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar del negocio
                    if (business?.avatarUrl != null) {
                        AsyncImage(
                            model = business.avatarUrl,
                            imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NuColors.Background)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NuColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = NuColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = business?.businessName ?: "Negocio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )
                        Text(
                            text = "${offers.size} ${if (offers.size == 1) "producto" else "productos"}",
                            fontSize = 13.sp,
                            color = NuColors.TextSecondary
                        )
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = NuColors.TextSecondary
                )
            }

            // Lista de ofertas (expandible)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = NuColors.Background)

                    offers.forEachIndexed { index, offer ->
                        ScheduledOfferItem(
                            offer = offer,
                            onCancel = onCancelOffer,
                            onDelete = onDeleteOffer
                        )

                        if (index < offers.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = NuColors.Background
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledOfferItem(
    offer: ScheduledOffer,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = offer.productName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = NuColors.TextPrimary
            )

            Text(
                text = "$${String.format("%.2f", offer.price)} / ${UnitType.fromString(offer.unit).displayName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NuColors.Primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NuColors.TextSecondary
                    )
                    Text(
                        text = formatScheduledDate(offer.scheduledDate),
                        fontSize = 13.sp,
                        color = NuColors.TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NuColors.TextSecondary
                    )
                    Text(
                        text = offer.scheduledTime,
                        fontSize = 13.sp,
                        color = NuColors.TextSecondary
                    )
                }
            }

            StatusChip(status = offer.status)
        }

        // Acciones
        if (offer.status == ScheduledOfferStatus.PENDING) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onCancel(offer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancelar",
                        tint = NuColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = NuColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (offer.status != ScheduledOfferStatus.PUBLISHED) {
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = NuColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = NuColors.Error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Eliminar Producto Programado",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar \"${offer.productName}\"? Esta acción no se puede deshacer.",
                    textAlign = TextAlign.Center,
                    color = NuColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(offer.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NuColors.Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = NuColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
fun StatusChip(status: ScheduledOfferStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        ScheduledOfferStatus.PENDING -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFFFF8F00),
            "Pendiente"
        )
        ScheduledOfferStatus.PUBLISHED -> Triple(
            Color(0xFFD4EDDA),
            Color(0xFF28A745),
            "Publicado"
        )
        ScheduledOfferStatus.CANCELLED -> Triple(
            Color(0xFFF8D7DA),
            Color(0xFFDC3545),
            "Cancelado"
        )
        ScheduledOfferStatus.FAILED -> Triple(
            Color(0xFFF8D7DA),
            Color(0xFFDC3545),
            "Fallido"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

fun formatScheduledDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        when (date) {
            today -> "Hoy"
            tomorrow -> "Mañana"
            else -> "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
        }
    } catch (e: Exception) {
        dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFilterChip(
    businesses: List<Business>,
    selectedBusinessId: String?,
    onBusinessSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val selectedBusiness = businesses.find { it.id == selectedBusinessId }
    val displayText = selectedBusiness?.businessName ?: "Todos los negocios"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            color = NuColors.Primary.copy(alpha = 0.1f),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        tint = NuColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = displayText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = NuColors.Primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            // Opción "Todos"
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = if (selectedBusinessId == null) NuColors.Primary else NuColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Todos los negocios",
                            fontWeight = if (selectedBusinessId == null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedBusinessId == null) NuColors.Primary else NuColors.TextPrimary
                        )
                    }
                },
                onClick = {
                    onBusinessSelected(null)
                    expanded = false
                },
                leadingIcon = if (selectedBusinessId == null) {
                    {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else null
            )

            HorizontalDivider(color = NuColors.Background)

            // Lista de negocios
            businesses.forEach { business ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (business.avatarUrl != null) {
                                AsyncImage(
                                    model = business.avatarUrl,
                                    imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(NuColors.Background)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(NuColors.Primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = NuColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = business.businessName,
                                fontWeight = if (selectedBusinessId == business.id) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedBusinessId == business.id) NuColors.Primary else NuColors.TextPrimary
                            )
                        }
                    },
                    onClick = {
                        onBusinessSelected(business.id)
                        expanded = false
                    },
                    leadingIcon = if (selectedBusinessId == business.id) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NuColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
