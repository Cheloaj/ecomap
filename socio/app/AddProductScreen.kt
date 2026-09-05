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
import androidx.compose.foundation.ExperimentalFoundationApi // ✅ IMPORTAR
import androidx.compose.foundation.relocation.BringIntoViewRequester // ✅ IMPORTAR
import androidx.compose.foundation.relocation.bringIntoViewRequester // ✅ IMPORTAR
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView
import com.ecomap.socio.presentation.viewmodel.ProductViewModel
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/**
 * 🎨 AddProductScreen - Versión 3 (CORRECTA)
 * ✅ SCROLL AUTOMÁTICO: Usa BringIntoViewRequester.
 * - Funciona en CUALQUIER tamaño de pantalla y CUALQUIER teclado.
 * - No usa valores fijos (como 300.dp).
 * - El "Enter" funciona perfectamente.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) // ✅ AÑADIR OptIn
@Composable
fun AddProductScreen(
    businessId: String,
    onNavigateBack: () -> Unit,
    onProductCreated: () -> Unit,
    viewModel: ProductViewModel,
    businessViewModel: BusinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- ✅ INICIO DE LA SOLUCIÓN ---
    // 1. Focus Requesters (Ya los tenías)
    val nameFocusRequester = remember { FocusRequester() }
    val customUnitFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    // 2. BringIntoView Requesters (La clave)
    val nameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val customUnitBringIntoViewRequester = remember { BringIntoViewRequester() }
    val priceBringIntoViewRequester = remember { BringIntoViewRequester() }
    val descriptionBringIntoViewRequester = remember { BringIntoViewRequester() }
    // --- ✅ FIN DE LA SOLUCIÓN ---


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

    // 🛡️ Estados de validación
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var showValidationWarning by remember { mutableStateOf(false) }

    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // 🆕 Estados para programación de publicación (Pro feature)
    var showScheduleBottomSheet by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf<LocalDate?>(null) }
    val isProUser by businessViewModel.isProUser.collectAsState()
    val isProStatusChecked by businessViewModel.isProStatusChecked.collectAsState()
    val business by businessViewModel.business.collectAsState()

    // ✅ Obtener horario de apertura y cierre del negocio
    val businessHours = remember(business) {
        business?.let { b ->
            try {
                val opHours = kotlinx.serialization.json.Json.decodeFromString<com.ecomap.socio.data.model.OperatingHours>(b.operatingHours)

                val allOpenTimes = listOfNotNull(
                    opHours.monday.shifts.firstOrNull()?.openTime?.takeIf { opHours.monday.isOpen },
                    opHours.tuesday.shifts.firstOrNull()?.openTime?.takeIf { opHours.tuesday.isOpen },
                    opHours.wednesday.shifts.firstOrNull()?.openTime?.takeIf { opHours.wednesday.isOpen },
                    opHours.thursday.shifts.firstOrNull()?.openTime?.takeIf { opHours.thursday.isOpen },
                    opHours.friday.shifts.firstOrNull()?.openTime?.takeIf { opHours.friday.isOpen },
                    opHours.saturday.shifts.firstOrNull()?.openTime?.takeIf { opHours.saturday.isOpen },
                    opHours.sunday.shifts.firstOrNull()?.openTime?.takeIf { opHours.sunday.isOpen }
                )

                val allCloseTimes = listOfNotNull(
                    opHours.monday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.monday.isOpen },
                    opHours.tuesday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.tuesday.isOpen },
                    opHours.wednesday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.wednesday.isOpen },
                    opHours.thursday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.thursday.isOpen },
                    opHours.friday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.friday.isOpen },
                    opHours.saturday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.saturday.isOpen },
                    opHours.sunday.shifts.firstOrNull()?.closeTime?.takeIf { opHours.sunday.isOpen }
                )

                Pair(
                    allOpenTimes.minOrNull() ?: "08:00",
                    allCloseTimes.maxOrNull() ?: "20:00"
                )
            } catch (e: Exception) {
                Pair("08:00", "20:00")
            }
        } ?: Pair("08:00", "20:00")
    }

    val currentDate = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    val createProductState by viewModel.createProductState.collectAsState()

    // ✅ Verificar estado Pro al cargar la pantalla
    LaunchedEffect(Unit) {
        businessViewModel.checkProStatus()
    }

    LaunchedEffect(createProductState) {
        when (createProductState) {
            is UiState.Success -> {
                showSuccessAnimation = true
                viewModel.resetCreateProductState()
            }
            is UiState.Error -> {
                viewModel.resetCreateProductState()
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

    // 🛡️ VALIDACIÓN INTELIGENTE
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
            selectedImageUri != null &&
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
                Text("Seleccionar Imagen", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Cómo deseas agregar la foto del producto?")
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agregar Producto",
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
            if (isFormValid && !showSuccessAnimation && createProductState !is UiState.Loading) {
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                        val newProduct = Product(
                            businessId = businessId,
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            price = price.toDouble(),
                            unit = if (isCustomUnit) customUnit.trim() else unit.trim(),
                            category = detectedCategory?.name ?: "Otros",
                            stock = null,
                            isAvailable = true,
                            publicationStatus = if (scheduledDate != null) "scheduled" else "published",
                            scheduledDate = scheduledDate?.toString()
                        )

                        viewModel.createProductWithImage(newProduct, imageFile, businessId)
                    },
                    containerColor = NuColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState) // <-- Deja que este haga el scroll
                .imePadding() // <-- Deja que este ajuste el tamaño
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 📸 Selector de imagen
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
                Text(
                    text = "*Obligatorio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.Error
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (selectedImageUri != null) {
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
                        color = if (selectedImageUri != null) Color.Transparent
                        else NuColors.Primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        showImageSourceDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Producto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = null,
                        error = painterResource(android.R.drawable.ic_menu_gallery)
                    )
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

            // ✏️ Nombre del producto - 🎯 CORREGIDO
            MinimalistTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre del producto *",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
                focusRequester = nameFocusRequester,
                bringIntoViewRequester = nameBringIntoViewRequester,
                coroutineScope = coroutineScope,
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
                            text = "No se pudo detectar la categoría. Escribe un nombre más específico",
                            fontSize = 13.sp,
                            color = NuColors.Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 🤖 CATEGORÍA DETECTADA
            AnimatedVisibility(
                visible = detectedCategory != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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
                                    text = "Unidades: ${category.relatedUnits.take(3).joinToString(", ")}",
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

            // 📏 UNIDAD DE MEDIDA
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Unidad de medida",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary
                )

                val lowerName = name.lowercase().trim()
                val specificUnits = productSpecificUnits.entries.firstOrNull { (keyword, _) ->
                    lowerName.contains(keyword)
                }?.value

                val validUnits = if (specificUnits != null) {
                    commonUnits.filter { unitOption ->
                        specificUnits.contains(unitOption.short) || unitOption.short == "Otro"
                    }
                } else {
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

                // Campo personalizado para "Otro" - 🎯 CORREGIDO
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
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        focusRequester = customUnitFocusRequester,
                        bringIntoViewRequester = customUnitBringIntoViewRequester,
                        coroutineScope = coroutineScope,
                        onImeAction = {
                            priceFocusRequester.requestFocus()
                        }
                    )
                }
            }

            // 💰 Precio - 🎯 CORREGIDO
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
                            .focusRequester(priceFocusRequester)
                            .bringIntoViewRequester(priceBringIntoViewRequester) // ✅ AÑADIR
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        delay(100) // Dar tiempo al teclado
                                        priceBringIntoViewRequester.bringIntoView() // ✅ LLAMAR
                                    }
                                }
                            },
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
                                // ✅ SIMPLE: Solo pide foco.
                                // El scroll lo manejará el "onFocusChanged" de Descripción.
                                descriptionFocusRequester.requestFocus()
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

            // 📝 Descripción - 🎯 CORREGIDO
            MinimalistTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                focusRequester = descriptionFocusRequester,
                bringIntoViewRequester = descriptionBringIntoViewRequester,
                coroutineScope = coroutineScope,
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
                                text = currentDate,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NuColors.TextPrimary
                            )
                            Text(
                                text = "Hoy",
                                fontSize = 13.sp,
                                color = NuColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // 🆕 PRO FEATURE
            if (isProUser) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                            if (!isFormValid) {
                                coroutineScope.launch {
                                    val missingFields = mutableListOf<String>()

                                    if (name.isBlank()) missingFields.add("Nombre")
                                    if (selectedImageUri == null) missingFields.add("Foto")
                                    if (detectedCategory == null) missingFields.add("Categoría")
                                    if (unit.isBlank()) missingFields.add("Unidad")
                                    if (price.isBlank() || price.toDoubleOrNull() == null || price.toDouble() <= 0) {
                                        missingFields.add("Precio")
                                    }

                                    val message = if (missingFields.isNotEmpty()) {
                                        "Completa los campos: ${missingFields.joinToString(", ")}"
                                    } else {
                                        "Completa todos los campos requeridos"
                                    }

                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                showScheduleBottomSheet = true
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            )
                        )
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
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⭐ Programar publicación",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Publica este producto en una fecha futura",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // ANIMACIONES
        if (scheduledDate == null) {
            NuLoadingSuccessView(
                isLoading = createP`RoductState is UiState.Loading,
            isSuccess = showSuccessAnimation,
            loadingMessage = "Guardando producto...",
            successMessage = "¡Producto agregado!",
            onComplete = {
                onProductCreated()
                onNavigateBack()
            }
            )
        }

        if (scheduledDate != null) {
            NuLoadingSuccessView(
                isLoading = createProductState is UiState.Loading,
                isSuccess = showSuccessAnimation,
                loadingMessage = "Programando publicación...",
                successMessage = "¡Producto programado!",
                onComplete = {
                    onProductCreated()
                    onNavigateBack()
                }
            )
        }
    }

    // BottomSheet
    if (showScheduleBottomSheet) {
        ScheduleBottomSheet(
            productName = name.trim(),
            productPrice = price,
            productUnit = if (isCustomUnit) customUnit.trim() else unit.trim(),
            productImageUri = selectedImageUri,
            businessOpeningTime = businessHours.first,
            businessClosingTime = businessHours.second,
            onSchedule = { selectedScheduleDate, selectedScheduleTime ->
                val newProduct = Product(
                    businessId = businessId,
                    name = name.trim(),
                    description = description.trim().ifBlank { null },
                    price = price.toDouble(),
                    unit = if (isCustomUnit) customUnit.trim() else unit.trim(),
                    category = detectedCategory?.name ?: "Otros",
                    stock = null,
                    isAvailable = true,
                    publicationStatus = "scheduled",
                    scheduledDate = selectedScheduleDate.toString()
                )

                viewModel.createProductWithImage(newProduct, imageFile, businessId)
                scheduledDate = selectedScheduleDate
            },
            onPublishNow = {
                val newProduct = Product(
                    businessId = businessId,
                    name = name.trim(),
                    description = description.trim().ifBlank { null },
                    price = price.toDouble(),
                    unit = if (isCustomUnit) customUnit.trim() else unit.trim(),
                    category = detectedCategory?.name ?: "Otros",
                    stock = null,
                    isAvailable = true,
                    publicationStatus = "published"
                )

                viewModel.createProductWithImage(newProduct, imageFile, businessId)
            },
            onDismiss = {
                showScheduleBottomSheet = false
            }
        )
    }
}

// 🎯 COMPONENTE CLAVE: TextField con Scroll Automático
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester?,
    bringIntoViewRequester: BringIntoViewRequester,
    coroutineScope: CoroutineScope,
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
                .bringIntoViewRequester(bringIntoViewRequester) // ✅ AÑADIR
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(100) // Dar tiempo al teclado
                            bringIntoViewRequester.bringIntoView() // ✅ LLAMAR
                        }
                    }
                }
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

// Grid Responsivo
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
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Card de Unidad
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
            Text(
                text = unitOption.emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unitOption.short,
                color = if (isSelected) NuColors.Primary else NuColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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