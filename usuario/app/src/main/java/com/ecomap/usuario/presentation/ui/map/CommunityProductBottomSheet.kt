package com.ecomap.usuario.presentation.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.ImageConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityProductBottomSheet(
    product: Product,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen del producto
            if (!product.imageUrl.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Encabezado con nombre y precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary
                        )
                        // Badge "Comunitario"
                        Surface(
                            color = AppleColors.IOSBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AppleColors.IOSBlue
                                )
                                Text(
                                    text = "Comunitario",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppleColors.IOSBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = product.categoryName ?: "Producto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuColors.TextSecondary
                    )
                }

                // Precio destacado
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$${product.price}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleColors.IOSGreen
                    )
                    Text(
                        text = "por ${product.unit ?: "unidad"}",
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary
                    )
                }
            }

            // Descripción
            if (!product.description.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = NuColors.Primary
                            )
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NuColors.TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Información de ubicación
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppleColors.IOSBlue.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AppleColors.IOSBlue
                        )
                        Text(
                            text = "Ubicación",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleColors.IOSBlue
                        )
                    }

                    Text(
                        text = product.locationAddress ?: "Ubicación comunitaria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1F1F1F)
                    )

                    // Botón para abrir en maps
                    if (product.latitude != null && product.longitude != null) {
                        OutlinedButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:${product.latitude},${product.longitude}?q=${product.latitude},${product.longitude}(${product.name})")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback si no tiene Google Maps instalado
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${product.latitude},${product.longitude}"))
                                    context.startActivity(browserIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppleColors.IOSBlue
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver en Maps", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Información del vendedor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar del vendedor
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        NuColors.Primary,
                                        NuColors.PrimaryLight
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = product.ownerName ?: "Vendedor comunitario",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (product.ownerPhone != null) {
                            Text(
                                text = product.ownerPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = NuColors.TextSecondary
                            )
                        }
                    }

                    // Botón de contacto
                    if (product.ownerPhone != null) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${product.ownerPhone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    AppleColors.IOSGreen.copy(alpha = 0.15f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Llamar",
                                tint = AppleColors.IOSGreen
                            )
                        }
                    }
                }
            }

            // Botón de cerrar
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Cerrar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
