package com.ecomap.usuario.presentation.ui.basket

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.BasketItem
import com.ecomap.usuario.presentation.ui.components.UpgradePromptDialog
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.BasketUiState
import com.ecomap.usuario.presentation.viewmodel.BasketViewModel
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.utils.rememberDebouncedClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketScreen(
    onNavigateBack: () -> Unit,
    basketViewModel: BasketViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {}
) {
    val basketState by basketViewModel.basketState.collectAsState()
    val message by basketViewModel.message.collectAsState()
    val showUpgradeDialog by basketViewModel.showUpgradeDialog.collectAsState()

    // Observar estado de suscripción en tiempo real
    val isProUser by authViewModel.subscriptionMonitor.isProStatus.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            basketViewModel.clearMessage()
        }
    }

    // Dialog de upgrade PRO mejorado
    if (showUpgradeDialog) {
        UpgradePromptDialog(
            feature = "Productos ilimitados en canasta",
            onDismiss = { basketViewModel.dismissUpgradeDialog() },
            onUpgrade = {
                basketViewModel.dismissUpgradeDialog()
                onNavigateToSubscription()
            }
        )
    }

    Scaffold( // <-- SCAFFOLD RESTAURADO
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mi Canasta",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // <-- LLAMAMOS A VOLVER
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, // <-- ÍCONO DE FLECHA
                            contentDescription = "Volver",
                            tint = Color(0xFF1F1F1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White // Usamos color sólido ya que el containerColor del Scaffold es gris
                )
            )
        }
    ) { paddingValues ->
        Box( // <-- APLICACIÓN DEL PADDING
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (basketState) {
                is BasketUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1F1F1F),
                            strokeWidth = 2.dp
                        )
                    }
                }

                is BasketUiState.Error -> {
                    ErrorState(
                        message = (basketState as BasketUiState.Error).message,
                        onRetry = { basketViewModel.loadBasket() }
                    )
                }

                is BasketUiState.Success -> {
                    val items = (basketState as BasketUiState.Success).items

                    if (items.isEmpty()) {
                        EmptyBasketState()
                    } else {
                        BasketContent(
                            items = items,
                            isProUser = isProUser,
                            onQuantityChange = { id, quantity ->
                                basketViewModel.updateQuantity(id, quantity)
                            },
                            onRemove = { id ->
                                basketViewModel.removeItem(id)
                            },
                            onOptimize = {
                                if (isProUser) {
                                    onNavigateToAnalysis()
                                } else {
                                    basketViewModel.showUpgradeDialog()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasketContent(
    items: List<BasketItem>,
    isProUser: Boolean,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onOptimize: () -> Unit
) {
    val totalItems = items.sumOf { it.quantity }
    val totalPrice = items.sumOf { (it.product?.price ?: 0.0) * it.quantity }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Lista de items
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalItems productos",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )

                    if (!isProUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Desbloquear PRO",
                                fontSize = 12.sp,
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            items(items, key = { it.id }) { basketItem ->
                MinimalBasketItemCard(
                    basketItem = basketItem,
                    onQuantityChange = { newQuantity ->
                        onQuantityChange(basketItem.id, newQuantity)
                    },
                    onRemove = {
                        onRemove(basketItem.id)
                    }
                )
            }

            // Spacer final
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom summary mejorado
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen de total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total estimado",
                            fontSize = 13.sp,
                            color = Color(0xFF999999)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$${String.format("%.2f", totalPrice)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                text = "MXN",
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    if (isProUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF9E6))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "PRO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFA000)
                                )
                            }
                        }
                    }
                }

                // Botón de optimización premium
                OptimizeButton(
                    isProUser = isProUser,
                    onClick = onOptimize
                )
            }
        }
    }
}

@Composable
private fun MinimalBasketItemCard(
    basketItem: BasketItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val onDecreaseClick = rememberDebouncedClick {
        if (basketItem.quantity > 1) {
            onQuantityChange(basketItem.quantity - 1)
        } else {
            onRemove()
        }
    }
    val onIncreaseClick = rememberDebouncedClick { onQuantityChange(basketItem.quantity + 1) }
    val product = basketItem.product

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                AsyncImage(
                    model = product?.imageUrl ?: "",
                    imageLoader = ImageConfig.getImageLoader(context),
                    contentDescription = product?.name ?: "Producto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Info y controles
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Nombre y precio
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = product?.name ?: "Producto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF1F1F1F)
                    )

                    Text(
                        text = "$${String.format("%.2f", product?.price ?: 0.0)} c/u",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }

                // Controles de cantidad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón disminuir
                    IconButton(
                        onClick = onDecreaseClick,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (basketItem.quantity > 1)
                                Color(0xFFF5F5F5)
                            else
                                Color(0xFFFFEBEE)
                        )
                    ) {
                        Icon(
                            imageVector = if (basketItem.quantity > 1)
                                Icons.Default.Remove
                            else
                                Icons.Default.Delete,
                            contentDescription = if (basketItem.quantity > 1) "Disminuir" else "Eliminar",
                            modifier = Modifier.size(16.dp),
                            tint = if (basketItem.quantity > 1)
                                Color(0xFF1F1F1F)
                            else
                                Color(0xFFE53935)
                        )
                    }

                    // Cantidad
                    Box(
                        modifier = Modifier
                            .widthIn(min = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${basketItem.quantity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                    }

                    // Botón aumentar
                    IconButton(
                        onClick = onIncreaseClick,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF1F1F1F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Total por item
                    Text(
                        text = "$${String.format("%.2f", (product?.price ?: 0.0) * basketItem.quantity)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizeButton(
    isProUser: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isProUser) 0.dp else 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isProUser) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1F1F1F),
                                Color(0xFF2D2D2D)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA000)
                            )
                        )
                    }
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isProUser)
                                Icons.Default.Calculate
                            else
                                Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isProUser) "Optimizar compra" else "Desbloquear análisis",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isProUser) {
                                "Ver dónde te sale más barato"
                            } else {
                                "Activa PRO para comparar precios"
                            },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyBasketState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color(0xFFCCCCCC)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tu canasta está vacía",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Agrega productos desde los negocios",
            fontSize = 15.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE53935)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            color = Color(0xFF666666),
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F1F1F)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun ProUpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFF9E6),
                                Color(0xFFFFE8CC)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Desbloquea EcoMap PRO",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Con PRO puedes:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F1F1F)
                )

                ProFeatureItem("Productos ilimitados en tu canasta")
                ProFeatureItem("Calcular dónde te sale más barato")
                ProFeatureItem("Comparar negocios con distancias")
                ProFeatureItem("Ver rutas optimizadas")
                ProFeatureItem("Ahorro estimado en transporte")

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF9E6))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Solo $29/mes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000),
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Activar PRO",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Más tarde",
                    color = Color(0xFF666666)
                )
            }
        }
    )
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF1F1F1F),
            lineHeight = 18.sp
        )
    }
}