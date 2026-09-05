package com.ecomap.usuario.presentation.ui.offers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Offer
import com.ecomap.usuario.presentation.viewmodel.BusinessUiState
import com.ecomap.usuario.presentation.viewmodel.BusinessViewModel
import com.ecomap.usuario.presentation.viewmodel.OffersUiState
import com.ecomap.usuario.ui.theme.AppleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onNavigateBack: () -> Unit,
    onOfferClick: (Business) -> Unit,
    viewModel: BusinessViewModel = hiltViewModel()
) {
    val offersState by viewModel.offersState.collectAsState()
    val businessState by viewModel.businessState.collectAsState()
    var selectedCategory by remember { mutableStateOf("Todas") }

    val categories = listOf("Todas", "Descuentos", "2x1", "Envío gratis", "Limitadas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ofertas Destacadas") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Categorías
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedCategory == category)
                                        AppleColors.IOSBlue.copy(alpha = 0.15f)
                                    else
                                        Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == category)
                                    AppleColors.IOSBlue
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Contenido
            when (val state = offersState) {
                is OffersUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is OffersUiState.Success -> {
                    if (state.offers.isEmpty()) {
                        EmptyOffersState()
                    } else {
                        // Obtener el mapa de negocios para buscar información
                        val businessMap = if (businessState is BusinessUiState.Success) {
                            (businessState as BusinessUiState.Success).businesses.associateBy { it.id }
                        } else {
                            emptyMap()
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.offers) { offer ->
                                // Buscar el negocio correspondiente
                                businessMap[offer.businessId]?.let { business ->
                                    OfferCard(
                                        business = business,
                                        offer = offer,
                                        onClick = { onOfferClick(business) }
                                    )
                                }
                            }
                        }
                    }
                }
                is OffersUiState.Error -> {
                    ErrorOffersState(message = state.message)
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    business: Business,
    offer: Offer,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // --- COLUMNA IZQUIERDA (Información) ---
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 0.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ETIQUETA DE OFERTA (TOP-LEFT)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Badge de Oferta con descuento
                    if (offer.discountPercentage != null) {
                        Surface(
                            color = Color(0xFFFF3B30),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${offer.discountPercentage}% OFF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 2. TÍTULO Y DESCRIPCIÓN
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = offer.title ?: offer.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = offer.description ?: business.businessName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                // 3. PRECIO Y BOTÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Precios (con oferta)
                    if (offer.originalPrice != null && offer.discountedPrice != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Precio original (tachado)
                            Text(
                                text = "$${offer.originalPrice.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                            // Precio con descuento (destacado)
                            Text(
                                text = "$${offer.discountedPrice.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFFFF3B30)
                            )
                        }
                    } else {
                        Text(
                            text = "$${offer.discountedPrice?.toInt() ?: 0}",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color(0xFFFF3B30)
                        )
                    }

                    // BOTÓN ROJO: Ver oferta
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF3B30)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Ver oferta",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // --- COLUMNA DERECHA (Imagen) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                if (!offer.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = offer.imageUrl,
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0F0F0))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AppleColors.IOSOrange.copy(alpha = 0.6f),
                                        AppleColors.IOSPink.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Rating overlay (top-right de la imagen)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "4.5",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOffersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(AppleColors.IOSOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppleColors.IOSOrange
                )
            }
            Text(
                text = "No hay ofertas disponibles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vuelve pronto para descubrir nuevas ofertas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorOffersState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppleColors.IOSRed.copy(alpha = 0.1f)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = AppleColors.IOSRed,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Error al cargar ofertas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
