package com.ecomap.usuario.presentation.ui.products

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.ProductRating
import com.ecomap.usuario.presentation.viewmodel.RatingUiState
import com.ecomap.usuario.presentation.viewmodel.RatingViewModel
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.ImageConfig
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewsScreen(
    productId: String,
    productName: String,
    onNavigateBack: () -> Unit,
    ratingViewModel: RatingViewModel = hiltViewModel()
) {
    val ratingsState by ratingViewModel.ratingsState.collectAsState()

    LaunchedEffect(productId) {
        ratingViewModel.loadProductRatings(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reseñas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = productName,
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                },
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
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
                            fontSize = 16.sp
                        )
                    }
                }
            }

            is RatingUiState.Success -> {
                AllReviewsContent(
                    ratings = state.ratings,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun AllReviewsContent(
    ratings: List<ProductRating>,
    modifier: Modifier = Modifier
) {
    if (ratings.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = NuColors.TextSecondary.copy(alpha = 0.3f)
                )
                Text(
                    text = "Sin reseñas aún",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )
                Text(
                    text = "Sé el primero en calificar este producto",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con estadísticas
            item {
                ReviewsHeader(ratings = ratings)
            }

            // Lista de todas las reseñas
            items(ratings) { rating ->
                FullReviewCard(rating = rating)
            }
        }
    }
}

@Composable
private fun ReviewsHeader(
    ratings: List<ProductRating>,
    modifier: Modifier = Modifier
) {
    val averageRating = if (ratings.isNotEmpty()) {
        ratings.map { it.rating }.average()
    } else 0.0

    val ratingCounts = ratings.groupingBy { it.rating }.eachCount()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NuColors.Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calificación promedio grande
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.1f", averageRating),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.Primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < averageRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFB800)
                            )
                        }
                    }
                    Text(
                        text = "${ratings.size} ${if (ratings.size == 1) "reseña" else "reseñas"}",
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary
                    )
                }

                // Barras de distribución
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (stars in 5 downTo 1) {
                        val count = ratingCounts[stars] ?: 0
                        val percentage = if (ratings.isNotEmpty()) {
                            (count.toFloat() / ratings.size)
                        } else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$stars",
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary,
                                modifier = Modifier.width(16.dp)
                            )
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp),
                                color = Color(0xFFFFB800),
                                trackColor = NuColors.Background,
                            )
                            Text(
                                text = count.toString(),
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullReviewCard(
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con nombre y estrellas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = rating.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )
                    Text(
                        text = formatDate(rating.createdAt),
                        fontSize = 12.sp,
                        color = NuColors.TextSecondary.copy(alpha = 0.6f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(rating.rating) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFFB800)
                        )
                    }
                }
            }

            // Comentario
            if (!rating.comment.isNullOrBlank()) {
                Text(
                    text = rating.comment,
                    fontSize = 15.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 22.sp
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

            // Respuesta del vendedor
            if (rating.hasVendorResponse) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
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

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es"))
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
