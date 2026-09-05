package com.ecomap.socio.presentation.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.socio.data.model.BusinessType
import com.ecomap.socio.utils.ImageConfig
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.viewmodel.ProfileViewModel
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.UiState
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToForgotPassword: (String) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    businessViewModel: BusinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val business by viewModel.business.collectAsState()
    val userState by viewModel.userState.collectAsState()
    val businessState by viewModel.businessState.collectAsState()
    val signOutState by viewModel.signOutState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditBusinessDialog by remember { mutableStateOf(false) }
    var showPasswordConfirmationDialog by remember { mutableStateOf(false) }
    var showBusinessPhotoDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    val uploadAvatarState by viewModel.uploadAvatarState.collectAsState()

    // Declarar cameraLauncher PRIMERO (antes de cameraPermissionLauncher)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // La foto se guardó en el URI temporal
            val file = File(context.cacheDir, "camera_photo.jpg")
            if (file.exists() && business != null) {
                // Subir foto del negocio (no del usuario)
                businessViewModel.uploadBusinessAvatar(file, business!!.id)
            }
        }
    }

    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso otorgado, lanzar cámara
            val photoFile = File(context.cacheDir, "camera_photo.jpg")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        } else {
            android.widget.Toast.makeText(
                context,
                "Permiso de cámara denegado",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (business != null) {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "business_avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                // Subir foto del negocio (no del usuario)
                businessViewModel.uploadBusinessAvatar(file, business!!.id)
            }
        }
    }

    // Observar el estado de subida de avatar
    LaunchedEffect(uploadAvatarState) {
        when (uploadAvatarState) {
            is UiState.Success -> {
                println("✅ Avatar actualizado exitosamente en la UI")
                showImagePickerDialog = false
            }
            is UiState.Error -> {
                val error = (uploadAvatarState as UiState.Error).message
                println("❌ Error al subir avatar: $error")
                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Recargar perfil automáticamente cuando la pantalla se vuelve visible
    // Esto asegura que el estado PRO se actualice en tiempo real después del pago
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadUserProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(signOutState) {
        when (signOutState) {
            is UiState.Success -> {
                BiometricHelper.clearBiometricCredentials(context)
                onSignOut()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = Color(0xFF1F1F1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                userState is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userState as UiState.Error).message,
                            fontSize = 14.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        // Header con avatar
                        ModernProfileHeader(
                            avatarUrl = business?.avatarUrl ?: "",
                            fullName = user?.fullName ?: "",
                            email = user?.email ?: "",
                            onChangeAvatar = { showBusinessPhotoDialog = true }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tarjeta de suscripción
                        user?.let { currentUser ->
                            AnimatedPremiumCard(
                                isPro = currentUser.isPro,
                                onUpgrade = onNavigateToUpgrade
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Información Personal
                        SectionHeader("INFORMACIÓN PERSONAL")

                        ModernProfileItem(
                            icon = Icons.Default.Person,
                            label = "Nombre completo",
                            value = user?.fullName ?: "",
                            onClick = { showEditProfileDialog = true }
                        )

                        ModernProfileItem(
                            icon = Icons.Default.Email,
                            label = "Correo electrónico",
                            value = user?.email ?: "",
                            onClick = null
                        )

                        ModernProfileItem(
                            icon = Icons.Default.Lock,
                            label = "Contraseña",
                            value = "••••••••",
                            onClick = { showPasswordConfirmationDialog = true }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Mi Negocio
                        business?.let { biz ->
                            SectionHeader("MI NEGOCIO")

                            ModernProfileItem(
                                icon = Icons.Default.Store,
                                label = "Nombre del negocio",
                                value = biz.businessName,
                                onClick = { showEditBusinessDialog = true }
                            )

                            ModernProfileItem(
                                icon = Icons.Default.Category,
                                label = "Tipo de negocio",
                                value = BusinessType.fromString(biz.businessType).displayName,
                                onClick = null
                            )

                            ModernProfileItem(
                                icon = Icons.Default.LocationOn,
                                label = "Dirección",
                                value = biz.address,
                                onClick = { showEditBusinessDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Cerrar sesión
                        SectionHeader("CUENTA")

                        SignOutButton(
                            isLoading = signOutState is UiState.Loading,
                            onClick = { viewModel.signOut() }
                        )
                    }
                }
            }

            NuLoadingStateView(
                isLoading = userState is UiState.Loading || businessState is UiState.Loading,
                message = "Cargando perfil..."
            )
        }
    }

    // Dialogs
    if (showEditProfileDialog && user != null) {
        ModernEditDialog(
            title = "Editar Perfil",
            currentValue = user!!.fullName,
            label = "Nombre Completo",
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { newName ->
                viewModel.updateUserProfile(newName)
                showEditProfileDialog = false
            }
        )
    }

    if (showEditBusinessDialog && business != null) {
        EditBusinessDialog(
            business = business!!,
            onDismiss = { showEditBusinessDialog = false },
            onConfirm = { updatedBusiness ->
                viewModel.updateBusiness(updatedBusiness)
                showEditBusinessDialog = false
            }
        )
    }

    if (showPasswordConfirmationDialog && user != null) {
        ModernPasswordChangeDialog(
            onDismiss = { showPasswordConfirmationDialog = false },
            onConfirm = {
                showPasswordConfirmationDialog = false
                onNavigateToForgotPassword(user!!.email)
            }
        )
    }

    if (showBusinessPhotoDialog) {
        BusinessPhotoMotivationalDialog(
            onDismiss = { showBusinessPhotoDialog = false },
            onConfirm = {
                showBusinessPhotoDialog = false
                showImagePickerDialog = true
            }
        )
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onCamera = {
                showImagePickerDialog = false // Cerrar modal
                // Solicitar permiso de cámara primero
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showImagePickerDialog = false // Cerrar modal
                galleryLauncher.launch("image/*")
            }
        )
    }
}

@Composable
private fun ModernProfileHeader(
    avatarUrl: String,
    fullName: String,
    email: String,
    onChangeAvatar: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .clickable(onClick = onChangeAvatar)
            ) {
                AsyncImage(
                    model = avatarUrl,
                    imageLoader = ImageConfig.getImageLoader(context),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1F1F))
                    .clickable(onClick = onChangeAvatar)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Cambiar avatar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = fullName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun AnimatedPremiumCard(
    isPro: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) Color(0xFFFFF9E6) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isPro) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA000)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPro) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFFF9E6),
                                                Color(0xFFFFE8CC)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFF5F5F5),
                                                Color(0xFFE0E0E0)
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPro) Icons.Default.Star else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isPro) Color(0xFFFFA000) else Color(0xFF666666),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isPro) "Plan PRO" else "Plan Básico",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPro) Color(0xFFFFA000) else Color(0xFF1F1F1F)
                            )
                            Text(
                                text = if (isPro) "Acceso completo" else "Funciones limitadas",
                                fontSize = 13.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    if (!isPro) {
                        Button(
                            onClick = onUpgrade,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F1F1F)
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "Mejorar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(20.dp))

                if (!isPro) {
                    ModernFeatureRow(
                        icon = Icons.Default.CheckCircle,
                        text = "Hasta 5 productos",
                        isActive = true,
                        isPro = false
                    )
                    ModernFeatureRow(
                        icon = Icons.Default.Close,
                        text = "Sin programación",
                        isActive = false,
                        isPro = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF1F1F1F),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Desbloquear funciones",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }
                } else {
                    ModernFeatureRow(
                        icon = Icons.Default.CheckCircle,
                        text = "Productos ilimitados",
                        isActive = true,
                        isPro = true
                    )
                    ModernFeatureRow(
                        icon = Icons.Default.CheckCircle,
                        text = "Programación",
                        isActive = true,
                        isPro = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Gracias por tu apoyo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFA000)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isActive: Boolean,
    isPro: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isPro && isActive -> Color(0xFFFFF9E6)
                        isActive -> Color(0xFFE8F5E9)
                        else -> Color(0xFFF5F5F5)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    isPro && isActive -> Color(0xFFFFA000)
                    isActive -> Color(0xFF4CAF50)
                    else -> Color(0xFF999999)
                },
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isActive) Color(0xFF1F1F1F) else Color(0xFF999999),
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF999999),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun ModernProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F1F1F)
                )
            }

            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Editar",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SignOutButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Cerrar sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE53935),
                modifier = Modifier.weight(1f)
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFE53935),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernEditDialog(
    title: String,
    currentValue: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F)
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F1F1F),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun EditBusinessDialog(
    business: com.ecomap.socio.data.model.Business,
    onDismiss: () -> Unit,
    onConfirm: (com.ecomap.socio.data.model.Business) -> Unit
) {
    var businessName by remember { mutableStateOf(business.businessName) }
    var address by remember { mutableStateOf(business.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Editar Negocio",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(business.copy(businessName = businessName, address = address))
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F))
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ModernPasswordChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                "Cambiar Contraseña",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Te enviaremos un enlace de recuperación a tu correo para cambiar tu contraseña de forma segura.",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun BusinessPhotoMotivationalDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B),
                                Color(0xFFFF8E53)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        title = {
            Text(
                "📸 ¡Atrae más clientes!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1F1F1F)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Una buena foto de tu negocio puede aumentar las visitas hasta un 70%.",
                    fontSize = 15.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color(0xFFE0E0E0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Tu foto será revisada para garantizar la calidad del servicio.",
                        fontSize = 13.sp,
                        color = Color(0xFF2196F3),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir foto", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ahora no", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Selecciona una opción",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Opción de Cámara
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCamera),
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Tomar foto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                "Usa la cámara del dispositivo",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }

                // Opción de Galería
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onGallery),
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Elegir de galería",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                "Selecciona una foto existente",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = Color(0xFF666666))
            }
        }
    )
}