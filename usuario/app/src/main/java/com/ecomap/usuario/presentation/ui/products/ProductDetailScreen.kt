package com.ecomap.usuario.presentation.ui.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.data.model.ProductRating
import com.ecomap.usuario.presentation.viewmodel.RatingUiState
import com.ecomap.usuario.presentation.viewmodel.RatingViewModel
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.utils.rememberSingleExecution
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    businessName: String?,
    businessAddress: String?,
    businessLatitude: Double?,
    businessLongitude: Double?,
    onNavigateBack: () -> Unit,
    onNavigateToAllReviews: (String) -> Unit,
    ratingViewModel: RatingViewModel = hiltViewModel()
) {
    val ratingsState by ratingViewModel.ratingsState.collectAsState()
    var showRatingForm by remember { mutableStateOf(false) }
    var showReportBottomSheet by remember { mutableStateOf(false) }

    // 🛡️ FASE 1: Protección contra clics rápidos (500ms cooldown)
    val executeSafely = rememberSingleExecution()

    LaunchedEffect(product.id) {
        ratingViewModel.loadProductRatings(product.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NuColors.Surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = ratingsState) {
            is RatingUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NuColors.Primary)
                }
            }

            is RatingUiState.Error -> {
                // Mostrar el producto sin reseñas
                ProductDetailContent(
                    product = product,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    businessLatitude = businessLatitude,
                    businessLongitude = businessLongitude,
                    ratings = emptyList(),
                    userRating = null,
                    showRatingForm = showRatingForm,
                    onShowRatingForm = { showRatingForm = it },
                    onShowReportBottomSheet = { showReportBottomSheet = true },
                    onNavigateToAllReviews = onNavigateToAllReviews,
                    ratingViewModel = ratingViewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is RatingUiState.Success -> {
                ProductDetailContent(
                    product = product,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    businessLatitude = businessLatitude,
                    businessLongitude = businessLongitude,
                    ratings = state.ratings,
                    userRating = state.userRating,
                    showRatingForm = showRatingForm,
                    onShowRatingForm = { showRatingForm = it },
                    onShowReportBottomSheet = { showReportBottomSheet = true },
                    onNavigateToAllReviews = onNavigateToAllReviews,
                    ratingViewModel = ratingViewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Bottom sheet para reportar producto
    if (showReportBottomSheet) {
        ReportProductBottomSheet(
            product = product,
            onDismiss = { showReportBottomSheet = false }
        )
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    businessName: String?,
    businessAddress: String?,
    businessLatitude: Double?,
    businessLongitude: Double?,
    ratings: List<ProductRating>,
    userRating: ProductRating?,
    showRatingForm: Boolean,
    onShowRatingForm: (Boolean) -> Unit,
    onShowReportBottomSheet: () -> Unit,
    onNavigateToAllReviews: (String) -> Unit,
    ratingViewModel: RatingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val averageRating = if (ratings.isNotEmpty()) {
        ratings.map { it.rating }.average()
    } else 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Imagen del producto
        item {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(NuColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = NuColors.TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Información del producto
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nombre
                Text(
                    text = product.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary
                )

                // Precio
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.Primary
                )

                // Calificación promedio
                RatingStarsDisplay(
                    rating = averageRating,
                    totalRatings = ratings.size
                )

                // Descripción
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = "Descripción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )
                    Text(
                        text = product.description,
                        fontSize = 15.sp,
                        color = NuColors.TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Ubicación del negocio
        if (businessName != null || businessAddress != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📍 Ubicación del negocio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = NuColors.Surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Ícono de ubicación
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NuColors.Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = NuColors.Primary
                                )
                            }

                            // Información del negocio
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (businessName != null) {
                                    Text(
                                        text = businessName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuColors.TextPrimary
                                    )
                                }
                                if (businessAddress != null) {
                                    Text(
                                        text = businessAddress,
                                        fontSize = 14.sp,
                                        color = NuColors.TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                }

                                // Botón para abrir en Google Maps
                                if (businessLatitude != null && businessLongitude != null) {
                                    val context = LocalContext.current
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                val gmmIntentUri = android.net.Uri.parse("geo:$businessLatitude,$businessLongitude?q=$businessLatitude,$businessLongitude")
                                                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                try {
                                                    context.startActivity(mapIntent)
                                                } catch (e: android.content.ActivityNotFoundException) {
                                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$businessLatitude,$businessLongitude"))
                                                    context.startActivity(browserIntent)
                                                }
                                            },
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = "Ver en mapa",
                                            tint = NuColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Ver en mapa",
                                            fontSize = 13.sp,
                                            color = NuColors.Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Formulario de reseña
        item {
            ReviewFormSection(
                product = product,
                userRating = userRating,
                showForm = showRatingForm,
                onShowForm = onShowRatingForm,
                onShowReportBottomSheet = onShowReportBottomSheet,
                ratingViewModel = ratingViewModel,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Sección de reseñas
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 Reseñas (${ratings.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )

                    if (ratings.size > 3) {
                        TextButton(
                            onClick = { onNavigateToAllReviews(product.id) }
                        ) {
                            Text(
                                text = "Ver todas (${ratings.size})",
                                color = NuColors.Primary
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = NuColors.Primary
                            )
                        }
                    }
                }
            }
        }

        // Mostrar primeras 3 reseñas
        items(ratings.take(3)) { rating ->
            ReviewCard(
                rating = rating,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Mensaje si no hay reseñas
        if (ratings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = NuColors.TextSecondary.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Sin reseñas aún",
                            fontSize = 16.sp,
                            color = NuColors.TextSecondary
                        )
                        Text(
                            text = "Sé el primero en calificar",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewFormSection(
    product: Product,
    userRating: ProductRating?,
    showForm: Boolean,
    onShowForm: (Boolean) -> Unit,
    onShowReportBottomSheet: () -> Unit,
    ratingViewModel: RatingViewModel,
    modifier: Modifier = Modifier
) {
    // 🛡️ FASE 1: Protección contra clics rápidos (500ms cooldown)
    val executeSafely = rememberSingleExecution()

    var selectedRating by remember { mutableIntStateOf(userRating?.rating ?: 0) }
    var comment by remember { mutableStateOf(userRating?.comment ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showEditForm by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // 📸 Launcher para selector nativo del sistema (incluye cámara y galería)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (userRating == null) "⭐ Deja tu reseña" else "⭐ Tu reseña",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = NuColors.TextPrimary
        )

        // Si no hay reseña del usuario, mostrar botón para calificar
        if (!showForm && userRating == null) {
            Button(
                onClick = { onShowForm(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Calificar producto")
            }
        }

        // Botón para reportar producto (siempre visible)
        OutlinedButton(
            onClick = { onShowReportBottomSheet() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NuColors.Primary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NuColors.Primary
            )
        ) {
            Icon(Icons.Default.Flag, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reportar problema")
        }

        // Si hay reseña publicada y no está en modo edición, mostrar el comentario con botón Editar
        if (userRating != null && !showEditForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NuColors.Surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NuColors.Primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header con estrellas y botón Editar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(userRating.rating) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFFB800)
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                showEditForm = true
                                selectedRating = userRating.rating
                                comment = userRating.comment ?: ""
                            }
                        ) {
                            Text(
                                text = "Editar",
                                color = NuColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Comentario publicado
                    if (!userRating.comment.isNullOrBlank()) {
                        Text(
                            text = userRating.comment,
                            fontSize = 15.sp,
                            color = NuColors.TextSecondary,
                            lineHeight = 22.sp
                        )
                    }

                    // Fecha de publicación
                    Text(
                        text = "Publicado el ${formatDate(userRating.createdAt)}",
                        fontSize = 12.sp,
                        color = NuColors.TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Formulario de edición o creación
        if ((showForm && userRating == null) || showEditForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NuColors.Surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NuColors.TextSecondary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selector de estrellas
                    RatingStarsSelector(
                        rating = selectedRating,
                        onRatingChange = { selectedRating = it }
                    )

                    // Campo de comentario con límite de caracteres
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 300) comment = it },
                        label = { Text("Comentario (opcional)") },
                        supportingText = {
                            Text(
                                text = "${comment.length}/300 caracteres",
                                fontSize = 12.sp,
                                color = if (comment.length >= 300) NuColors.Error else NuColors.TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NuColors.Primary,
                            unfocusedBorderColor = NuColors.TextSecondary.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 📸 Selector de imagen (usa selector nativo del sistema)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NuColors.Primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Agregar foto",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri != null) "Cambiar foto" else "Agregar foto (opcional)")
                        }

                        // Preview de la imagen seleccionada
                        selectedImageUri?.let { uri ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NuColors.Surface)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Botón para quitar imagen
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar imagen",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mensaje de feedback
                    if (message != null) {
                        Text(
                            text = message!!,
                            color = if (message!!.contains("exitosa")) NuColors.Success else NuColors.Error,
                            fontSize = 14.sp
                        )
                    }

                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showEditForm) {
                            // Botón Eliminar cuando está editando
                            OutlinedButton(
                                onClick = {
                                    isSubmitting = true
                                    ratingViewModel.deleteRating(
                                        ratingId = userRating!!.id,
                                        productId = product.id
                                    ) { success, msg ->
                                        isSubmitting = false
                                        message = msg
                                        if (success) {
                                            showEditForm = false
                                            onShowForm(false)
                                            comment = ""
                                            selectedRating = 0
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NuColors.Error
                                ),
                                border = BorderStroke(1.dp, NuColors.Error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Eliminar")
                            }
                        } else {
                            // Botón Cancelar cuando está creando nueva reseña
                            OutlinedButton(
                                onClick = {
                                    onShowForm(false)
                                    comment = ""
                                    selectedRating = 0
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }

                        Button(
                            onClick = {
                                // 🛡️ FASE 1: Protección contra doble clic (evita reviews duplicadas)
                                executeSafely {
                                    if (selectedRating == 0) {
                                        message = "Por favor selecciona una calificación"
                                        return@executeSafely
                                    }

                                    isSubmitting = true
                                    if (showEditForm && userRating != null) {
                                        // Actualizar reseña existente
                                        ratingViewModel.updateRating(
                                            ratingId = userRating.id,
                                            productId = product.id,
                                            newRating = selectedRating,
                                            comment = comment.ifBlank { null },
                                            imageUri = selectedImageUri
                                        ) { success, msg ->
                                            isSubmitting = false
                                            message = msg
                                            if (success) {
                                                showEditForm = false
                                                selectedImageUri = null
                                            }
                                        }
                                    } else {
                                        // Crear nueva reseña
                                        ratingViewModel.rateProduct(
                                            productId = product.id,
                                            rating = selectedRating,
                                            comment = comment.ifBlank { null },
                                            imageUri = selectedImageUri
                                        ) { success, msg ->
                                            isSubmitting = false
                                            message = msg
                                            if (success) {
                                                onShowForm(false)
                                                selectedImageUri = null
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NuColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (showEditForm) "Actualizar" else "Publicar")
                            }
                        }
                    }

                    // Botón Cancelar edición
                    if (showEditForm) {
                        OutlinedButton(
                            onClick = {
                                showEditForm = false
                                message = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar edición")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    rating: ProductRating,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NuColors.Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rating.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(rating.rating) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB800)
                        )
                    }
                }
            }

            if (!rating.comment.isNullOrBlank()) {
                Text(
                    text = rating.comment,
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // 📸 Imagen de la reseña
            if (!rating.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = rating.imageUrl,
                    imageLoader = ImageConfig.getImageLoader(context),
                    contentDescription = "Imagen de la reseña",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = formatDate(rating.createdAt),
                fontSize = 12.sp,
                color = NuColors.TextSecondary.copy(alpha = 0.6f)
            )

            // Respuesta del vendedor
            if (rating.hasVendorResponse) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = NuColors.Background
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Primary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = NuColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Respuesta del vendedor",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NuColors.Primary
                            )
                        }
                        Text(
                            text = rating.vendorResponse ?: "",
                            fontSize = 14.sp,
                            color = NuColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingStarsDisplay(
    rating: Double,
    totalRatings: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starValue = index + 1
            Icon(
                imageVector = when {
                    rating >= starValue -> Icons.Default.Star
                    rating >= starValue - 0.5 -> Icons.Default.StarHalf
                    else -> Icons.Default.StarBorder
                },
                contentDescription = null,
                tint = if (rating >= starValue - 0.5) {
                    Color(0xFFFFB800)
                } else {
                    NuColors.TextSecondary.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        Text(
            text = "${String.format("%.1f", rating)} ($totalRatings ${if (totalRatings == 1) "reseña" else "reseñas"})",
            fontSize = 14.sp,
            color = NuColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RatingStarsSelector(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Calificación:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NuColors.TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val starValue = index + 1
                IconButton(
                    onClick = { onRatingChange(starValue) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (starValue <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "$starValue estrellas",
                        tint = if (starValue <= rating) Color(0xFFFFB800) else NuColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d 'de' MMMM", Locale("es"))
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
