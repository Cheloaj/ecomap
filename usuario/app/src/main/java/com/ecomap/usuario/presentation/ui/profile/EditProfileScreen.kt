package com.ecomap.usuario.presentation.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.presentation.viewmodel.UserDataUiState
import com.ecomap.usuario.presentation.viewmodel.UserDataViewModel
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.ImageConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserDataViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(userPreferences) {
        userPreferences?.let {
            displayName = it.displayName ?: ""
        }
    }

    LaunchedEffect(operationState) {
        when (operationState) {
            is UserDataUiState.Success -> {
                showSuccessDialog = true
                viewModel.resetOperationState()
            }
            is UserDataUiState.Error -> {
                errorMessage = (operationState as UserDataUiState.Error).message
                viewModel.resetOperationState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Perfil",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F1F1F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1F1F1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar mejorado
                ModernAvatarSection(
                    currentAvatarUrl = userPreferences?.avatarUrl,
                    selectedImageUri = selectedImageUri,
                    onSelectImage = { imagePickerLauncher.launch("image/*") }
                )

                // Card de información
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Información Personal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )

                        // Campo de nombre mejorado
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Nombre para mostrar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                placeholder = { Text("Ingresa tu nombre", color = Color(0xFFCCCCCC)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF999999),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F1F1F),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = Color(0xFF1F1F1F)
                                ),
                                singleLine = true
                            )
                        }

                        // Botón guardar mejorado
                        Button(
                            onClick = {
                                if (selectedImageUri != null) {
                                    viewModel.uploadAvatar(selectedImageUri!!)
                                }

                                if (displayName.isNotBlank() && displayName != userPreferences?.displayName) {
                                    viewModel.updateDisplayName(displayName)
                                }

                                if (selectedImageUri == null && displayName == userPreferences?.displayName) {
                                    errorMessage = "No hay cambios para guardar"
                                }

                                if (displayName.isBlank()) {
                                    errorMessage = "El nombre no puede estar vacío"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F1F1F)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = operationState !is UserDataUiState.Loading
                        ) {
                            if (operationState is UserDataUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Guardar Cambios",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Info card mejorado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Este nombre será visible en tu perfil público",
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Snackbar mejorado
            AnimatedVisibility(
                visible = errorMessage.isNotBlank(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF1F1F1F),
                    shape = RoundedCornerShape(12.dp),
                    action = {
                        TextButton(onClick = { errorMessage = "" }) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                ) {
                    Text(errorMessage, color = Color.White)
                }
            }
        }
    }

    // Diálogo de éxito mejorado
    if (showSuccessDialog) {
        ModernSuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun ModernAvatarSection(
    currentAvatarUrl: String?,
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar con efecto hover
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Avatar principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .clickable(onClick = onSelectImage)
            ) {
                when {
                    selectedImageUri != null -> {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !currentAvatarUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = currentAvatarUrl,
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
            }

            // Botón de cámara flotante
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1F1F))
                    .clickable(onClick = onSelectImage)
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Texto indicativo
        Text(
            text = "Toca para cambiar tu foto",
            fontSize = 13.sp,
            color = Color(0xFF999999)
        )
    }
}

@Composable
private fun ModernSuccessDialog(
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        kotlinx.coroutines.delay(200)
        checkScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                "¡Perfil actualizado!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1F1F1F),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Tus cambios se han guardado correctamente",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Aceptar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    )
}