package com.ecomap.socio.presentation.ui.products

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ecomap.socio.data.model.Product
import com.ecomap.socio.data.model.ScheduledOffer
import com.ecomap.socio.domain.repository.RatingRepository
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.presentation.viewmodel.ProductViewModel
import com.ecomap.socio.presentation.viewmodel.ScheduledOfferViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

/**
 * Swipe-to-reveal actions estilo WhatsApp/iOS Mail
 * Los botones se revelan detrás del card con efecto rubber-banding perfecto
 */

// MARK: - HorizontalEdge
enum class HorizontalEdge {
    Leading,
    Trailing
}

// MARK: - BusinessDashboardScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDashboardScreen(
    businessId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    businessViewModel: BusinessViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val businesses by businessViewModel.businesses.collectAsState()
    val business = remember(businesses) { businesses.firstOrNull { it.id == businessId } }
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val pagerState = rememberPagerState(pageCount = { 3 }) // Productos, Programados, Reportes
    val scope = rememberCoroutineScope()

    LaunchedEffect(businessId) {
        productViewModel.publishScheduledProducts(businessId) // ✅ Publicar productos programados
        productViewModel.loadProducts(businessId)
        businessViewModel.checkProStatus() // ✅ Verificar estado Pro
    }

    val products by productViewModel.products.collectAsState()
    val isProUser by businessViewModel.isProUser.collectAsState()
    val productLimit = if (isProUser) Int.MAX_VALUE else 5
    val canAddMore = products.size < productLimit

    // 🐛 Debug: Imprimir estado Pro
    LaunchedEffect(isProUser, products.size) {
        println("🔍 DEBUG BusinessDashboard:")
        println("   - isProUser: $isProUser")
        println("   - products.size: ${products.size}")
        println("   - productLimit: $productLimit")
        println("   - canAddMore: $canAddMore")
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier.shadow(4.dp, spotColor = Color.Black.copy(alpha = 0.1f))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = business?.businessName ?: "Mi Negocio",
                                color = NuColors.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isProUser) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Plan Pro",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onNavigateBack()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = NuColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NuColors.Background
                    )
                )

                // Buscador con BasicTextField

// --- LA CORRECCIÓN: Usar BasicTextField y un Card ---
// Esto nos da control total sobre la altura y el padding
                Card(
                    shape = RoundedCornerShape(23.dp), // Forma de píldora
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp) // 1. Altura fija "angosta" en el Card
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Surface // Color de fondo
                    ),
                    border = BorderStroke( // 2. El borde que querías
                        width = 1.dp,
                        color = if (isFocused) {
                            NuColors.Primary.copy(alpha = 0.5f) // Color de borde enfocado
                        } else {
                            NuColors.TextSecondary.copy(alpha = 0.2f) // Color de borde normal
                        }
                    )
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = NuColors.TextPrimary
                        ),
                        cursorBrush = SolidColor(NuColors.Primary),
                        decorationBox = { innerTextField ->
                            // --- Aquí va la decoración (Iconos y Placeholder) ---
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp), // Padding interno para los iconos
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icono de Lupa
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = NuColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Caja para el texto y el placeholder
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Buscar productos...",
                                            fontSize = 14.sp,
                                            color = NuColors.TextSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField() // Aquí se dibuja el texto que escribe el usuario
                                }

                                // Icono de "X" para borrar (aparece cuando está enfocado)
                                if (isFocused) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        },
                                        modifier = Modifier
                                            .size(24.dp) // Área de toque
                                            .padding(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = NuColors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // Chips de navegación con scroll horizontal
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            label = {
                                Text(
                                    "Productos",
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NuColors.Primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = NuColors.Surface,
                                labelColor = NuColors.TextSecondary,
                                iconColor = NuColors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = pagerState.currentPage == 0,
                                borderColor = if (pagerState.currentPage == 0) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                if (isProUser) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                }
                            },
                            label = {
                                Text(
                                    "Programados",
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isProUser) Icons.Default.Schedule else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            enabled = isProUser,
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NuColors.Primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = NuColors.Surface,
                                labelColor = if (isProUser) NuColors.TextSecondary else NuColors.TextSecondary.copy(alpha = 0.4f),
                                iconColor = if (isProUser) NuColors.TextSecondary else Color(0xFFFFB300),
                                disabledContainerColor = NuColors.Surface.copy(alpha = 0.5f),
                                disabledLabelColor = NuColors.TextSecondary.copy(alpha = 0.4f),
                                disabledLeadingIconColor = Color(0xFFFFB300)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = isProUser,
                                selected = pagerState.currentPage == 1,
                                borderColor = if (pagerState.currentPage == 1) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.3f),
                                disabledBorderColor = NuColors.TextSecondary.copy(alpha = 0.2f)
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = pagerState.currentPage == 2,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                scope.launch { pagerState.animateScrollToPage(2) }
                            },
                            label = {
                                Text(
                                    "Reportes",
                                    fontWeight = if (pagerState.currentPage == 2) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NuColors.Primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = NuColors.Surface,
                                labelColor = NuColors.TextSecondary,
                                iconColor = NuColors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = pagerState.currentPage == 2,
                                borderColor = if (pagerState.currentPage == 2) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = NuColors.Background,
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            if (canAddMore) {
                                onNavigateToAddProduct()
                            } else {
                                showUpgradeDialog = true
                            }
                        },
                        containerColor = NuColors.Primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        ),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar producto",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = false // Bloquear swipe, solo cambiar con botones
        ) { page ->
            when (page) {
                0 -> ProductsTab(
                    products = products,
                    businessId = businessId,
                    onProductClick = onNavigateToProductDetail,
                    onEditProduct = onNavigateToEditProduct,
                    productViewModel = productViewModel,
                    canAddMore = canAddMore,
                    productLimit = productLimit,
                    searchQuery = searchQuery
                )
                1 -> ScheduledProductsTab(
                    businessId = businessId,
                    products = products
                )
                2 -> ReportsTab()
            }
        }
    }

    // Diálogo de upgrade
    if (showUpgradeDialog) {
        UpgradeDialog(
            productLimit = productLimit,
            onUpgrade = {
                showUpgradeDialog = false
                onNavigateToSubscription()
            },
            onDismiss = { showUpgradeDialog = false }
        )
    }
}

// MARK: - ProductsTab
@Composable
private fun ProductsTab(
    products: List<Product>,
    businessId: String,
    onProductClick: (String) -> Unit,
    onEditProduct: (String) -> Unit,
    productViewModel: ProductViewModel,
    canAddMore: Boolean,
    productLimit: Int,
    searchQuery: String = ""
) {
    var openSwipeProductId by remember { mutableStateOf<String?>(null) }
    var isAnyCardDragging by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Filtrar productos: solo publicados (NO programados) y según búsqueda
    val filteredProducts = remember(products, searchQuery) {
        val publishedProducts = products.filter { product ->
            product.publicationStatus == "published"
        }

        if (searchQuery.isBlank()) {
            publishedProducts
        } else {
            publishedProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && openSwipeProductId != null) {
            openSwipeProductId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (filteredProducts.isEmpty()) {
            if (searchQuery.isNotBlank()) {
                // Mostrar mensaje de "no hay resultados"
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NuColors.TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No se encontraron productos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )
                        Text(
                            "Intenta con otro término de búsqueda",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary
                        )
                    }
                }
            } else {
                EmptyProductsState()
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredProducts,
                    key = { it.id }
                ) { product ->
                    ProductRow(
                        product = product,
                        businessId = businessId,
                        onProductClick = onProductClick,
                        onEditProduct = onEditProduct,
                        productViewModel = productViewModel,
                        isSwipeOpen = openSwipeProductId == product.id,
                        hasAnyCardOpen = openSwipeProductId != null,
                        onSwipeStateChange = { isOpen ->
                            openSwipeProductId = if (isOpen) product.id else null
                        },
                        // ▼▼▼ NUEVAS LAMBDAS DE BLOQUEO ▼▼▼
                        requestDragLock = {
                            // Esta función es llamada por el hijo (SwipeActions)
                            // cuando *intenta* empezar a arrastrar.
                            if (isAnyCardDragging) {
                                false // DENIAR: Alguien más ya está arrastrando
                            } else {
                                isAnyCardDragging = true // ACEPTAR: Tú ganas. Bloquea a los demás.
                                // Cerrar otros cards abiertos
                                if (openSwipeProductId != null && openSwipeProductId != product.id) {
                                    openSwipeProductId = null
                                }
                                true // Devuelve "true" para permitir el arrastre
                            }
                        },
                        releaseDragLock = {
                            // El hijo llama a esto cuando termina de arrastrar
                            isAnyCardDragging = false
                        }
                    )
                }
            }
        }
    }
}

// MARK: - ProductRow (con swipeActions estilo WhatsApp)
@Composable
private fun ProductRow(
    product: Product,
    businessId: String,
    onProductClick: (String) -> Unit,
    onEditProduct: (String) -> Unit,
    productViewModel: ProductViewModel,
    isSwipeOpen: Boolean,
    hasAnyCardOpen: Boolean,
    onSwipeStateChange: (Boolean) -> Unit,
    // ▼▼▼ RECIBIR NUEVAS LAMBDAS ▼▼▼
    requestDragLock: () -> Boolean,
    releaseDragLock: () -> Unit
) {
    val view = LocalView.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            productName = product.name,
            onConfirm = {
                productViewModel.deleteProduct(product.id, businessId)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    SwipeActions(
        edge = HorizontalEdge.Trailing,
        isOpen = isSwipeOpen,
        onOpenChange = onSwipeStateChange,
        // ▼▼▼ PASAR LAS LAMBDAS AL HIJO ▼▼▼
        onDragStart = requestDragLock,
        onDragEnd = releaseDragLock,
        content = {
            ProductCard(
                product = product,
                onClick = {
                    if (isSwipeOpen) {
                        // Si ESTE card está abierto, cerrarlo
                        onSwipeStateChange(false)
                    } else if (hasAnyCardOpen) {
                        // Si HAY ALGÚN card abierto (pero no es este), solo cerrar el que está abierto
                        onSwipeStateChange(false)
                    } else {
                        // No hay ningún card abierto, abrir este product
                        onProductClick(product.id)
                    }
                }
            )
        }
    ) {
        SwipeActionButton(
            backgroundColor = Color(0xFF007AFF),
            icon = Icons.Default.Edit,
            label = "Editar",
            onClick = {
                onEditProduct(product.id)
                onSwipeStateChange(false)
            }
        )

        SwipeActionButton(
            backgroundColor = NuColors.Error,
            icon = Icons.Default.Delete,
            label = "Eliminar",
            onClick = {
                showDeleteDialog = true
            }
        )
    }
}

// MARK: - SwipeActions (CORREGIDO: Lógica de bloqueo atómica)
@Composable
fun SwipeActions(
    edge: HorizontalEdge = HorizontalEdge.Trailing,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    // ▼▼▼ RECIBIR LAMBDAS ▼▼▼
    onDragStart: () -> Boolean, // Ahora devuelve un Booleano (permiso)
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val actionWidth = 70.dp
    val actionCount = 2
    val totalActionWidthPx = with(density) { (actionWidth * actionCount).toPx() }
    val maxSwipe = -totalActionWidthPx

    LaunchedEffect(isOpen, isDragging) {
        if (!isDragging) {
            animate(
                initialValue = offsetX,
                targetValue = if (isOpen) maxSwipe else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ -> offsetX = value }
        }
    }

    val draggableState = rememberDraggableState { delta ->
        // Solo aplicar el movimiento si TENEMOS el bloqueo
        if (isDragging) {
            if (edge == HorizontalEdge.Trailing) {
                val newOffset = offsetX + delta
                offsetX = when {
                    newOffset > 0 -> newOffset * 0.2f
                    newOffset < maxSwipe -> maxSwipe + (newOffset - maxSwipe) * 0.25f
                    else -> newOffset
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Background: Botones (FIJOS)
        if (edge == HorizontalEdge.Trailing) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val progress = (abs(offsetX) / totalActionWidthPx).coerceIn(0f, 1f)
                        alpha = progress
                        scaleX = 0.8f + (progress * 0.2f)
                        scaleY = 0.8f + (progress * 0.2f)
                    },
                horizontalArrangement = Arrangement.End
            ) {
                actions()
            }
        }

        // Foreground: Card deslizable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .graphicsLayer {
                    shadowElevation = abs(offsetX / 10f)
                }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = true, // Siempre habilitado, la lógica de abajo lo controla
                    onDragStarted = {
                        // ▼▼▼ LÓGICA DE BLOQUEO ▼▼▼
                        // Pedir permiso al padre ANTES de hacer nada
                        if (onDragStart()) {
                            // PERMISO CONCEDIDO
                            isDragging = true
                        }
                        // Si onDragStart() devuelve false, 'isDragging'
                        // se queda en false, y el draggableState
                        // ignorará todos los 'delta'.
                    },
                    onDragStopped = {
                        // Solo hacer algo si éramos nosotros los que
                        // teníamos el permiso
                        if (isDragging) {
                            isDragging = false
                            onDragEnd() // Liberar el bloqueo para los demás

                            // Lógica de "1 Píxel"
                            if (isOpen) {
                                if (offsetX > maxSwipe) onOpenChange(false)
                                else onOpenChange(true)
                            } else {
                                if (offsetX < 0f) onOpenChange(true)
                                else onOpenChange(false)
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

// MARK: - SwipeActionButton
@Composable
fun RowScope.SwipeActionButton(
    backgroundColor: Color,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(70.dp) // Más angosto: coincide con actionWidth
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// MARK: - ProductCard
@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuColors.Surface)
    ) {
        HorizontalDivider(
            modifier = Modifier.background(NuColors.Surface),
            color = Color.LightGray.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuColors.Surface)
                .clickable {
                    onClick()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(product)
            Spacer(Modifier.width(14.dp))
            ProductInfo(product)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NuColors.TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.background(NuColors.Surface),
            color = Color.LightGray.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

// MARK: - ProductImage
@Composable
private fun ProductImage(product: Product) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = product.imageUrl ?: "https://via.placeholder.com/150",
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(NuColors.Background)
        )
        if (!product.isAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // Badge de oferta en la esquina superior derecha
        if (product.isOnOffer) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        Color(0xFFFF6B6B),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = product.offerDescription ?: "OFERTA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// MARK: - ProductInfo
@Composable
private fun RowScope.ProductInfo(product: Product) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = product.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = NuColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))

        // Precio con manejo de oferta
        if (product.isOnOffer && product.originalPrice != null) {
            // Precio original tachado
            Text(
                text = "$${String.format("%.2f", product.originalPrice)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = NuColors.TextSecondary,
                style = androidx.compose.ui.text.TextStyle(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            )
            Spacer(Modifier.height(2.dp))
            // Precio con descuento
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
                if (product.discountPercentage != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "-${product.discountPercentage}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFFF6B6B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            Text(
                text = "$${String.format("%.2f", product.price)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.Primary
            )
        }

        Spacer(Modifier.height(6.dp))
        // ⭐ Calificación del producto
        ProductRatingStars(productId = product.id)
        Spacer(Modifier.height(8.dp))

        // Badges de estado
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AvailabilityBadge(product.isAvailable)

            // Badge de "Programado" si aplica
            if (product.publicationStatus == "scheduled" && product.scheduledDate != null) {
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF9C27B0).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Programado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

// MARK: - AvailabilityBadge
@Composable
private fun AvailabilityBadge(isAvailable: Boolean) {
    val color = if (isAvailable) NuColors.Success else NuColors.Error
    val backgroundColor = color.copy(alpha = 0.12f)
    val borderColor = color.copy(alpha = 0.3f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = if (isAvailable) "Disponible" else "Agotado",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

// MARK: - EmptyProductsState
@Composable
private fun EmptyProductsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            NuColors.Primary.copy(alpha = 0.1f),
                            NuColors.Primary.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = null,
                tint = NuColors.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No hay productos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Comienza agregando tu primer producto",
            fontSize = 15.sp,
            color = NuColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - LimitReachedCard
@Composable
private fun LimitReachedCard(productLimit: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NuColors.Primary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NuColors.Primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NuColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Límite alcanzado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NuColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tienes $productLimit/$productLimit productos. Actualiza a Pro para productos ilimitados.",
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// MARK: - UpgradeDialog
@Composable
private fun UpgradeDialog(
    productLimit: Int,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Límite alcanzado",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ya tienes $productLimit productos registrados.",
                    fontSize = 15.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Actualiza a Pro para agregar productos ilimitados y desbloquear todas las funciones premium.",
                    fontSize = 15.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                )
            ) {
                Text("Ver planes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = NuColors.TextSecondary)
            }
        }
    )
}

// MARK: - DeleteConfirmationDialog
@Composable
private fun DeleteConfirmationDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NuColors.Surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(NuColors.Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = NuColors.Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "¿Eliminar producto?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    "¿Estás seguro de eliminar \"$productName\"?",
                    fontSize = 15.sp,
                    color = NuColors.TextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Esta acción no se puede deshacer.",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NuColors.Error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Eliminar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancelar", fontWeight = FontWeight.Medium, color = NuColors.TextPrimary)
            }
        }
    )
}

// MARK: - ReportsTab
@Composable
private fun ReportsTab(
    viewModel: com.ecomap.socio.presentation.viewmodel.ReportsViewModel = hiltViewModel()
) {
    val reportsState by viewModel.reportsState.collectAsState()
    var selectedComplaint by remember { mutableStateOf<com.ecomap.socio.data.model.ProductComplaint?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadVendorReports()
    }

    when (val state = reportsState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NuColors.Primary)
            }
        }
        is UiState.Success -> {
            val complaints = state.data

            if (complaints.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(NuColors.Primary.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NuColors.Primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sin reportes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )
                        Text(
                            "No has recibido reportes de clientes",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Lista de reportes
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "⚠️ Las respuestas son monitoreadas por el equipo de EcoMap.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    items(complaints) { complaint ->
                        com.ecomap.socio.presentation.ui.reports.ComplaintCard(
                            complaint = complaint,
                            onClick = { selectedComplaint = complaint }
                        )
                    }
                }
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE53935)
                    )
                    Text(
                        state.message,
                        fontSize = 16.sp,
                        color = NuColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.loadVendorReports() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NuColors.Primary
                        )
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
        else -> {}
    }

    // Dialog de respuesta
    selectedComplaint?.let { complaint ->
        com.ecomap.socio.presentation.ui.reports.ResponseDialog(
            complaint = complaint,
            onDismiss = { selectedComplaint = null },
            onSubmit = { response ->
                viewModel.respondToComplaint(complaint.id, response)
                selectedComplaint = null
            }
        )
    }
}

// MARK: - ScheduledProductsTab
@Composable
private fun ScheduledProductsTab(
    businessId: String,
    products: List<Product>,
    viewModel: ScheduledOfferViewModel = hiltViewModel()
) {
    val scheduledOffers by viewModel.scheduledOffers.collectAsState()
    val scheduledOffersState by viewModel.scheduledOffersState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadScheduledOffers()
    }

    // Filtrar solo las ofertas de este negocio específico
    val businessScheduledOffers = remember(scheduledOffers, businessId) {
        scheduledOffers.filter { it.businessId == businessId }
    }

    // Filtrar productos programados (publicationStatus = "scheduled")
    val scheduledProducts = remember(products) {
        products.filter { it.publicationStatus == "scheduled" }
    }

    val totalScheduledItems = businessScheduledOffers.size + scheduledProducts.size

    when (val state = scheduledOffersState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NuColors.Primary)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                }
            }
        }

        else -> {
            if (totalScheduledItems == 0) {
                EmptyScheduledProductsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "$totalScheduledItems ${if (totalScheduledItems == 1) "producto programado" else "productos programados"}",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Mostrar productos programados (Product con publicationStatus = "scheduled")
                    items(scheduledProducts) { product ->
                        ScheduledProductCardFromProduct(
                            product = product
                        )
                    }

                    // Mostrar ofertas programadas (ScheduledOffer - sistema antiguo)
                    items(businessScheduledOffers) { offer ->
                        ScheduledProductCard(
                            offer = offer,
                            onCancel = { viewModel.cancelScheduledOffer(it) },
                            onDelete = { viewModel.deleteScheduledOffer(it) }
                        )
                    }
                }
            }
        }
    }
}

// MARK: - ScheduledProductCard
@Composable
private fun ScheduledProductCard(
    offer: ScheduledOffer,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )

                    Text(
                        text = "$${String.format("%.2f", offer.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.Primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${offer.scheduledDate} ${offer.scheduledTime}",
                            fontSize = 13.sp,
                            color = NuColors.TextSecondary
                        )
                    }

                    // Información adicional
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NuColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = offer.unit,
                                fontSize = 12.sp,
                                color = NuColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NuColors.TextSecondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = offer.validityType,
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = NuColors.Background)

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NuColors.Error
                    ),
                    border = BorderStroke(1.dp, NuColors.Error.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar", fontSize = 14.sp)
                }

                Button(
                    onClick = { onCancel(offer.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NuColors.TextSecondary.copy(alpha = 0.1f),
                        contentColor = NuColors.TextPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar", fontSize = 14.sp)
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = NuColors.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(NuColors.Error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = NuColors.Error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "¿Eliminar programación?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "¿Estás seguro de eliminar la programación de \"${offer.productName}\"?",
                    fontSize = 15.sp,
                    color = NuColors.TextPrimary,
                    lineHeight = 22.sp
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
                    Text("Eliminar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Medium, color = NuColors.TextPrimary)
                }
            }
        )
    }
}

// MARK: - ScheduledProductCardFromProduct
@Composable
private fun ScheduledProductCardFromProduct(
    product: Product
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )

                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.Primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = product.scheduledDate ?: "Sin fecha",
                            fontSize = 13.sp,
                            color = NuColors.TextSecondary
                        )
                    }

                    // Información adicional
                    if (product.unit != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NuColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = product.unit,
                                fontSize = 12.sp,
                                color = NuColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - EmptyScheduledProductsState
@Composable
private fun EmptyScheduledProductsState() {
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
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NuColors.Primary.copy(alpha = 0.1f),
                                NuColors.Primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NuColors.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = "No hay productos programados",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = NuColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Los productos que programes para este negocio aparecerán aquí",
                fontSize = 14.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// MARK: - ProductRatingStars
@Composable
private fun ProductRatingStars(
    productId: String,
    ratingRepository: RatingRepository = hiltViewModel<RatingViewModel>().ratingRepository
) {
    var averageRating by remember { mutableStateOf(0.0) }
    var totalRatings by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        isLoading = true

        // Obtener calificación promedio
        val avgResult = ratingRepository.getAverageRating(productId)
        averageRating = avgResult.getOrNull() ?: 0.0

        // Obtener total de calificaciones
        val countResult = ratingRepository.getTotalRatings(productId)
        totalRatings = countResult.getOrNull() ?: 0

        isLoading = false
    }

    if (isLoading) {
        // Mientras carga, mostrar placeholder
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Icon(
                    Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = NuColors.TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else if (totalRatings > 0) {
        // Mostrar estrellas con calificación
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Estrellas
            repeat(5) { index ->
                val starValue = index + 1
                Icon(
                    imageVector = when {
                        averageRating >= starValue -> Icons.Default.Star
                        averageRating >= starValue - 0.5 -> Icons.Default.StarHalf
                        else -> Icons.Default.StarBorder
                    },
                    contentDescription = null,
                    tint = if (averageRating >= starValue - 0.5) {
                        Color(0xFFFFB800) // Color dorado para estrellas
                    } else {
                        NuColors.TextSecondary.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Texto con promedio y cantidad
            Text(
                text = "${String.format("%.1f", averageRating)} ($totalRatings)",
                fontSize = 13.sp,
                color = NuColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        // Sin calificaciones
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Icon(
                    Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = NuColors.TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Sin calificaciones",
                fontSize = 13.sp,
                color = NuColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

// MARK: - RatingViewModel (Helper)
@HiltViewModel
class RatingViewModel @Inject constructor(
    val ratingRepository: RatingRepository
) : ViewModel()

