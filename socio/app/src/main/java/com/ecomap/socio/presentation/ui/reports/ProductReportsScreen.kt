package com.ecomap.socio.presentation.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.presentation.viewmodel.ReportsViewModel
import com.ecomap.socio.ui.theme.NuColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Colores Premium
private object PremiumColors {
    val Background = Color(0xFFF8F9FA)
    val Surface = Color.White
    val TextPrimary = Color(0xFF0F1419)
    val TextSecondary = Color(0xFF536471)
    val TextTertiary = Color(0xFF8B98A5)
    val Border = Color(0xFFE8EBED)
    val Accent = Color(0xFF0F1419)
    val AccentSubtle = Color(0xFFF7F7F8)
    val Success = Color(0xFF059669)
    val SuccessSubtle = Color(0xFFECFDF5)
    val Warning = Color(0xFFEA580C)
    val WarningSubtle = Color(0xFFFEF3C7)
    val Error = Color(0xFFDC2626)
    val ErrorSubtle = Color(0xFFFEE2E2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val reportsState by viewModel.reportsState.collectAsState()
    var selectedComplaint by remember { mutableStateOf<com.ecomap.socio.data.model.ProductComplaint?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadVendorReports()
    }

    // Mostrar mensaje de éxito
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2500)
            showSuccessMessage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestión de Reportes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = PremiumColors.TextPrimary,
                        letterSpacing = (-0.2).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = PremiumColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PremiumColors.Surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = PremiumColors.Success,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        containerColor = PremiumColors.Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = reportsState) {
                is com.ecomap.socio.utils.UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PremiumColors.Accent,
                            strokeWidth = 2.dp
                        )
                    }
                }

                is com.ecomap.socio.utils.UiState.Success -> {
                    val complaints = state.data

                    if (complaints.isEmpty()) {
                        EmptyReportsState(modifier = Modifier.padding(padding))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(complaints, key = { it.id }) { complaint ->
                                ComplaintCard(
                                    complaint = complaint,
                                    onClick = { selectedComplaint = complaint }
                                )
                            }
                        }
                    }
                }

                is com.ecomap.socio.utils.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadVendorReports() },
                        modifier = Modifier.padding(padding)
                    )
                }

                else -> {}
            }

            // Success banner flotante
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding() + 16.dp)
            ) {
                Surface(
                    color = PremiumColors.Success,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Respuesta enviada correctamente",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    selectedComplaint?.let { complaint ->
        ResponseDialog(
            complaint = complaint,
            onDismiss = { selectedComplaint = null },
            onSubmit = { response ->
                viewModel.respondToComplaint(complaint.id, response)
                selectedComplaint = null
                showSuccessMessage = true
            }
        )
    }
}

@Composable
internal fun ComplaintCard(
    complaint: com.ecomap.socio.data.model.ProductComplaint,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasResponse = !complaint.vendorResponse.isNullOrBlank()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PremiumColors.Surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (hasResponse) BorderStroke(1.5.dp, PremiumColors.Success) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Estado y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasResponse) {
                    // Badge de "Respondido"
                    Surface(
                        color = PremiumColors.SuccessSubtle,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PremiumColors.Success,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Respondido",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PremiumColors.Success,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                } else {
                    StatusBadge(status = complaint.status)
                }

                Text(
                    text = formatDate(complaint.createdAt),
                    fontSize = 12.sp,
                    color = PremiumColors.TextTertiary,
                    fontWeight = FontWeight.Normal
                )
            }

            // Producto reportado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!complaint.productImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = complaint.productImageUrl,
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PremiumColors.AccentSubtle),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PremiumColors.AccentSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = PremiumColors.TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.productName ?: "Producto sin nombre",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumColors.TextPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getReasonText(complaint.reason),
                        fontSize = 13.sp,
                        color = PremiumColors.Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Descripción del reporte
            if (!complaint.description.isNullOrBlank()) {
                Text(
                    text = complaint.description,
                    fontSize = 14.sp,
                    color = PremiumColors.TextSecondary,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Imagen de evidencia
            if (!complaint.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = complaint.imageUrl,
                    imageLoader = ImageConfig.getImageLoader(context),
                    contentDescription = "Evidencia",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PremiumColors.Border, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Información del usuario
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PremiumColors.AccentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PremiumColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = complaint.userName ?: "Cliente",
                    fontSize = 13.sp,
                    color = PremiumColors.TextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }

            // Respuesta del vendedor (si existe)
            if (!complaint.vendorResponse.isNullOrBlank()) {
                HorizontalDivider(color = PremiumColors.Border, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PremiumColors.SuccessSubtle, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = PremiumColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tu respuesta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PremiumColors.Success,
                            letterSpacing = 0.sp
                        )
                    }
                    Text(
                        text = complaint.vendorResponse,
                        fontSize = 14.sp,
                        color = PremiumColors.TextPrimary,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal
                    )
                    if (complaint.vendorResponseAt != null) {
                        Text(
                            text = "Enviado ${formatDate(complaint.vendorResponseAt)}",
                            fontSize = 11.sp,
                            color = PremiumColors.TextTertiary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                // Botón para responder
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumColors.Accent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Responder reporte",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, text) = when (status) {
        "pending" -> Triple(PremiumColors.WarningSubtle, PremiumColors.Warning, "Pendiente")
        "resolved" -> Triple(PremiumColors.SuccessSubtle, PremiumColors.Success, "Resuelto")
        "dismissed" -> Triple(PremiumColors.ErrorSubtle, PremiumColors.Error, "Descartado")
        else -> Triple(PremiumColors.AccentSubtle, PremiumColors.TextSecondary, "Sin estado")
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            letterSpacing = 0.sp
        )
    }
}

data class QuickResponse(
    val title: String,
    val message: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
internal fun ResponseDialog(
    complaint: com.ecomap.socio.data.model.ProductComplaint,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<QuickResponse?>(null) }
    var customResponse by remember { mutableStateOf("") }
    var showCustomField by remember { mutableStateOf(false) }

    val quickResponses = remember {
        listOf(
            QuickResponse(
                "Disculpa y solución",
                "Lamentamos los inconvenientes. Hemos revisado tu reporte y tomaremos las medidas necesarias para resolver esta situación de inmediato.",
                Icons.Default.CheckCircle
            ),
            QuickResponse(
                "Solicitar más información",
                "Gracias por tu reporte. Para poder atender mejor tu caso, necesitamos información adicional. ¿Podrías contactarnos directamente?",
                Icons.Default.Info
            ),
            QuickResponse(
                "Producto corregido",
                "Agradecemos tu observación. Ya hemos actualizado la información del producto para reflejar datos precisos.",
                Icons.Default.Edit
            ),
            QuickResponse(
                "Respuesta personalizada",
                "",
                Icons.Default.Create
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PremiumColors.Surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Responder reporte",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = PremiumColors.TextPrimary,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    "Selecciona una respuesta o personalízala",
                    fontSize = 13.sp,
                    color = PremiumColors.TextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info del reporte
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PremiumColors.AccentSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PremiumColors.TextSecondary
                        )
                        Text(
                            complaint.productName ?: "Sin nombre",
                            fontSize = 13.sp,
                            color = PremiumColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        getReasonText(complaint.reason),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PremiumColors.Error
                    )
                    if (!complaint.description.isNullOrBlank()) {
                        HorizontalDivider(color = PremiumColors.Border, thickness = 1.dp)
                        Text(
                            complaint.description,
                            fontSize = 13.sp,
                            color = PremiumColors.TextSecondary,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Quick responses
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickResponses.forEach { response ->
                        val isSelected = selectedTemplate == response
                        val isCustom = response.title == "Respuesta personalizada"

                        Surface(
                            onClick = {
                                selectedTemplate = response
                                if (isCustom) {
                                    showCustomField = true
                                    customResponse = ""
                                } else {
                                    showCustomField = false
                                    customResponse = response.message
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PremiumColors.Accent else PremiumColors.Surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PremiumColors.Accent else PremiumColors.Border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    response.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) Color.White else PremiumColors.TextSecondary
                                )
                                Text(
                                    response.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else PremiumColors.TextPrimary,
                                    letterSpacing = 0.sp
                                )
                            }
                        }
                    }
                }

                // Vista previa o campo personalizado
                if (showCustomField || selectedTemplate?.message?.isEmpty() == true) {
                    OutlinedTextField(
                        value = customResponse,
                        onValueChange = { if (it.length <= 500) customResponse = it },
                        label = {
                            Text(
                                "Escribe tu respuesta",
                                fontSize = 13.sp,
                                color = PremiumColors.TextSecondary
                            )
                        },
                        supportingText = {
                            Text(
                                "${customResponse.length}/500",
                                fontSize = 11.sp,
                                color = PremiumColors.TextTertiary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumColors.Accent,
                            unfocusedBorderColor = PremiumColors.Border,
                            focusedTextColor = PremiumColors.TextPrimary,
                            unfocusedTextColor = PremiumColors.TextPrimary
                        )
                    )
                } else if (selectedTemplate != null && selectedTemplate!!.message.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PremiumColors.AccentSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Vista previa:",
                            fontSize = 11.sp,
                            color = PremiumColors.TextTertiary,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            selectedTemplate!!.message,
                            fontSize = 13.sp,
                            color = PremiumColors.TextSecondary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalResponse = if (showCustomField || selectedTemplate?.message?.isEmpty() == true) {
                        customResponse
                    } else {
                        selectedTemplate?.message ?: ""
                    }
                    if (finalResponse.isNotBlank()) onSubmit(finalResponse)
                },
                enabled = (showCustomField && customResponse.isNotBlank()) ||
                        (selectedTemplate != null && selectedTemplate!!.message.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumColors.Accent,
                    disabledContainerColor = PremiumColors.Border
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Enviar respuesta",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Cancelar",
                    color = PremiumColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    )
}

@Composable
private fun EmptyReportsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(PremiumColors.SuccessSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = PremiumColors.Success
                )
            }
            Text(
                "Todo en orden",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = PremiumColors.TextPrimary,
                letterSpacing = (-0.3).sp
            )
            Text(
                "No hay reportes pendientes por atender",
                fontSize = 14.sp,
                color = PremiumColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = PremiumColors.Error
            )
            Text(
                message,
                fontSize = 14.sp,
                color = PremiumColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Normal
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumColors.Accent
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Reintentar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

internal fun getReasonText(reason: String): String {
    return when (reason) {
        "misleading" -> "Información engañosa"
        "poor_quality" -> "Calidad deficiente"
        "wrong_price" -> "Precio incorrecto"
        "unavailable" -> "No disponible"
        "inappropriate" -> "Contenido inapropiado"
        "other" -> "Otro motivo"
        else -> reason
    }
}

internal fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMM, HH:mm", Locale("es"))
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}