package com.ecomap.socio.presentation.ui.onboarding

import androidx.activity.compose.BackHandler
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.rememberAsyncImagePainter
import com.ecomap.socio.data.workers.NotificationWorker
import com.ecomap.socio.presentation.ui.components.NuFloatingActionButton
import com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView
import com.ecomap.socio.presentation.viewmodel.OnboardingViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVerificationScreen(
    userId: String,
    onUploadComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var tempFileToUpload by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val uploadDocumentState by viewModel.uploadDocumentState.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            selectedImageUri = cameraUri
            cameraUri?.let {
                convertUriToFile(context, it) { file ->
                    tempFileToUpload = file
                    showConfirmDialog = true
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraUri = createTempImageUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            convertUriToFile(context, it) { file ->
                tempFileToUpload = file
                showConfirmDialog = true
            }
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        println("🖼️ Permiso de galería: $isGranted")
        if (isGranted) {
            println("✅ Permiso concedido - abriendo galería")
            galleryLauncher.launch("image/*")
        } else {
            println("❌ Permiso de galería denegado")
        }
    }

    LaunchedEffect(uploadDocumentState) {
        if (uploadDocumentState is UiState.Success) {
            println("📄 Documento enviado exitosamente")
            scheduleNotificationWorker(context)
            // ✅ Esperar 3 segundos para que la barra llegue al 100%
            delay(3000)
            showSuccessAnimation = true
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Color.White, // ✅ Fondo blanco (modo claro)
            icon = {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Seleccionar Imagen",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Elige cómo deseas seleccionar la imagen de tu recibo",
                    fontSize = 16.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImageSourceDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cámara", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showImageSourceDialog = false
                        println("📱 Abriendo galería...")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            println("📱 Android 13+ - abriendo galería sin permiso")
                            galleryLauncher.launch("image/*")
                        } else {
                            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                            println("📱 Android 12- - solicitando permiso: $permission")
                            galleryPermissionLauncher.launch(permission)
                        }
                    },
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NuColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería", color = NuColors.Primary)
                }
            }
        )
    }

    // ✅ Solo mostrar modal si NO está subiendo
    if (showConfirmDialog && uploadDocumentState !is UiState.Loading) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedImageUri = null
                tempFileToUpload = null
            },
            containerColor = Color.White, // ✅ Fondo blanco (modo claro)
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(56.dp)
                )
            },
            title = {
                Text(
                    text = "¿Confirmar documento?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Estás seguro de que esta es la imagen correcta de tu recibo de CFE o Agua?",
                        fontSize = 16.sp,
                        color = NuColors.TextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu documento será revisado y recibirás una notificación cuando tu negocio sea verificado.",
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // ✅ CERRAR TECLADO
                        focusManager.clearFocus()

                        showConfirmDialog = false
                        tempFileToUpload?.let { file ->
                            println("📤 Confirmando subida de documento para usuario: $userId")
                            viewModel.uploadVerificationDocument(file, userId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NuColors.Primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Sí, enviar", color = Color.White, fontSize = 16.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedImageUri = null
                        tempFileToUpload = null
                    },
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Cancelar", color = NuColors.Primary, fontSize = 16.sp)
                }
            }
        )
    }

    // ✅ Usar Box en lugar de Crossfade para evitar pantalla negra
    Box(modifier = Modifier.fillMaxSize()) {
        // Pantalla normal (siempre renderizada)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Verificación de Documento",
                            color = NuColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NuColors.Background)
                )
            },
            containerColor = NuColors.Background,
            floatingActionButton = {
                if (selectedImageUri != null && !showConfirmDialog) {
                    val isLoading = uploadDocumentState is UiState.Loading
                    NuFloatingActionButton(
                        onClick = {
                            println("🚀 FAB de DocumentVerification clicado, mostrando diálogo de confirmación")
                            showConfirmDialog = true
                        },
                        enabled = !isLoading,
                        isLoading = isLoading,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Verifica tu Negocio",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary,
                        lineHeight = 42.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sube una foto de tu recibo de CFE o Agua para verificar tu negocio",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = NuColors.TextSecondary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = 2.dp,
                                color = if (selectedImageUri != null) NuColors.Primary else NuColors.Surface,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { showImageSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Documento seleccionado",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = NuColors.Primary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Toca para seleccionar",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NuColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cámara o Galería",
                                    fontSize = 14.sp,
                                    color = NuColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Máximo 5MB • JPG, PNG",
                        fontSize = 13.sp,
                        color = NuColors.TextSecondary.copy(alpha = 0.7f)
                    )
                    AnimatedVisibility(
                        visible = uploadDocumentState is UiState.Error,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = NuColors.Error.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = if (uploadDocumentState is UiState.Error) {
                                    ErrorMessageHelper.getFriendlyMessage(
                                        (uploadDocumentState as UiState.Error).message
                                    )
                                } else "",
                                fontSize = 14.sp,
                                color = NuColors.Error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ✅ Animación encima de la pantalla (como EmailVerificationScreen)
        val shouldShowLoading = uploadDocumentState is UiState.Loading ||
                               (uploadDocumentState is UiState.Success && !showSuccessAnimation)

        // ✅ Bloquear retroceder durante la animación
        BackHandler(enabled = shouldShowLoading || showSuccessAnimation) {
            // No hacer nada - bloquear navegación hacia atrás
        }

        NuLoadingSuccessView(
            isLoading = shouldShowLoading,
            isSuccess = showSuccessAnimation,
            loadingMessage = "Enviando tu foto...",
            successMessage = "¡Foto enviada!",
            loadingDuration = 6000L,
            successDuration = 6000L,
            onComplete = {
                // ✅ Llamada genérica - la pantalla NO sabe a dónde va
                onUploadComplete()
            }
        )
    }
}

private fun createTempImageUri(context: Context): Uri? {
    return try {
        val photoFile = File.createTempFile("verification_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun convertUriToFile(
    context: Context,
    uri: Uri,
    onFileCreated: (File) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "verification_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        onFileCreated(file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun scheduleNotificationWorker(context: Context) {
    try {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        println("✅ Worker de notificaciones activado correctamente")
    } catch (e: Exception) {
        e.printStackTrace()
        println("❌ Error al activar Worker: ${e.message}")
    }
}
