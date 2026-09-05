package com.ecomap.socio.presentation.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.ui.theme.NuColors

@Composable
fun BusinessSelectorDialog(
    businesses: List<Business>,
    onBusinessSelected: (Business) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBusiness by remember { mutableStateOf<Business?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Selecciona tu Negocio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¿Para qué negocio deseas programar este producto?",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                businesses.forEach { business ->
                    BusinessOption(
                        business = business,
                        isSelected = selectedBusiness?.id == business.id,
                        onClick = { selectedBusiness = business }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedBusiness?.let { onBusinessSelected(it) }
                },
                enabled = selectedBusiness != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NuColors.Primary
                )
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = NuColors.TextSecondary)
            }
        }
    )
}

@Composable
fun BusinessOption(
    business: Business,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NuColors.Primary.copy(alpha = 0.1f) else NuColors.Background,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, NuColors.Primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del negocio
            if (business.avatarUrl != null) {
                AsyncImage(
                    model = business.avatarUrl,
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
                        .background(NuColors.Primary.copy(alpha = 0.2f)),
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

            // Información del negocio
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = business.businessName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )

                Text(
                    text = business.address.ifEmpty { "Sin dirección" },
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary
                )
            }

            // Indicador de selección
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
