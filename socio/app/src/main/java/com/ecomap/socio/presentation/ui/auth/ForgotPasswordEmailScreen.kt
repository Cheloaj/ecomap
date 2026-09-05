package com.ecomap.socio.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState

/**
 * Pantalla de validación de identidad para resetear contraseña
 * Muestra el email del usuario y envía código de verificación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordEmailScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToCodeVerification: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val emailCheckState by viewModel.emailCheckState.collectAsState()
    val resetPasswordState by viewModel.resendCodeState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Observar estado de verificación de email
    LaunchedEffect(emailCheckState) {
        when (emailCheckState) {
            is UiState.Success -> {
                val exists = (emailCheckState as UiState.Success).data
                if (exists) {
                    // Email existe → enviar código
                    println("✅ ForgotPasswordEmail: Email existe, enviando código...")
                    viewModel.resendVerificationCode(email)
                } else {
                    // Email NO existe → mostrar error
                    println("❌ ForgotPasswordEmail: Email no registrado")
                    errorMessage = "Este correo no está registrado en el sistema"
                }
                viewModel.resetEmailCheckState()
            }
            is UiState.Error -> {
                val error = (emailCheckState as UiState.Error).message
                println("❌ ForgotPasswordEmail: Error al verificar email: $error")
                errorMessage = "Error al verificar el correo"
                viewModel.resetEmailCheckState()
            }
            else -> {}
        }
    }

    // Observar estado de envío de código
    LaunchedEffect(resetPasswordState) {
        when (resetPasswordState) {
            is UiState.Success -> {
                println("✅ ForgotPasswordEmail: Código enviado exitosamente")
                onNavigateToCodeVerification(email)
                viewModel.resetResendCodeState()
            }
            is UiState.Error -> {
                val error = (resetPasswordState as UiState.Error).message
                println("❌ ForgotPasswordEmail: Error al enviar código: $error")
                errorMessage = "Error al enviar el código de verificación"
                viewModel.resetResendCodeState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Contenido superior
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Título
                Text(
                    text = "Necesitamos validar tu identidad antes de continuar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subtítulo con mensaje de seguridad
                Text(
                    text = "Hacemos esto como una medida de seguridad para proteger tu cuenta",
                    fontSize = 16.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mostrar el email del usuario
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NuColors.Primary.copy(alpha = 0.05f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enviaremos un código de verificación a:",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = email,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.Primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ✅ Mostrar error si el email no está registrado
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = NuColors.Error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = NuColors.Error,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Botón inferior
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                NuPrimaryButton(
                    text = "Continuar",
                    onClick = {
                        // ✅ Primero verificar si el email existe
                        println("📧 ForgotPasswordEmail: Verificando si email existe: $email")
                        errorMessage = null // Limpiar error anterior
                        viewModel.checkEmailExists(email)
                    },
                    enabled = emailCheckState !is UiState.Loading && resetPasswordState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Loading indicator
    if (emailCheckState is UiState.Loading || resetPasswordState is UiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NuColors.Primary)
        }
    }
}
