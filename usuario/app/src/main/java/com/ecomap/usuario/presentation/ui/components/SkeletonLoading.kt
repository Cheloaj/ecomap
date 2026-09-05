package com.ecomap.usuario.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Componentes de Skeleton Loading para mejorar percepción de velocidad
 *
 * Uso:
 * - Muestra placeholders mientras carga contenido real
 * - Mejora UX dando feedback visual inmediato
 * - Reduce percepción de lentitud
 */

/**
 * Shimmer Effect - Animación de brillo que se mueve horizontalmente
 */
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation - 1000f, y = 0f),
        end = Offset(x = translateAnimation, y = 0f)
    )
}

/**
 * SkeletonBox - Caja rectangular con shimmer
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = shimmerBrush())
    )
}

/**
 * SkeletonCircle - Círculo con shimmer (para avatares)
 */
@Composable
fun SkeletonCircle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush = shimmerBrush())
    )
}

/**
 * SkeletonText - Línea de texto con shimmer
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    height: Int = 16
) {
    SkeletonBox(
        modifier = modifier.height(height.dp),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * BusinessCardSkeleton - Tarjeta completa de negocio
 */
@Composable
fun BusinessCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular
            SkeletonCircle(
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información del negocio
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nombre del negocio
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    height = 20
                )

                // Categoría
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    height = 14
                )

                // Rating y distancia
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonText(
                        modifier = Modifier.width(60.dp),
                        height = 14
                    )
                    SkeletonText(
                        modifier = Modifier.width(80.dp),
                        height = 14
                    )
                }
            }
        }
    }
}

/**
 * ProductCardSkeleton - Tarjeta completa de producto
 */
@Composable
fun ProductCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagen del producto
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )

            // Información del producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nombre del producto
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    height = 16
                )

                // Precio
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(0.4f),
                    height = 18
                )

                // Stock/Info adicional
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    height = 14
                )
            }
        }
    }
}

/**
 * BusinessListSkeleton - Lista de 5 tarjetas de negocio
 */
@Composable
fun BusinessListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(itemCount) {
            BusinessCardSkeleton()
        }
    }
}

/**
 * ProductGridSkeleton - Grid de 6 productos
 */
@Composable
fun ProductGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ProductCardSkeleton()
        }
    }
}

/**
 * BusinessDetailHeaderSkeleton - Header de detalle de negocio
 */
@Composable
fun BusinessDetailHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo y nombre
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(modifier = Modifier.size(80.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonText(modifier = Modifier.fillMaxWidth(0.7f), height = 24)
                SkeletonText(modifier = Modifier.fillMaxWidth(0.5f), height = 16)
            }
        }

        // Rating y estadísticas
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonText(modifier = Modifier.width(80.dp), height = 16)
            SkeletonText(modifier = Modifier.width(100.dp), height = 16)
        }

        // Descripción
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonText(modifier = Modifier.fillMaxWidth(), height = 14)
            SkeletonText(modifier = Modifier.fillMaxWidth(0.9f), height = 14)
            SkeletonText(modifier = Modifier.fillMaxWidth(0.6f), height = 14)
        }

        // Horarios
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}
