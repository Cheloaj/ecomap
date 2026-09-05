package com.ecomap.usuario.presentation.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.data.model.ProductCategories
import com.ecomap.usuario.data.model.ProductCategory
import com.ecomap.usuario.data.model.MeasurementUnit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// --- TEMA MINIMALISTA MEJORADO ---
private val PrimaryDark = Color(0xFF111111)
private val BackgroundLight = Color(0xFFFBFBFB)
private val SurfaceCard = Color(0xFFFFFFFF)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentOrange = Color(0xFFF59E0B)
private val AccentBlue = Color(0xFF3B82F6)
private val TextSecondary = Color(0xFF6B7280)
private val BorderLight = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReportProductScreen(
    onNavigateBack: () -> Unit,
    productViewModel: com.ecomap.usuario.presentation.viewmodel.ProductViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val uploadState by productViewModel.uploadState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Estados de Formulario
    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var estimatedPrice by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    // Categoría y Unidad
    var detectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var selectedUnit by remember { mutableStateOf<MeasurementUnit?>(null) }

    // Ubicación y GPS
    var userLatitude by remember { mutableStateOf<Double?>(null) }
    var userLongitude by remember { mutableStateOf<Double?>(null) }
    var userAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationAccuracy by remember { mutableStateOf<Float?>(null) }
    var showLowAccuracyWarning by remember { mutableStateOf(false) }

    // Permisos
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )

    // Lógica de detección de categoría
    LaunchedEffect(productName) {
        kotlinx.coroutines.delay(300)
        if (productName.isNotBlank() && productName.length >= 3) {
            try {
                val newCategory = ProductCategories.detectCategory(productName)
                if (newCategory != detectedCategory) {
                    detectedCategory = newCategory
                    selectedUnit = null
                }
            } catch (e: Exception) {
                // Silent catch
            }
        } else {
            detectedCategory = null
            selectedUnit = null
        }
    }

    // Lógica de ubicación
    suspend fun getUserLocation() {
        isLoadingLocation = true
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    locationAccuracy = location.accuracy
                    showLowAccuracyWarning = (location.accuracy > 50f)

                    withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                userAddress = buildString {
                                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                                    if (address.locality != null) append("${address.locality}, ")
                                    if (address.adminArea != null) append(address.adminArea)
                                }
                            }
                        } catch (e: Exception) {
                            userAddress = "Ubicación detectada"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoadingLocation = false
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionState.allPermissionsGranted) {
            getUserLocation()
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    // Limpiar tras éxito
    LaunchedEffect(uploadState) {
        if (uploadState is com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Success) {
            productName = ""
            description = ""
            estimatedPrice = ""
            selectedImageUri = null
            detectedCategory = null
            selectedUnit = null
            kotlinx.coroutines.delay(2000)
            productViewModel.resetUploadState()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = tempCameraUri else tempCameraUri = null
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Publicar",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundLight)
            )
        },
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- FOTO DEL PRODUCTO ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Imagen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryDark,
                            letterSpacing = (-0.3).sp
                        )
                        Surface(
                            color = AccentRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Requerida",
                                fontSize = 11.sp,
                                color = AccentRed,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(selectedImageUri).crossfade(true).build(),
                                imageLoader = ImageConfig.getImageLoader(context),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { showImagePickerDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .background(PrimaryDark.copy(alpha = 0.75f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                .clickable { showImagePickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "Agregar foto",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // --- INFORMACIÓN BÁSICA ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Información",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDark,
                        letterSpacing = (-0.3).sp
                    )

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Nombre del producto", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDark,
                            focusedLabelColor = PrimaryDark,
                            cursorColor = PrimaryDark,
                            unfocusedBorderColor = BorderLight
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = estimatedPrice,
                            onValueChange = { estimatedPrice = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Precio", fontSize = 14.sp) },
                            prefix = { Text("$", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryDark,
                                focusedLabelColor = PrimaryDark,
                                cursorColor = PrimaryDark,
                                unfocusedBorderColor = BorderLight
                            ),
                            singleLine = true
                        )

                        // Preview de unidad seleccionada
                        if (selectedUnit != null) {
                            Surface(
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = AccentGreen.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(selectedUnit!!.emoji, fontSize = 20.sp)
                                    Text(
                                        "/ ${selectedUnit!!.abbreviation}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentGreen
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción (opcional)", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDark,
                            focusedLabelColor = PrimaryDark,
                            cursorColor = PrimaryDark,
                            unfocusedBorderColor = BorderLight
                        ),
                        minLines = 3,
                        maxLines = 4
                    )
                }
            }

            // --- CATEGORÍA Y UNIDADES ---
            AnimatedVisibility(
                visible = detectedCategory != null,
                enter = fadeIn() + expandVertically()
            ) {
                detectedCategory?.let { category ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Categoría",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryDark,
                                    letterSpacing = (-0.3).sp
                                )
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = AccentGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "Detectada",
                                            fontSize = 11.sp,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(category.icon, fontSize = 28.sp)
                                    Column {
                                        Text(
                                            category.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryDark,
                                            letterSpacing = (-0.3).sp
                                        )
                                        Text(
                                            "Selecciona unidad de medida",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Text(
                                "Unidad de Medida",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryDark
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.height(((category.units.size / 4 + 1) * 80).dp),
                                userScrollEnabled = false
                            ) {
                                items(category.units) { unit ->
                                    val isSelected = selectedUnit == unit
                                    Card(
                                        onClick = { selectedUnit = unit },
                                        modifier = Modifier.aspectRatio(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) PrimaryDark else Color(0xFFF9FAFB)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(unit.emoji, fontSize = 22.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                unit.abbreviation,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else PrimaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- ADVERTENCIA GPS ---
            if (showLowAccuracyWarning && locationAccuracy != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GpsOff, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GPS impreciso",
                                fontWeight = FontWeight.SemiBold,
                                color = AccentOrange,
                                fontSize = 13.sp
                            )
                            Text(
                                "±${locationAccuracy?.toInt()}m - Sal al exterior",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { coroutineScope.launch { getUserLocation() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // --- ESTADOS ---
            when (uploadState) {
                is com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(20.dp))
                            Text(
                                (uploadState as com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Error).message,
                                color = AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                is com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Text(
                                (uploadState as com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Success).message,
                                color = AccentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                else -> {}
            }

            // --- BOTÓN PUBLICAR ---
            val canSubmit = productName.isNotBlank() &&
                    estimatedPrice.isNotBlank() &&
                    selectedImageUri != null &&
                    detectedCategory != null &&
                    selectedUnit != null &&
                    userLatitude != null &&
                    uploadState !is com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Loading

            Button(
                onClick = {
                    val price = estimatedPrice.toDoubleOrNull() ?: 0.0
                    val latitude = userLatitude ?: 0.0
                    val longitude = userLongitude ?: 0.0
                    // 🔧 FIX CRÍTICO: Usar abbreviation directamente como unit
                    val unit = selectedUnit?.abbreviation ?: "pza"

                    // Log para debug
                    android.util.Log.d("ReportProduct", "Enviando unit: $unit")

                    productViewModel.uploadProduct(
                        productName,
                        description.ifBlank { null },
                        price,
                        selectedImageUri,
                        latitude,
                        longitude,
                        userAddress,
                        unit // ⬅️ Este es el parámetro crítico
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDark,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE5E7EB),
                    disabledContentColor = TextSecondary
                ),
                enabled = canSubmit
            ) {
                if (uploadState is com.ecomap.usuario.presentation.viewmodel.ProductUploadUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                        Text(
                            "Publicar Producto",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // --- DIÁLOGO DE IMAGEN ---
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Seleccionar imagen",
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryDark,
                    fontSize = 17.sp,
                    letterSpacing = (-0.3).sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showImagePickerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Galería", fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = {
                            if (cameraPermissionState.allPermissionsGranted) {
                                try {
                                    val photoFile = java.io.File(context.cacheDir, "product_${System.currentTimeMillis()}.jpg")
                                    tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraLauncher.launch(tempCameraUri!!)
                                    showImagePickerDialog = false
                                } catch (e: Exception) {
                                    android.util.Log.e("ReportProduct", "Error: ${e.message}")
                                }
                            } else {
                                cameraPermissionState.launchMultiplePermissionRequest()
                                showImagePickerDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cámara", fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) {
                    Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}