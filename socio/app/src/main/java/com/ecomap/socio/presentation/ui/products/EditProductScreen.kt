package com.ecomap.socio.presentation.ui.products

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.socio.data.model.Product
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView
import com.ecomap.socio.presentation.viewmodel.ProductViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import com.ecomap.socio.utils.rememberSingleExecution
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🎨 EditProductScreen - Versión Mejorada con validación de canasta básica
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProductScreen(
    productId: String,
    businessId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 🛡️ FASE 1: Protección contra clics rápidos (500ms cooldown)
    val executeSafely = rememberSingleExecution()

    // Focus requesters
    val customUnitFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    val product by viewModel.selectedProduct.collectAsState()
    val updateProductState by viewModel.updateProductState.collectAsState()

    // Form states
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var customUnit by remember { mutableStateOf("") }
    var isCustomUnit by remember { mutableStateOf(false) }
    var detectedCategory by remember { mutableStateOf<CategoryInfo?>(null) }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var hasChangedImage by remember { mutableStateOf(false) }

    // 🛡️ Estados de validación
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var showValidationWarning by remember { mutableStateOf(false) }

    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // 🎁 Estados para ofertas
    var showOfferBottomSheet by remember { mutableStateOf(false) }
    var offerData by remember { mutableStateOf<OfferData?>(null) }

    // 🆕 Estados para programación de publicación
    var showScheduleBottomSheet by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var showScheduleWithOfferDialog by remember { mutableStateOf(false) }

    // Cargar producto
    LaunchedEffect(productId) {
        viewModel.loadProductById(productId)
    }

    // Inicializar formulario con datos del producto
    LaunchedEffect(product) {
        product?.let { p ->
            name = p.name
            unit = p.unit ?: ""
            description = p.description ?: ""
            price = p.price.toString()

            // Detectar si la unidad es personalizada
            val isStandardUnit = commonUnits.any { it.short == p.unit }
            if (!isStandardUnit && p.unit?.isNotBlank() == true) {
                isCustomUnit = true
                customUnit = p.unit ?: ""
            }

            // Detectar categoría inicial
            val lowerName = name.lowercase().trim()
            if (lowerName.length >= 3) {
                val detected = smartCategories
                    .filter { categoryInfo ->
                        categoryInfo.keywords.any { keyword -> lowerName.contains(keyword) }
                    }
                    .maxByOrNull { categoryInfo ->
                        categoryInfo.keywords.count { keyword -> lowerName.contains(keyword) }
                    }
                detectedCategory = detected
            }

            // Inicializar oferta si existe
            if (p.isOnOffer) {
                offerData = OfferData(
                    offerType = p.offerType ?: "",
                    discountPercentage = p.discountPercentage,
                    validDays = p.offerValidUntil?.let { validUntilStr ->
                        try {
                            val validUntil = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(validUntilStr)
                            val now = java.util.Date()
                            val diffMillis = validUntil.time - now.time
                            (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        } catch (e: Exception) {
                            7 // default
                        }
                    } ?: 7,
                    offerDescription = p.offerDescription ?: "",
                    newPrice = p.price
                )
            }

            // Inicializar fecha programada si existe
            if (p.publicationStatus == "scheduled" && p.scheduledDate != null) {
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = dateFormat.parse(p.scheduledDate)
                    date?.let {
                        val instant = it.toInstant()
                        scheduledDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                } catch (e: Exception) {
                    // ignore parsing errors
                }
            }
        }
    }

    LaunchedEffect(updateProductState) {
        when (updateProductState) {
            is UiState.Success -> {
                showSuccessAnimation = true
                viewModel.resetUpdateProductState()
            }
            is UiState.Error -> {
                viewModel.resetUpdateProductState()
            }
            is UiState.Loading -> {}
            else -> {}
        }
    }

    // 🤖 DETECCIÓN AUTOMÁTICA DE CATEGORÍA
    LaunchedEffect(name) {
        nameError = when {
            name.isBlank() -> null
            name.length < 3 -> "Mínimo 3 caracteres"
            name.length > 50 -> "Máximo 50 caracteres"
            else -> null
        }

        val lowerName = name.lowercase().trim()
        if (lowerName.length >= 3) {
            delay(500)

            val detected = smartCategories
                .filter { categoryInfo ->
                    categoryInfo.keywords.any { keyword -> lowerName.contains(keyword) }
                }
                .maxByOrNull { categoryInfo ->
                    categoryInfo.keywords.count { keyword -> lowerName.contains(keyword) }
                }

            if (detected != null) {
                detectedCategory = detected
            }
        } else {
            detectedCategory = null
        }
    }

    // 🛡️ VALIDACIÓN INTELIGENTE al cambiar unidad
    LaunchedEffect(unit, detectedCategory, name) {
        if (unit.isNotBlank() && detectedCategory != null && !isCustomUnit) {
            validationResult = validateCategoryAndUnit(name, detectedCategory, unit)
            showValidationWarning = validationResult?.isValid == false
        } else {
            validationResult = null
            showValidationWarning = false
        }
    }

    LaunchedEffect(price) {
        priceError = when {
            price.isBlank() -> null
            price.toDoubleOrNull() == null -> "Precio inválido"
            price.toDouble() <= 0 -> "El precio debe ser mayor a 0"
            price.toDouble() > 999999 -> "Precio demasiado alto"
            else -> null
        }
    }

    val isFormValid = name.isNotBlank() &&
            unit.isNotBlank() &&
            price.isNotBlank() &&
            detectedCategory != null &&
            nameError == null &&
            priceError == null &&
            price.toDoubleOrNull() != null &&
            price.toDouble() > 0

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            hasChangedImage = true
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val tempFile = File(context.cacheDir, "product_image_${System.currentTimeMillis()}.jpg")
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    imageFile = tempFile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraUri
            if (uri != null) {
                selectedImageUri = uri
                hasChangedImage = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = File(context.cacheDir, "product_camera_${System.currentTimeMillis()}.jpg")
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        inputStream.close()
                        imageFile = tempFile
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Launcher para permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                photoFile.createNewFile()

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                cameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dialog selección de imagen
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            icon = {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Cambiar Imagen", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Cómo deseas cambiar la foto del producto?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImageSourceDialog = false
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NuColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería", color = NuColors.Primary)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Editar Producto",
                        color = NuColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NuColors.Background)
            )
        },
        containerColor = NuColors.Background,
        floatingActionButton = {
            if (isFormValid && !showSuccessAnimation && updateProductState !is UiState.Loading) {
                FloatingActionButton(
                    onClick = {
                        // 🛡️ FASE 1: Protección contra doble clic (evita actualizaciones duplicadas)
                        executeSafely {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                            product?.let { originalProduct ->
                                println("✏️ ACTUALIZANDO PRODUCTO:")
                                println("   - Nombre: $name")
                                println("   - Unidad: ${if (isCustomUnit) customUnit else unit}")
                                println("   - Categoría detectada: ${detectedCategory?.name}")
                                println("   - Precio: $price")
                                println("   - Imagen: ${if (hasChangedImage) "SÍ (nueva)" else "NO (mantiene anterior)"}")

                                // Calcular campos de oferta
                                val isOnOffer: Boolean
                                val originalPrice: Double?
                                val offerType: String?
                                val discountPercentage: Int?
                                val offerValidUntil: String?
                                val offerDescription: String?

                                if (offerData != null) {
                                    val today = java.util.Date()
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.time = today
                                    calendar.add(java.util.Calendar.DAY_OF_YEAR, offerData!!.validDays)
                                    val validUntil = calendar.time
                                    val validUntilStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(validUntil)

                                    isOnOffer = true
                                    originalPrice = price.toDouble()
                                    offerType = offerData!!.offerType
                                    discountPercentage = offerData!!.discountPercentage
                                    offerValidUntil = validUntilStr
                                    offerDescription = offerData!!.offerDescription
                                } else {
                                    isOnOffer = false
                                    originalPrice = null
                                    offerType = null
                                    discountPercentage = null
                                    offerValidUntil = null
                                    offerDescription = null
                                }

                                // Calcular precio con descuento si hay oferta
                                val finalPrice = if (isOnOffer && discountPercentage != null) {
                                    val discount = price.toDouble() * (discountPercentage / 100.0)
                                    price.toDouble() - discount
                                } else {
                                    price.toDouble()
                                }

                                // Calcular campos de programación
                                val (publicationStatus, scheduledDateStr) = if (scheduledDate != null) {
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                    Pair("scheduled", scheduledDate!!.format(formatter))
                                } else {
                                    Pair("published", null)
                                }


                                val updatedProduct = originalProduct.copy(
                                    name = name.trim(),
                                    description = description.trim().ifBlank { null },
                                    price = finalPrice,
                                    unit = if (isCustomUnit) customUnit.trim() else unit.trim(),
                                    category = detectedCategory?.name ?: "Otros",
                                    isOnOffer = isOnOffer,
                                    originalPrice = originalPrice,
                                    offerType = offerType,
                                    discountPercentage = discountPercentage,
                                    offerValidUntil = offerValidUntil,
                                    offerDescription = offerDescription,
                                    publicationStatus = publicationStatus,
                                    scheduledDate = scheduledDateStr
                                )

                                val fileToUpload = if (hasChangedImage) imageFile else null
                                viewModel.updateProductWithImage(updatedProduct, fileToUpload, businessId)
                            }
                        }
                    },
                    containerColor = NuColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar")
                }
            }
        }
    ) { paddingValues ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NuColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 📸 Selector de imagen mejorado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foto del producto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (selectedImageUri != null || product?.imageUrl != null) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(
                                            NuColors.Primary.copy(alpha = 0.05f),
                                            NuColors.Primary.copy(alpha = 0.02f)
                                        )
                                    )
                                )
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = if (selectedImageUri != null || product?.imageUrl != null) Color.Transparent
                            else NuColors.Primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            showImageSourceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imageToShow = selectedImageUri ?: product?.imageUrl

                    if (imageToShow != null) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(imageToShow)
                                .crossfade(true)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context), // 🖼️ FASE 1: Caché optimizada de imágenes
                            contentDescription = "Producto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = null,
                            error = painterResource(android.R.drawable.ic_menu_gallery)
                        )
                        // Botón para cambiar imagen
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NuColors.Primary)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    showImageSourceDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Cambiar foto",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(NuColors.Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = NuColors.Primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Agregar foto del producto",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NuColors.TextPrimary
                                )
                                Text(
                                    "Toca para seleccionar",
                                    fontSize = 13.sp,
                                    color = NuColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // ✏️ Nombre del producto
                MinimalistTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del producto *",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth(),
                    onImeAction = {
                        focusManager.clearFocus()
                    }
                )
                if (nameError != null) {
                    Text(
                        nameError!!,
                        color = NuColors.Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // ⚠️ Error si no hay categoría detectada
                if (detectedCategory == null && name.length >= 3) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NuColors.Error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NuColors.Error)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = NuColors.Error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "No se pudo detectar la categoría. Escribe un nombre más específico (ej. 'Leche', 'Arroz', 'Tomate')",
                                fontSize = 13.sp,
                                color = NuColors.Error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 🤖 CATEGORÍA DETECTADA AUTOMÁTICAMENTE
                AnimatedVisibility(
                    visible = detectedCategory != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categoría detectada",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuColors.TextPrimary
                            )

                            // Indicador de confianza
                            detectedCategory?.let { category ->
                                val confidence = validationResult?.confidence ?: 1.0f
                                Surface(
                                    color = when {
                                        confidence > 0.7f -> NuColors.Success.copy(alpha = 0.1f)
                                        confidence > 0.4f -> Color(0xFFFFA726).copy(alpha = 0.1f)
                                        else -> NuColors.Error.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            confidence > 0.7f -> NuColors.Success.copy(alpha = 0.3f)
                                            confidence > 0.4f -> Color(0xFFFFA726).copy(alpha = 0.3f)
                                            else -> NuColors.Error.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            when {
                                                confidence > 0.7f -> Icons.Default.CheckCircle
                                                confidence > 0.4f -> Icons.Default.Info
                                                else -> Icons.Default.Warning
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                confidence > 0.7f -> NuColors.Success
                                                confidence > 0.4f -> Color(0xFFFFA726)
                                                else -> NuColors.Error
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            when {
                                                confidence > 0.7f -> "Alta precisión"
                                                confidence > 0.4f -> "Media precisión"
                                                else -> "Baja precisión"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                confidence > 0.7f -> NuColors.Success
                                                confidence > 0.4f -> Color(0xFFFFA726)
                                                else -> NuColors.Error
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Card de la categoría detectada
                        detectedCategory?.let { category ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = category.color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(2.dp, category.color)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(category.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = category.icon,
                                            fontSize = 32.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            color = category.color,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Unidades sugeridas: ${category.relatedUnits.take(3).joinToString(", ")}",
                                            color = category.color.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = category.color,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ⚠️ Warning de validación
                AnimatedVisibility(
                    visible = showValidationWarning && validationResult?.message != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFA726))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = validationResult?.message ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 📏 UNIDAD DE MEDIDA - Solo muestra unidades válidas para la categoría
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Unidad de medida",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )

                    // Filtrar unidades según el producto ESPECÍFICO o categoría
                    val lowerName = name.lowercase().trim()
                    val specificUnits = productSpecificUnits.entries.firstOrNull { (keyword, _) ->
                        lowerName.contains(keyword)
                    }?.value

                    val validUnits = if (specificUnits != null) {
                        // Si hay unidades específicas para este producto, mostrar SOLO esas
                        commonUnits.filter { unitOption ->
                            specificUnits.contains(unitOption.short) || unitOption.short == "Otro"
                        }
                    } else {
                        // Si no, usar las de la categoría
                        detectedCategory?.let { category ->
                            commonUnits.filter { unitOption ->
                                category.relatedUnits.contains(unitOption.short) || unitOption.short == "Otro"
                            }
                        } ?: commonUnits
                    }

                    ResponsiveGrid(
                        items = validUnits,
                        columns = 4,
                        horizontalSpacing = 10.dp,
                        verticalSpacing = 10.dp
                    ) { unitOption ->
                        val isSelected = if (isCustomUnit) {
                            unitOption.short == "Otro"
                        } else {
                            unitOption.short == unit
                        }

                        UnitCard(
                            unitOption = unitOption,
                            isSelected = isSelected,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (unitOption.short == "Otro") {
                                    isCustomUnit = true
                                    unit = customUnit
                                    coroutineScope.launch {
                                        delay(100)
                                        customUnitFocusRequester.requestFocus()
                                    }
                                } else {
                                    isCustomUnit = false
                                    unit = unitOption.short
                                }
                            }
                        )
                    }

                    // Campo personalizado para "Otro"
                    AnimatedVisibility(
                        visible = isCustomUnit,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MinimalistTextField(
                            value = customUnit,
                            onValueChange = {
                                customUnit = it
                                unit = it
                            },
                            label = "Escribe tu unidad (ej. Docena, Rollo)",
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            focusRequester = customUnitFocusRequester,
                            onImeAction = {
                                priceFocusRequester.requestFocus()
                            }
                        )
                    }
                }

                // 💰 Precio
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Precio",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.Primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BasicTextField(
                            value = price,
                            onValueChange = { price = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(priceFocusRequester),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    descriptionFocusRequester.requestFocus()
                                    coroutineScope.launch {
                                        delay(100)
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(NuColors.Primary),
                            decorationBox = { innerTextField ->
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (price.isBlank()) {
                                            Text(
                                                "0.00",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                    HorizontalDivider(
                                        thickness = 2.dp,
                                        color = Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        )
                    }
                    if (priceError != null) {
                        Text(
                            priceError!!,
                            color = NuColors.Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                // 📝 Descripción (opcional)
                MinimalistTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descripción (opcional)",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = descriptionFocusRequester,
                    onImeAction = {
                        focusManager.clearFocus()
                    }
                )

                // 📅 Fecha de actualización
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fecha de actualización",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Surface(
                        color = NuColors.Surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NuColors.Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = NuColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product?.updatedAt?.let { dateStr ->
                                        try {
                                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            val date = inputFormat.parse(dateStr)
                                            date?.let { outputFormat.format(it) } ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                        } catch (e: Exception) {
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                        }
                                    } ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NuColors.TextPrimary
                                )
                                Text(
                                    text = "Se actualizará al guardar",
                                    fontSize = 13.sp,
                                    color = NuColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // 🎁 Sección de Ofertas
                AnimatedVisibility(
                    visible = offerData == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                showOfferBottomSheet = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = Color(0xFFFF6B6B).copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Agregar Oferta",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B6B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agrega descuentos o promociones",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF6B6B).copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Preview de la oferta (si existe)
                AnimatedVisibility(
                    visible = offerData != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    offerData?.let { offer ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA726))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vista previa de la oferta",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFA726)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            offerData = null
                                        },
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFFF6B6B)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFFF6B6B))
                                    ) {
                                        Text("Quitar Oferta", fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFFFA726).copy(alpha = 0.3f))
                                Text(
                                    text = "• Tipo: ${offer.offerType}",
                                    fontSize = 14.sp,
                                    color = NuColors.TextPrimary
                                )
                                offer.discountPercentage?.let { discount ->
                                    Text(
                                        text = "• Descuento: $discount%",
                                        fontSize = 14.sp,
                                        color = NuColors.TextPrimary
                                    )
                                }
                                Text(
                                    text = "• Duración: ${offer.validDays} días",
                                    fontSize = 14.sp,
                                    color = NuColors.TextPrimary
                                )
                                if (offer.offerDescription.isNotBlank()) {
                                    Text(
                                        text = "• ${offer.offerDescription}",
                                        fontSize = 14.sp,
                                        color = NuColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Loading/Success overlay
        NuLoadingSuccessView(
            isLoading = updateProductState is UiState.Loading,
            isSuccess = showSuccessAnimation,
            loadingMessage = "Actualizando producto...",
            successMessage = "¡Producto actualizado!",
            onComplete = {
                onNavigateBack()
            }
        )
    }

    // 🎁 Bottom Sheet de Ofertas
    if (showOfferBottomSheet) {
        OfferBottomSheet(
            productName = name,
            productPrice = price,
            onApplyOffer = { newOfferData ->
                offerData = newOfferData
                showOfferBottomSheet = false
            },
            onDismiss = { showOfferBottomSheet = false }
        )
    }
}

// 🎯 Grid Responsivo Genérico
@Composable
private fun <T> ResponsiveGrid(
    items: List<T>,
    columns: Int,
    horizontalSpacing: androidx.compose.ui.unit.Dp,
    verticalSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                // Rellenar espacios vacíos
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// 📦 Card de Unidad MEJORADA - Texto más visible
@Composable
private fun UnitCard(
    unitOption: UnitOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NuColors.Primary else Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) NuColors.Primary.copy(alpha = 0.1f) else NuColors.Surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji más pequeño para dar espacio
            Text(
                text = unitOption.emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Texto corto (Kg, Lt, etc.)
            Text(
                text = unitOption.short,
                color = if (isSelected) NuColors.Primary else NuColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Texto largo (Kilogramo, Litro) - MÁS VISIBLE
            Text(
                text = unitOption.long,
                color = if (isSelected) NuColors.Primary.copy(alpha = 0.8f) else NuColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 11.sp
            )
        }
    }
}

// ✏️ TextField Minimalista
@Composable
private fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onImeAction: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester)
                    else Modifier
                ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            singleLine = singleLine,
            cursorBrush = SolidColor(NuColors.Primary),
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                    HorizontalDivider(
                        thickness = 2.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                }
            }
        )
    }
}
