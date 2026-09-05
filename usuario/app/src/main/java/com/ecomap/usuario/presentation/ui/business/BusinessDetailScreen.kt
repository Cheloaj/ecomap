package com.ecomap.usuario.presentation.ui.business

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.Business
import com.ecomap.usuario.data.model.Offer
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.presentation.viewmodel.BusinessViewModel
import com.ecomap.usuario.presentation.viewmodel.OffersUiState
import com.ecomap.usuario.presentation.viewmodel.ProductsUiState
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.presentation.ui.components.BusinessDetailHeaderSkeleton
import com.ecomap.usuario.presentation.ui.components.ProductCardSkeleton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BusinessDetailScreen(
    business: Business,
    onNavigateBack: () -> Unit,
    onNavigateToBasket: () -> Unit = {},
    onProductClick: ((Product) -> Unit)? = null,
    viewModel: BusinessViewModel = hiltViewModel(),
    userDataViewModel: com.ecomap.usuario.presentation.viewmodel.UserDataViewModel = hiltViewModel(),
    authViewModel: com.ecomap.usuario.presentation.viewmodel.AuthViewModel = hiltViewModel(),
    basketViewModel: com.ecomap.usuario.presentation.viewmodel.BasketViewModel = hiltViewModel()
) {
    val productsState by viewModel.productsState.collectAsState()
    val offersState by viewModel.offersState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // Favoritos
    val isFavoriteMap by userDataViewModel.isFavorite.collectAsState()
    val isFavorite = isFavoriteMap[business.id] ?: false

    // Usuario actual para verificar si es PRO
    val currentUser by authViewModel.currentUser.collectAsState()
    val isProUser = currentUser?.isPro ?: false

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Mensajes de la canasta
    val basketMessage by basketViewModel.message.collectAsState()
    LaunchedEffect(basketMessage) {
        basketMessage?.let {
            // Mostrar Snackbar con texto clickeable
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Producto agregado a ",
                    actionLabel = "Canasta",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigateToBasket()
                }
            }
            basketViewModel.clearMessage()
        }
    }

    LaunchedEffect(business) {
        viewModel.selectBusiness(business)
        // Agregar al historial cuando se abre el detalle
        userDataViewModel.addToHistory(business.id)
        // Verificar si es favorito
        userDataViewModel.checkIfFavorite(business.id)
    }

    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    actionColor = Color(0xFF4CAF50)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        business.businessName,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Favoritos
                    IconButton(onClick = {
                        userDataViewModel.toggleFavorite(business.id)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) com.ecomap.usuario.ui.theme.NuColors.Error else androidx.compose.ui.graphics.Color.Gray
                        )
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Imagen de portada con gradiente
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (!business.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = business.avatarUrl,
                            imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                            contentDescription = "Imagen de ${business.businessName}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradiente overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                    } else {
                        // Placeholder con gradiente
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AppleColors.IOSBlue,
                                            AppleColors.IOSIndigo
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Información principal del negocio
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Nombre y tipo
                        Text(
                            text = business.businessName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = AppleColors.IOSBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = business.businessType,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = AppleColors.IOSBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dirección
                        InfoRow(
                            icon = Icons.Default.Place,
                            text = business.address,
                            color = AppleColors.IOSRed
                        )

                        if (business.phone != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(
                                icon = Icons.Default.Phone,
                                text = business.phone,
                                color = AppleColors.IOSGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estado de verificación
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppleColors.IOSGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Negocio verificado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleColors.IOSGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Tabs para Productos y Ofertas
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AppleColors.IOSBlue
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Productos",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Ofertas",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Contenido de las tabs
            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        0 -> ProductsTabContent(
                            state = productsState,
                            isProUser = isProUser,
                            onProductClick = onProductClick,
                            onAddToBasket = { productId ->
                                basketViewModel.addToBasket(productId)
                            }
                        )
                        1 -> OffersTabContent(
                            state = offersState,
                            business = business,
                            onOfferClick = { offer ->
                                // Convertir oferta a producto y navegar a detalles
                                onProductClick?.invoke(offer.toProduct())
                            },
                            onAddToBasket = { offerId ->
                                basketViewModel.addToBasket(offerId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProductsTabContent(
    state: ProductsUiState,
    isProUser: Boolean,
    onProductClick: ((Product) -> Unit)? = null,
    onAddToBasket: (String) -> Unit = {}
) {
    when (state) {
        is ProductsUiState.Loading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    ProductCardSkeleton()
                }
            }
        }
        is ProductsUiState.Success -> {
            if (state.products.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ShoppingBag,
                    message = "No hay productos disponibles"
                )
            } else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.products.forEach { product ->
                        ProductCard(
                            product = product,
                            isProUser = isProUser,
                            onProductClick = onProductClick,
                            onAddToCart = { prod ->
                                onAddToBasket(prod.id)
                            }
                        )
                    }
                }
            }
        }
        is ProductsUiState.Error -> {
            ErrorState(message = state.message)
        }
    }
}

@Composable
fun OffersTabContent(
    state: OffersUiState,
    business: Business,
    onOfferClick: (Offer) -> Unit = {},
    onAddToBasket: (String) -> Unit = {}
) {
    when (state) {
        is OffersUiState.Loading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    ProductCardSkeleton()
                }
            }
        }
        is OffersUiState.Success -> {
            if (state.offers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.LocalOffer,
                    message = "No hay ofertas disponibles"
                )
            } else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.offers.forEach { offer ->
                        OfferCard(
                            offer = offer,
                            onShowDetails = { onOfferClick(offer) },
                            onAddToBasket = { onAddToBasket(offer.id) }
                        )
                    }
                }
            }
        }
        is OffersUiState.Error -> {
            ErrorState(message = state.message)
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isProUser: Boolean, // false = Gratuito (No ve botón), true = Pro (Ve botón)
    onProductClick: ((Product) -> Unit)? = null,
    onAddToCart: (Product) -> Unit = {}
) {
    // Estado LOCAL para las estadísticas de calificación de ESTE producto específico
    var ratingStats by remember(product.id) { mutableStateOf<com.ecomap.usuario.data.model.ProductRatingStats?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(product.id) {
        try {
            val ratingRepository = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                RatingRepositoryEntryPoint::class.java
            ).ratingRepository()

            ratingRepository.getProductRatingStats(product.id).onSuccess { stats ->
                ratingStats = stats
            }
        } catch (e: Exception) {
            // Si hay error, dejar ratingStats en null
            ratingStats = null
        }
    }

    Card(
        onClick = { onProductClick?.invoke(product) },
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
                // 1. ETIQUETAS (OFERTA O DISPONIBLE - no ambos)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Badge de Oferta (si existe) - PRIORIDAD
                    if (product.isOnOffer && product.offerDescription != null) {
                        Surface(
                            color = Color(0xFFFF3B30),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = product.offerDescription ?: "OFERTA",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Etiqueta Disponible (solo si NO hay oferta)
                    else if (product.isAvailable) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "Disponible",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Espaciador si no hay etiquetas
                    if (!product.isOnOffer && !product.isAvailable) {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // 2. TÍTULO Y DESCRIPCIÓN
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = product.name,
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
                        text = product.description ?: "Sin descripción",
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
                    // Precio (con soporte para ofertas)
                    if (product.isOnOffer && product.originalPrice != null) {
                        // OFERTA ACTIVA - Mostrar precio antes y ahora
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Precio original (tachado)
                            Text(
                                text = "$${product.originalPrice.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                            // Precio con oferta (destacado)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$${product.effectivePrice.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = Color(0xFFFF3B30)
                                )
                                if (product.unit != null) {
                                    Text(
                                        text = "/ ${product.unit}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // PRECIO NORMAL - Sin oferta
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$${product.price.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.Black
                            )
                            if (product.unit != null) {
                                Text(
                                    text = "/ ${product.unit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                )
                            }
                        }
                    }

                    // BOTÓN ROJO: Agregar al carrito (disponible para todos los usuarios)
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clickable { onAddToCart(product) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF3B30)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar",
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
                    .padding(8.dp) // Marco blanco
            ) {
                // Imagen
                AsyncImage(
                    model = product.imageUrl,
                    imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0))
                )

                // --- OVERLAY DE CALIFICACIÓN Y TIEMPO (En una línea horizontal) ---
                val hasRatings = (ratingStats?.totalRatings ?: 0) > 0
                val averageScore = ratingStats?.averageRating ?: 0.0
                val timeAgo = getTimeAgo(product.createdAt)

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Rating con estrella (píldora MUCHO más pequeña)
                    Surface(
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
                                tint = if (hasRatings) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = if (hasRatings) String.format("%.1f", averageScore) else "0.0",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tiempo de publicación (píldora MUCHO más pequeña)
                    Surface(
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
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = timeAgo,
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


        // Ubicación para productos comunitarios (fuera del card)
        if (product.isCommunityProduct && product.latitude != null && product.longitude != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (product.latitude != null && product.longitude != null) {
                            val gmmIntentUri = android.net.Uri.parse("geo:${product.latitude},${product.longitude}?q=${product.latitude},${product.longitude}")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${product.latitude},${product.longitude}"))
                                context.startActivity(browserIntent)
                            }
                        }
                    }
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = com.ecomap.usuario.ui.theme.NuColors.Info,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = product.locationAddress ?: "Ver en mapa",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }


@Composable
fun OfferCard(
    offer: Offer,
    onShowDetails: () -> Unit = {},
    onAddToBasket: () -> Unit = {}
) {
    val context = LocalContext.current

    // Determinar el texto del badge de oferta
    val offerBadgeText = when {
        !offer.title.isNullOrBlank() -> offer.title // "2x1", "3x2", etc.
        offer.discountPercentage != null -> "${offer.discountPercentage}% OFF"
        else -> "OFERTA"
    }

    // Calcular tiempo transcurrido
    val timeAgo = getTimeAgo(offer.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(140.dp)
            .clickable { onShowDetails() },
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
                    // Badge rojo con la oferta
                    Surface(
                        color = Color(0xFFFF3B30),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = offerBadgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // 2. TÍTULO Y DESCRIPCIÓN
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = offer.productName,
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
                        text = offer.description ?: "Oferta especial",
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
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$${offer.discountedPrice.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = Color(0xFFFF3B30)
                                )
                                if (!offer.unit.isNullOrBlank()) {
                                    Text(
                                        text = "/ ${offer.unit}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Precio sin oferta
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$${(offer.discountedPrice ?: offer.price).toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.Black
                            )
                            if (!offer.unit.isNullOrBlank()) {
                                Text(
                                    text = "/ ${offer.unit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                )
                            }
                        }
                    }

                    // BOTÓN ROJO: Agregar a canasta
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clickable { onAddToBasket() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF3B30)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar",
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
                    .padding(8.dp) // Marco blanco
            ) {
                // Imagen
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

                // --- OVERLAY DE RATING Y TIEMPO (En una línea horizontal) ---
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Rating con estrella (píldora pequeña)
                    Surface(
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

                    // Tiempo de publicación (píldora pequeña)
                    Surface(
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
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = timeAgo,
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
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = AppleColors.IOSRed,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppleColors.IOSRed
            )
        }
    }
}

@Composable
fun RatingDialog(
    product: Product,
    ratingsState: com.ecomap.usuario.presentation.viewmodel.RatingUiState,
    onDismiss: () -> Unit,
    onRate: (Int, String?) -> Unit,
    onUpdate: (String, Int, String?) -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var existingRating by remember { mutableStateOf<com.ecomap.usuario.data.model.ProductRating?>(null) }

    // Cargar calificación existente del usuario
    LaunchedEffect(ratingsState) {
        if (ratingsState is com.ecomap.usuario.presentation.viewmodel.RatingUiState.Success) {
            existingRating = ratingsState.userRating
            if (existingRating != null) {
                selectedRating = existingRating!!.rating
                comment = existingRating!!.comment ?: ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingRating != null) "Actualizar Calificación" else "Calificar Producto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre del producto
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selector de estrellas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Toca las estrellas para calificar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(5) { index ->
                            IconButton(
                                onClick = { selectedRating = index + 1 }
                            ) {
                                Icon(
                                    imageVector = if (index < selectedRating)
                                        Icons.Default.Star
                                    else
                                        Icons.Default.StarBorder,
                                    contentDescription = "${index + 1} estrellas",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    if (selectedRating > 0) {
                        Text(
                            text = when (selectedRating) {
                                1 -> "Muy malo"
                                2 -> "Malo"
                                3 -> "Regular"
                                4 -> "Bueno"
                                5 -> "Excelente"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB800)
                        )
                    }
                }

                // Comentario opcional
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 300) comment = it },
                    label = { Text("Comentario (opcional)") },
                    supportingText = { Text("${comment.length}/300 caracteres") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRating > 0) {
                        if (existingRating != null) {
                            onUpdate(existingRating!!.id, selectedRating, comment.ifBlank { null })
                        } else {
                            onRate(selectedRating, comment.ifBlank { null })
                        }
                    }
                },
                enabled = selectedRating > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleColors.IOSRed
                )
            ) {
                Text(if (existingRating != null) "Actualizar" else "Calificar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Función helper para calcular tiempo transcurrido
private fun getTimeAgo(createdAt: String): String {
    if (createdAt.isEmpty()) return "Ahora"

    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = format.parse(createdAt) ?: return "Ahora"

        val now = java.util.Date()
        val diffMillis = now.time - date.time

        when {
            diffMillis < 0 -> "Ahora"
            diffMillis < 60_000 -> "Ahora" // Menos de 1 minuto
            diffMillis < 3_600_000 -> "${(diffMillis / 60_000).toInt()}m" // Menos de 1 hora
            diffMillis < 86_400_000 -> "${(diffMillis / 3_600_000).toInt()}h" // Menos de 1 día
            diffMillis < 604_800_000 -> "${(diffMillis / 86_400_000).toInt()}d" // Menos de 1 semana
            diffMillis < 2_592_000_000 -> "${(diffMillis / 604_800_000).toInt()}sem" // Menos de 1 mes
            diffMillis < 31_536_000_000 -> "${(diffMillis / 2_592_000_000).toInt()}mes" // Menos de 1 año
            else -> "${(diffMillis / 31_536_000_000).toInt()}a" // Años
        }
    } catch (e: Exception) {
        "Ahora"
    }
}

// EntryPoint para acceder al RatingRepository desde Composable
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface RatingRepositoryEntryPoint {
    fun ratingRepository(): com.ecomap.usuario.domain.repository.RatingRepository
}