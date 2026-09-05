package com.ecomap.socio.presentation.ui.main

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.data.model.Business
import com.ecomap.socio.presentation.ui.products.SwipeButtonRed
import com.ecomap.socio.presentation.ui.products.SwipeButtonGreen
import com.ecomap.socio.ui.theme.NuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBusinessBottomSheet(
    businesses: List<Business>,
    onDeactivateBusiness: (String) -> Unit,
    onReactivateBusiness: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var selectedBusiness by remember { mutableStateOf<Business?>(null) }
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Filtrar TODOS los negocios aprobados (activos e inactivos)
    val approvedBusinesses = businesses.filter {
        it.verificationStatus == "approved"
    }

    val filteredBusinesses = remember(approvedBusinesses, searchQuery) {
        if (searchQuery.isBlank()) {
            approvedBusinesses
        } else {
            approvedBusinesses.filter { business ->
                business.businessName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NuColors.Background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NuColors.TextSecondary.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Título
            Text(
                text = when {
                    selectedBusiness == null -> "Gestionar Negocios"
                    selectedBusiness?.isActive == true -> "Desactivar Negocio"
                    else -> "Activar Negocio"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = when {
                    selectedBusiness == null -> "Selecciona el negocio que deseas gestionar"
                    selectedBusiness?.isActive == true -> "Revisa los datos antes de desactivar"
                    else -> "Revisa los datos antes de activar"
                },
                fontSize = 14.sp,
                color = NuColors.TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            AnimatedVisibility(
                visible = selectedBusiness == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    // Campo de búsqueda con BasicTextField
                    Card(
                        shape = RoundedCornerShape(23.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = NuColors.Surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFocused) {
                                NuColors.Primary.copy(alpha = 0.5f)
                            } else {
                                NuColors.TextSecondary.copy(alpha = 0.2f)
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NuColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Buscar negocio...",
                                                fontSize = 14.sp,
                                                color = NuColors.TextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }

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
                                                .size(24.dp)
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

                    // Lista de negocios
                    if (filteredBusinesses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = NuColors.TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No se encontraron negocios",
                                    color = NuColors.TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredBusinesses) { business ->
                                BusinessSelectableCard(
                                    business = business,
                                    onClick = { selectedBusiness = business }
                                )
                            }
                        }
                    }
                }
            }

            // Vista de detalles del negocio seleccionado
            AnimatedVisibility(
                visible = selectedBusiness != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedBusiness?.let { business ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card con información del negocio
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Nombre del negocio
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = NuColors.Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = business.businessName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuColors.TextPrimary
                                    )
                                }

                                HorizontalDivider(color = NuColors.Background)

                                // Información detallada
                                BusinessDetailRow(
                                    icon = Icons.Default.Category,
                                    label = "Tipo",
                                    value = business.businessType
                                )

                                BusinessDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    label = "Dirección",
                                    value = business.address.ifEmpty { "Sin dirección" }
                                )

                                BusinessDetailRow(
                                    icon = if (business.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    label = "Estado",
                                    value = if (business.isActive) "Activo" else "Desactivado",
                                    valueColor = if (business.isActive) NuColors.Success else NuColors.Error
                                )
                            }
                        }

                        // Advertencia o Información
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (business.isActive)
                                NuColors.Error.copy(alpha = 0.1f)
                            else
                                NuColors.Success.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (business.isActive) Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (business.isActive) NuColors.Error else NuColors.Success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        if (business.isActive)
                                            "⚠️ Al desactivar este negocio:"
                                        else
                                            "✓ Al activar este negocio:",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (business.isActive) NuColors.Error else NuColors.Success,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (business.isActive)
                                            "• El negocio no será visible para clientes\n" +
                                            "• Los productos se ocultarán automáticamente\n" +
                                            "• Las programaciones se cancelarán\n" +
                                            "• Podrás reactivarlo cuando quieras"
                                        else
                                            "• El negocio será visible nuevamente\n" +
                                            "• Los productos estarán disponibles\n" +
                                            "• Podrás crear nuevas programaciones\n" +
                                            "• Los clientes podrán encontrarte",
                                        fontSize = 13.sp,
                                        color = NuColors.TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // SwipeButton dinámico según estado
                        if (business.isActive) {
                            SwipeButtonRed(
                                text = "Desliza para desactivar",
                                onSwipeComplete = {
                                    onDeactivateBusiness(business.id)
                                    onDismiss()
                                }
                            )
                        } else {
                            SwipeButtonGreen(
                                text = "Desliza para activar",
                                onSwipeComplete = {
                                    onReactivateBusiness(business.id)
                                    onDismiss()
                                }
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessSelectableCard(
    business: Business,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NuColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = business.businessName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NuColors.TextPrimary
                    )
                    if (!business.isActive) {
                        Surface(
                            color = NuColors.Error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Desactivado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = NuColors.Error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = business.address.ifEmpty { "Sin dirección" },
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NuColors.TextSecondary
            )
        }
    }
}

@Composable
private fun BusinessDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = NuColors.TextPrimary
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NuColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = NuColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}
