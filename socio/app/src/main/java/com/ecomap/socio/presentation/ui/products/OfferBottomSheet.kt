package com.ecomap.socio.presentation.ui.products

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 🎁 Bottom Sheet para configurar ofertas de productos
 * - Diseño minimalista y profesional
 * - Chips en forma de píldoras
 * - Slide-to-confirm para aplicar oferta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferBottomSheet(
    productName: String,
    productPrice: String,
    onApplyOffer: (OfferData) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    var showConfirmation by remember { mutableStateOf(true) }
    var showFullConfig by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Estados de la oferta
    var selectedOfferType by remember { mutableStateOf<String?>(null) }
    var customOfferType by remember { mutableStateOf("") }
    var selectedDiscount by remember { mutableStateOf<Int?>(null) }
    var customDiscount by remember { mutableStateOf("") }
    var selectedValidDays by remember { mutableStateOf<Int?>(null) }
    var customValidDays by remember { mutableStateOf("") }

    // Estado del slide button
    var slideOffset by remember { mutableStateOf(0f) }
    var isSlideComplete by remember { mutableStateOf(false) }

    // Verificar si hay configuración para advertir
    val hasConfiguration = selectedOfferType != null || customOfferType.isNotBlank() ||
                          selectedDiscount != null || customDiscount.isNotBlank() ||
                          selectedValidDays != null || customValidDays.isNotBlank()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = showFullConfig // Solo el paso 2 sube completamente
    )

    // Expandir completamente cuando se muestra la configuración
    LaunchedEffect(showFullConfig) {
        if (showFullConfig) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NuColors.Background,
        dragHandle = if (showFullConfig) null else { {} }, // Sin drag en paso 2, con drag en paso 1
        modifier = if (showFullConfig) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = showFullConfig,
            transitionSpec = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn() togetherWith
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut()
            },
            label = "offerAnimation"
        ) { showFull ->
            if (!showFull) {
                // PASO 1: Confirmación inicial (mitad de pantalla)
                OfferConfirmationStep(
                    productName = productName,
                    onYes = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        showFullConfig = true
                    },
                    onNo = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onDismiss()
                    }
                )
            } else {
                // PASO 2: Configuración completa de oferta (pantalla completa)
                OfferConfigurationStep(
                    productName = productName,
                    productPrice = productPrice,
                    selectedOfferType = selectedOfferType,
                    onOfferTypeSelected = { selectedOfferType = it },
                    customOfferType = customOfferType,
                    onCustomOfferTypeChange = { customOfferType = it },
                    selectedDiscount = selectedDiscount,
                    onDiscountSelected = { selectedDiscount = it },
                    customDiscount = customDiscount,
                    onCustomDiscountChange = { customDiscount = it },
                    selectedValidDays = selectedValidDays,
                    onValidDaysSelected = { selectedValidDays = it },
                    customValidDays = customValidDays,
                    onCustomValidDaysChange = { customValidDays = it },
                    slideOffset = slideOffset,
                    onSlideOffsetChange = { slideOffset = it },
                    isSlideComplete = isSlideComplete,
                    onSlideComplete = { isSlideComplete = it },
                    hasConfiguration = hasConfiguration,
                    showCancelDialog = showCancelDialog,
                    onShowCancelDialog = { showCancelDialog = it },
                    onDismiss = onDismiss,
                    onApplyOffer = { offerData ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onApplyOffer(offerData)
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * PASO 1: Confirmación inicial - Diseño minimalista y profesional
 */
@Composable
private fun OfferConfirmationStep(
    productName: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Drag handle visual
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(
                    Color.Gray.copy(alpha = 0.3f),
                    RoundedCornerShape(2.dp)
                )
        )

        // Icono minimalista con animación
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Color(0xFFFF6B6B).copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(36.dp)
            )
        }

        // Texto principal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Crear Oferta Especial",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NuColors.Primary.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = productName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    maxLines = 2
                )
            }

            Text(
                text = "Atrae más clientes con descuentos y promociones",
                fontSize = 13.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Botones modernos
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón SÍ - Principal
            Button(
                onClick = onYes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Configurar Oferta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }

            // Botón NO - Secundario
            TextButton(
                onClick = onNo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Ahora no",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * PASO 2: Configuración completa de la oferta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferConfigurationStep(
    productName: String,
    productPrice: String,
    selectedOfferType: String?,
    onOfferTypeSelected: (String?) -> Unit,
    customOfferType: String,
    onCustomOfferTypeChange: (String) -> Unit,
    selectedDiscount: Int?,
    onDiscountSelected: (Int?) -> Unit,
    customDiscount: String,
    onCustomDiscountChange: (String) -> Unit,
    selectedValidDays: Int?,
    onValidDaysSelected: (Int?) -> Unit,
    customValidDays: String,
    onCustomValidDaysChange: (String) -> Unit,
    slideOffset: Float,
    onSlideOffsetChange: (Float) -> Unit,
    isSlideComplete: Boolean,
    onSlideComplete: (Boolean) -> Unit,
    hasConfiguration: Boolean,
    showCancelDialog: Boolean,
    onShowCancelDialog: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApplyOffer: (OfferData) -> Unit
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current

    // Estados para mostrar campos personalizados
    var showCustomOfferField by remember { mutableStateOf(false) }
    var showCustomDiscountField by remember { mutableStateOf(false) }
    var showCustomValidDaysField by remember { mutableStateOf(false) }

    // Diálogo de confirmación al cancelar
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { onShowCancelDialog(false) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Cancelar configuración?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Perderás toda la configuración de la oferta que has realizado.",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelDialog(false)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onShowCancelDialog(false) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar editando")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TopAppBar fijo con botón de cerrar
        TopAppBar(
            title = { Text("Configurar Oferta", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = {
                    if (hasConfiguration) {
                        onShowCancelDialog(true)
                    } else {
                        onDismiss()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancelar",
                        tint = Color.Black // Botón negro para mejor visibilidad
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NuColors.Background,
                titleContentColor = Color.Black
            )
        )

        // Contenido scrolleable
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Header con icono y título
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B),
                                Color(0xFFFF8E53)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Discount,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Configurar Oferta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )
            Text(
                text = productName,
                fontSize = 14.sp,
                color = NuColors.TextSecondary
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.2f)
        )

        // 1. TIPO DE OFERTA
        OfferSection(
            title = "Tipo de Oferta",
            icon = Icons.Default.Category
        ) {
            val offerTypes = listOf("2x1", "3x2", "%OFF", "PRECIO")

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                offerTypes.forEach { type ->
                    PillChip(
                        label = type,
                        isSelected = selectedOfferType == type,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onOfferTypeSelected(if (selectedOfferType == type) null else type)
                            // Resetear "Otro" cuando seleccionas un chip predefinido
                            showCustomOfferField = false
                            onCustomOfferTypeChange("")
                        }
                    )
                }

                // Chip "Otro" con icono
                PillChip(
                    label = if (customOfferType.isBlank()) "Otro" else customOfferType.take(10) + if (customOfferType.length > 10) "..." else "",
                    isSelected = showCustomOfferField || customOfferType.isNotBlank(),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onOfferTypeSelected(null) // Desmarcar chips predefinidos
                        showCustomOfferField = !showCustomOfferField
                        if (!showCustomOfferField) {
                            onCustomOfferTypeChange("")
                        }
                    }
                )
            }

            // Campo personalizado - siempre visible cuando "Otro" está seleccionado
            AnimatedVisibility(
                visible = showCustomOfferField,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CustomTextField(
                        value = customOfferType,
                        onValueChange = onCustomOfferTypeChange,
                        placeholder = "Ej: Lleva 4 paga 3"
                    )
                    Text(
                        text = "Describe tu oferta personalizada",
                        fontSize = 11.sp,
                        color = NuColors.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // 2. DESCUENTO (solo si es %OFF o PRECIO)
        AnimatedVisibility(
            visible = selectedOfferType == "%OFF" || selectedOfferType == "PRECIO",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OfferSection(
                title = if (selectedOfferType == "%OFF") "Porcentaje de Descuento" else "Precio con descuento",
                icon = Icons.Default.Percent
            ) {
                if (selectedOfferType == "%OFF") {
                    val discounts = listOf(10, 20, 30, 50)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        discounts.forEach { discount ->
                            PillChip(
                                label = "$discount%",
                                isSelected = selectedDiscount == discount,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onDiscountSelected(if (selectedDiscount == discount) null else discount)
                                    // Resetear "Otro %" cuando seleccionas un chip predefinido
                                    showCustomDiscountField = false
                                    onCustomDiscountChange("")
                                }
                            )
                        }

                        PillChip(
                            label = if (customDiscount.isBlank()) "Otro %" else "${customDiscount}%",
                            isSelected = showCustomDiscountField || customDiscount.isNotBlank(),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onDiscountSelected(null) // Desmarcar chips predefinidos
                                showCustomDiscountField = !showCustomDiscountField
                                if (!showCustomDiscountField) {
                                    onCustomDiscountChange("")
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = showCustomDiscountField,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CustomTextField(
                                value = customDiscount.trim(),
                                onValueChange = {
                                    val trimmed = it.trim()
                                    if (trimmed.isEmpty()) {
                                        onCustomDiscountChange("")
                                    } else if (trimmed.toIntOrNull() != null && trimmed.toInt() in 1..99) {
                                        onCustomDiscountChange(trimmed)
                                        onDiscountSelected(null)
                                    }
                                },
                                placeholder = "Ej: 25",
                                keyboardType = KeyboardType.Number,
                                suffix = "%"
                            )
                            Text(
                                text = "Ingresa un descuento entre 1% y 99%",
                                fontSize = 11.sp,
                                color = NuColors.TextSecondary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Preview del precio con descuento
                    if (selectedDiscount != null || customDiscount.isNotBlank()) {
                        val discount = selectedDiscount ?: customDiscount.toIntOrNull() ?: 0
                        val originalPrice = productPrice.toDoubleOrNull() ?: 0.0
                        val newPrice = originalPrice * (1 - discount / 100.0)

                        PricePreview(
                            originalPrice = originalPrice,
                            newPrice = newPrice,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    // Campo para nuevo precio
                    val originalPrice = productPrice.toDoubleOrNull() ?: 0.0
                    val newPrice = customDiscount.toDoubleOrNull() ?: 0.0
                    val isPriceInvalid = customDiscount.isNotBlank() && newPrice >= originalPrice

                    CustomTextField(
                        value = customDiscount,
                        onValueChange = onCustomDiscountChange,
                        placeholder = "Precio con descuento",
                        keyboardType = KeyboardType.Decimal,
                        prefix = "$"
                    )

                    // Validación: precio descuento debe ser menor al original
                    if (isPriceInvalid) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFEF5350)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "El precio con descuento debe ser menor al precio original ($${String.format("%.2f", originalPrice)})",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    if (customDiscount.isNotBlank() && newPrice > 0 && !isPriceInvalid) {
                        PricePreview(
                            originalPrice = originalPrice,
                            newPrice = newPrice,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        // 3. VIGENCIA
        OfferSection(
            title = "Vigencia de la Oferta",
            icon = Icons.Default.DateRange
        ) {
            val validityOptions = listOf(
                "Hoy" to 0,
                "3 días" to 3,
                "1 sem" to 7,
                "2 sem" to 14
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                validityOptions.forEach { (label, days) ->
                    PillChip(
                        label = label,
                        isSelected = selectedValidDays == days,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onValidDaysSelected(if (selectedValidDays == days) null else days)
                            // Resetear "Otro" cuando seleccionas un chip predefinido
                            showCustomValidDaysField = false
                            onCustomValidDaysChange("")
                        }
                    )
                }

                PillChip(
                    label = if (customValidDays.isBlank()) "Otro" else "${customValidDays}d",
                    isSelected = showCustomValidDaysField || customValidDays.isNotBlank(),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onValidDaysSelected(null) // Desmarcar chips predefinidos
                        showCustomValidDaysField = !showCustomValidDaysField
                        if (!showCustomValidDaysField) {
                            onCustomValidDaysChange("")
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = showCustomValidDaysField,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CustomTextField(
                        value = customValidDays.trim(),
                        onValueChange = {
                            val trimmed = it.trim()
                            if (trimmed.isEmpty()) {
                                onCustomValidDaysChange("")
                            } else if (trimmed.toIntOrNull() != null && trimmed.toInt() in 1..30) {
                                onCustomValidDaysChange(trimmed)
                                onValidDaysSelected(null)
                            }
                        },
                        placeholder = "Días (máx 30)",
                        keyboardType = KeyboardType.Number,
                        suffix = "días"
                    )
                    Text(
                        text = "Ingresa un número entre 1 y 30 días",
                        fontSize = 11.sp,
                        color = NuColors.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Mostrar fecha de expiración
            val validDays = selectedValidDays ?: customValidDays.toIntOrNull()

            if (validDays != null) {
                val expiryDate = LocalDate.now().plusDays(validDays.toLong())
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = NuColors.Success.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        NuColors.Success.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = NuColors.Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Válida hasta: ${expiryDate.format(formatter)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NuColors.Success
                        )
                    }
                }
            }
        }

        } // Fin del Column scrolleable

        // SLIDE TO CONFIRM BUTTON - Fuera del scroll, fijo al final
        val originalPrice = productPrice.toDoubleOrNull() ?: 0.0
        val newPrice = customDiscount.toDoubleOrNull() ?: 0.0
        val isPriceValid = selectedOfferType != "PRECIO" ||
                          (customDiscount.isNotBlank() && newPrice > 0 && newPrice < originalPrice)

        val isValid = (selectedOfferType != null || customOfferType.isNotBlank()) &&
                (selectedValidDays != null || customValidDays.isNotBlank()) &&
                isPriceValid

        if (isValid) {
            SlideToConfirmButton(
                text = "Desliza para aplicar oferta",
                onSlideComplete = {
                    val offerData = OfferData(
                        offerType = selectedOfferType ?: customOfferType.ifBlank { "custom" },
                        discountPercentage = when {
                            selectedOfferType == "%OFF" -> selectedDiscount ?: customDiscount.toIntOrNull()
                            else -> null
                        },
                        newPrice = when {
                            selectedOfferType == "PRECIO" -> customDiscount.toDoubleOrNull()
                            else -> null
                        },
                        validDays = selectedValidDays ?: customValidDays.toIntOrNull() ?: 0,
                        offerDescription = when {
                            selectedOfferType == "2x1" -> "2x1"
                            selectedOfferType == "3x2" -> "3x2"
                            selectedOfferType == "%OFF" -> "${selectedDiscount ?: customDiscount}% OFF"
                            selectedOfferType == "PRECIO" -> "Precio especial"
                            else -> customOfferType
                        }
                    )
                    onApplyOffer(offerData)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            )
        }
    } // Fin del Column principal
}

/**
 * Sección de configuración con título e icono
 */
@Composable
private fun OfferSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )
        }

        content()
    }
}

/**
 * Chip en forma de píldora
 */
@Composable
private fun PillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(50),
        color = if (isSelected) NuColors.Primary else NuColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 0.dp else 1.dp,
            color = if (isSelected) Color.Transparent else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else NuColors.TextPrimary
        )
    }
}

/**
 * Campo de texto personalizado
 */
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    suffix: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NuColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            prefix?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.Primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.TextPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(NuColors.Primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            suffix?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * Preview del precio con descuento
 */
@Composable
private fun PricePreview(
    originalPrice: Double,
    newPrice: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFFFA726)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Precio antes:",
                    fontSize = 12.sp,
                    color = NuColors.TextSecondary
                )
                Text(
                    text = "$${String.format("%.2f", originalPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextSecondary,
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFFFA726),
                modifier = Modifier.size(24.dp)
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Precio ahora:",
                    fontSize = 12.sp,
                    color = NuColors.TextSecondary
                )
                Text(
                    text = "$${String.format("%.2f", newPrice)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
            }
        }
    }
}

/**
 * Botón de deslizar para confirmar - Versión mejorada y fluida
 */
@Composable
private fun SlideToConfirmButton(
    text: String,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val threshold = 0.75f // 75% del ancho para completar
    val thumbSize = 56f

    // Animación suave al resetear
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(
            durationMillis = if (offsetX == 0f) 300 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "slideAnimation"
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .onGloballyPositioned { coordinates ->
                // Calcular el ancho máximo permitido para el deslizamiento
                if (containerWidth == 0f) {
                    containerWidth = coordinates.size.width.toFloat() - thumbSize
                }
            }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF6B6B),
                        Color(0xFFFF8E53)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .pointerInput(containerWidth) {
                detectDragGestures(
                    onDragEnd = {
                        // Verificar si se completó el deslizamiento
                        if (offsetX >= containerWidth * threshold) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            offsetX = 0f
                            onSlideComplete()
                        } else {
                            // Regresar suavemente a la posición inicial
                            offsetX = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Solo permitir arrastre horizontal hacia la derecha
                        val newOffset = (offsetX + dragAmount.x).coerceIn(0f, containerWidth)
                        offsetX = newOffset

                        // Feedback háptico al alcanzar el threshold
                        if (newOffset >= containerWidth * threshold && offsetX < containerWidth * threshold) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    }
                )
            }
    ) {
        // Texto central
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f)
        )

        // Slider thumb (círculo arrastrable)
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.toInt(), 0) }
                .size(56.dp)
                .padding(4.dp)
                .background(
                    Color.White,
                    CircleShape
                )
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            // Icono de flecha con animación de rebote
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        // Escalar ligeramente cuando se alcanza el threshold
                        val scale = if (offsetX >= containerWidth * threshold) 1.2f else 1f
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }

        // Indicador de progreso en el fondo
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(
                    with(androidx.compose.ui.platform.LocalDensity.current) {
                        animatedOffsetX.toDp() + 28.dp
                    }
                )
                .background(
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
                )
                .align(Alignment.CenterStart)
        )
    }
}

/**
 * Data class para los datos de la oferta
 */
data class OfferData(
    val offerType: String,
    val discountPercentage: Int?,
    val newPrice: Double?,
    val validDays: Int,
    val offerDescription: String
)
