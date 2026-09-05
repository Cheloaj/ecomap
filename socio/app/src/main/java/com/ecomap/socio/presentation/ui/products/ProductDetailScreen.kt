package com.ecomap.socio.presentation.ui.products

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.socio.presentation.viewmodel.ProductViewModel
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

/**
 * ProductDetailScreen - Detalles completos del producto
 * Incluye Switch para marcar como Disponible/Agotado
 * Incluye sección de reseñas con respuestas del vendedor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    businessId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
    ratingViewModel: com.ecomap.socio.presentation.viewmodel.RatingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val product by viewModel.selectedProduct.collectAsState()
    val toggleAvailabilityState by viewModel.toggleAvailabilityState.collectAsState()

    // Rating states
    val productRatings by ratingViewModel.productRatings.collectAsState()
    val ratingsState by ratingViewModel.ratingsState.collectAsState()
    val averageRating by ratingViewModel.averageRating.collectAsState()
    val totalRatings by ratingViewModel.totalRatings.collectAsState()

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProductById(productId)
        ratingViewModel.loadProductRatings(productId)
        ratingViewModel.loadRatingStats(productId)
        delay(100)
        showContent = true
    }

    // Manejar cambios de disponibilidad
    LaunchedEffect(toggleAvailabilityState) {
        when (toggleAvailabilityState) {
            is UiState.Success -> {
                // Mostrar feedback de éxito
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                viewModel.resetToggleAvailabilityState()
            }
            is UiState.Error -> {
                // Mostrar error
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                viewModel.resetToggleAvailabilityState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalles del Producto",
                        color = NuColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
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
        if (product == null) {
            // Loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NuColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Imagen del producto
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + expandVertically()
                ) {
                    AsyncImage(
                        model = product?.imageUrl ?: "https://via.placeholder.com/400",
                        imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                        contentDescription = product?.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(NuColors.Surface),
                        contentScale = ContentScale.Crop
                    )
                }

                // Contenido
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Nombre
                        Text(
                            text = product?.name ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary,
                            lineHeight = 34.sp
                        )

                        // Precio
                        Text(
                            text = "$${String.format("%.2f", product?.price ?: 0.0)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.Primary
                        )

                        // Descripción (si existe)
                        product?.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Column {
                                    Text(
                                        text = "Descripción",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 14.sp,
                                        color = NuColors.TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // Divider
                        HorizontalDivider(color = NuColors.Surface)

                        // Switch de Disponibilidad
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (product?.isAvailable == true)
                                    NuColors.Success.copy(alpha = 0.1f)
                                else
                                    NuColors.Error.copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (product?.isAvailable == true) "Disponible" else "Agotado",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product?.isAvailable == true)
                                            NuColors.Success
                                        else
                                            NuColors.Error
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (product?.isAvailable == true)
                                            "Los clientes pueden ver este producto"
                                        else
                                            "Este producto está oculto para los clientes",
                                        fontSize = 13.sp,
                                        color = NuColors.TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Switch
                                Switch(
                                    checked = product?.isAvailable ?: false,
                                    onCheckedChange = { isAvailable ->
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        product?.let { p ->
                                            viewModel.toggleProductAvailability(
                                                productId = p.id,
                                                isAvailable = isAvailable,
                                                businessId = businessId
                                            )
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NuColors.Success,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = NuColors.Error
                                    ),
                                    enabled = toggleAvailabilityState !is UiState.Loading
                                )
                            }
                        }

                        // Información adicional
                        if (product?.category != null || product?.stock != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Información Adicional",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuColors.TextPrimary
                                )

                                product?.category?.let { category ->
                                    InfoRow(label = "Categoría", value = category)
                                }

                                product?.stock?.let { stock ->
                                    InfoRow(label = "Stock", value = "$stock unidades")
                                }
                            }
                        }

                        // Información de Oferta
                        product?.let { prod ->
                            if (prod.isOnOffer) {
                                HorizontalDivider(color = NuColors.Surface)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = "Oferta",
                                            tint = Color(0xFFFF6B6B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Oferta Activa",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NuColors.TextPrimary
                                        )
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            prod.offerType?.let { offerType ->
                                                InfoRow(label = "Tipo de oferta", value = offerType)
                                            }

                                            prod.discountPercentage?.let { discount ->
                                                InfoRow(label = "Descuento", value = "$discount%")
                                            }

                                            if (prod.originalPrice != null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Precio original",
                                                        fontSize = 14.sp,
                                                        color = NuColors.TextSecondary
                                                    )
                                                    Text(
                                                        text = "$${String.format("%.2f", prod.originalPrice)}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = NuColors.TextSecondary,
                                                        style = androidx.compose.ui.text.TextStyle(
                                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                        )
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Precio con oferta",
                                                        fontSize = 14.sp,
                                                        color = NuColors.TextSecondary
                                                    )
                                                    Text(
                                                        text = "$${String.format("%.2f", prod.price)}",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFF6B6B)
                                                    )
                                                }
                                            }

                                            prod.offerValidUntil?.let { validUntilStr ->
                                                val formattedDate = try {
                                                    // Parse ISO date string and format to dd/MM/yyyy
                                                    val parsedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                        .parse(validUntilStr)
                                                    parsedDate?.let {
                                                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
                                                    } ?: validUntilStr
                                                } catch (e: Exception) {
                                                    validUntilStr
                                                }

                                                InfoRow(
                                                    label = "Válida hasta",
                                                    value = formattedDate
                                                )
                                            }

                                            prod.offerDescription?.let { description ->
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Descripción",
                                                        fontSize = 14.sp,
                                                        color = NuColors.TextSecondary
                                                    )
                                                    Text(
                                                        text = description,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFFF6B6B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Información de Programación
                            if (prod.publicationStatus == "scheduled" && prod.scheduledDate != null) {
                                HorizontalDivider(color = NuColors.Surface)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Programado",
                                            tint = Color(0xFF9C27B0),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Publicación Programada",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NuColors.TextPrimary
                                        )
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Parse scheduledDate string to display formatted
                                            val scheduledDateParsed = try {
                                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    .parse(prod.scheduledDate)
                                            } catch (e: Exception) {
                                                null
                                            }

                                            scheduledDateParsed?.let { date ->
                                                InfoRow(
                                                    label = "Fecha programada",
                                                    value = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                                        .format(date)
                                                )
                                            } ?: InfoRow(
                                                label = "Fecha programada",
                                                value = prod.scheduledDate
                                            )

                                            val daysUntilPublication = scheduledDateParsed?.let { date ->
                                                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                                                    date.time - System.currentTimeMillis()
                                                )
                                            } ?: 0L

                                            if (daysUntilPublication > 0) {
                                                InfoRow(
                                                    label = "Se publicará en",
                                                    value = "$daysUntilPublication días"
                                                )
                                            } else if (daysUntilPublication == 0L) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Estado",
                                                        fontSize = 14.sp,
                                                        color = NuColors.TextSecondary
                                                    )
                                                    Text(
                                                        text = "Se publicará hoy",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF9C27B0)
                                                    )
                                                }
                                            }

                                            // Mostrar si hay oferta asociada
                                            if (prod.isOnOffer) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = Color(0xFF9C27B0).copy(alpha = 0.3f)
                                                )

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = Color(0xFF9C27B0),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "Oferta programada",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF9C27B0)
                                                        )
                                                    }
                                                    Text(
                                                        text = "La oferta iniciará automáticamente cuando se publique el producto",
                                                        fontSize = 12.sp,
                                                        color = NuColors.TextSecondary,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reseñas de Clientes
                        HorizontalDivider(color = NuColors.Surface)

                        ProductReviewsSection(
                            productId = productId,
                            ratings = productRatings,
                            ratingsState = ratingsState,
                            averageRating = averageRating,
                            totalRatings = totalRatings,
                            ratingViewModel = ratingViewModel,
                            onHapticFeedback = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = NuColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NuColors.TextPrimary
        )
    }
}

@Composable
private fun ProductReviewsSection(
    productId: String,
    ratings: List<com.ecomap.socio.data.model.ProductRating>,
    ratingsState: UiState<List<com.ecomap.socio.data.model.ProductRating>>,
    averageRating: Double,
    totalRatings: Int,
    ratingViewModel: com.ecomap.socio.presentation.viewmodel.RatingViewModel,
    onHapticFeedback: () -> Unit
) {
    var showResponseDialog by remember { mutableStateOf(false) }
    var selectedRatingId by remember { mutableStateOf<String?>(null) }
    var existingResponse by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reseñas de Clientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )

            if (totalRatings > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = String.format("%.1f", averageRating),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )
                    Text(
                        text = "($totalRatings)",
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary
                    )
                }
            }
        }

        // Content
        when (ratingsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NuColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is UiState.Success -> {
                if (ratings.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NuColors.Surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = NuColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sin reseñas aún",
                                fontSize = 14.sp,
                                color = NuColors.TextSecondary
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ratings.forEach { rating ->
                            ReviewCard(
                                rating = rating,
                                onRespondClick = {
                                    selectedRatingId = rating.id
                                    existingResponse = rating.vendorResponse
                                    showResponseDialog = true
                                    onHapticFeedback()
                                }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NuColors.Error.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "Error al cargar reseñas",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        color = NuColors.Error
                    )
                }
            }
            else -> {}
        }
    }

    // Response Dialog
    if (showResponseDialog && selectedRatingId != null) {
        VendorResponseDialog(
            ratingId = selectedRatingId!!,
            productId = productId,
            existingResponse = existingResponse,
            ratingViewModel = ratingViewModel,
            onDismiss = {
                showResponseDialog = false
                selectedRatingId = null
                existingResponse = null
            },
            onHapticFeedback = onHapticFeedback
        )
    }
}

@Composable
private fun ReviewCard(
    rating: com.ecomap.socio.data.model.ProductRating,
    onRespondClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NuColors.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User info and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rating.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < rating.rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (index < rating.rating) Color(0xFFFFC107) else NuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Comment
            rating.comment?.let { comment ->
                if (comment.isNotBlank()) {
                    Text(
                        text = comment,
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // Vendor response (if exists)
            if (rating.hasVendorResponse) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = NuColors.Background
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NuColors.Primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Respuesta del vendedor:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.Primary
                    )
                    Text(
                        text = rating.vendorResponse ?: "",
                        fontSize = 13.sp,
                        color = NuColors.TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Respond button
            Button(
                onClick = onRespondClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rating.hasVendorResponse)
                        NuColors.Surface.copy(alpha = 0.5f)
                    else
                        NuColors.Primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (rating.hasVendorResponse) "Editar respuesta" else "Responder",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorResponseDialog(
    ratingId: String,
    productId: String,
    existingResponse: String?,
    ratingViewModel: com.ecomap.socio.presentation.viewmodel.RatingViewModel,
    onDismiss: () -> Unit,
    onHapticFeedback: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var customResponse by remember { mutableStateOf(existingResponse ?: "") }
    val vendorResponseState by ratingViewModel.vendorResponseState.collectAsState()

    // Handle success
    LaunchedEffect(vendorResponseState) {
        if (vendorResponseState is UiState.Success) {
            onHapticFeedback()
            ratingViewModel.resetVendorResponseState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingResponse != null) "Editar Respuesta" else "Responder Reseña",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick templates
                Text(
                    text = "Respuestas rápidas:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.ecomap.socio.utils.VendorResponseTemplates.templates.forEach { template ->
                        OutlinedButton(
                            onClick = {
                                selectedTemplate = template.id
                                customResponse = template.message
                                onHapticFeedback()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTemplate == template.id)
                                    NuColors.Primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent
                            )
                        ) {
                            Text(
                                text = template.label,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom response
                Text(
                    text = "O escribe tu propia respuesta:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )

                OutlinedTextField(
                    value = customResponse,
                    onValueChange = {
                        customResponse = it
                        selectedTemplate = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe aquí...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp)
                )

                if (vendorResponseState is UiState.Error) {
                    Text(
                        text = (vendorResponseState as UiState.Error).message,
                        fontSize = 12.sp,
                        color = NuColors.Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onHapticFeedback()
                    if (existingResponse != null) {
                        ratingViewModel.updateVendorResponse(ratingId, customResponse, productId)
                    } else {
                        ratingViewModel.addVendorResponse(ratingId, customResponse, productId)
                    }
                },
                enabled = customResponse.isNotBlank() && vendorResponseState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary)
            ) {
                if (vendorResponseState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (existingResponse != null) "Actualizar" else "Enviar")
                }
            }
        },
        dismissButton = {
            if (existingResponse != null) {
                TextButton(
                    onClick = {
                        onHapticFeedback()
                        ratingViewModel.deleteVendorResponse(ratingId, productId)
                    },
                    enabled = vendorResponseState !is UiState.Loading
                ) {
                    Text("Eliminar", color = NuColors.Error)
                }
            }
            TextButton(
                onClick = {
                    onHapticFeedback()
                    onDismiss()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}
