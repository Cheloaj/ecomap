package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.NuPrimaryButton
import com.ecomap.usuario.presentation.ui.components.NuSecondaryButton
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de verificación de código para resetear contraseña
 * Usuario ingresa código de 6 dígitos recibido por email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordCodeScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var resendEnabled by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }

    val verificationState by viewModel.verificationState.collectAsState()
    val resendCodeState by viewModel.resendCodeState.collectAsState()

    // Countdown para reenvío
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        resendEnabled = true
    }

    // ✅ Verificar automáticamente cuando se ingresan 6 dígitos
    LaunchedEffect(code) {
        if (code.length == 6 && verificationState !is UiState.Loading) {
            println("🔍 ForgotPasswordCode: Auto-verificando código $code para $email")
            viewModel.verifyEmail(email, code)
        }
    }

    // Observar estado de verificación
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is UiState.Success -> {
                println("✅ ForgotPasswordCode: Código verificado correctamente")
                onNavigateToResetPassword(email, code)
                viewModel.resetVerificationState()
            }
            is UiState.Error -> {
                val errorMessage = (verificationState as UiState.Error).message
                println("❌ ForgotPasswordCode: Error en verificación: $errorMessage")
                codeError = errorMessage
            }
            else -> {}
        }
    }

    // Observar estado de reenvío
    LaunchedEffect(resendCodeState) {
        when (resendCodeState) {
            is UiState.Success -> {
                println("✅ ForgotPasswordCode: Código reenviado")
                code = ""
                codeError = null
                countdown = 60
                resendEnabled = false
                viewModel.resetResendCodeState()
            }
            is UiState.Error -> {
                println("❌ ForgotPasswordCode: Error al reenviar código")
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
                    text = "Verificar código",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtítulo
                Text(
                    text = "Ingresa el código de 6 dígitos que enviamos a",
                    fontSize = 16.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.Primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Input de código de 6 cajas
                CodeInputBoxes(
                    code = code,
                    onCodeChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            code = it
                            codeError = null
                        }
                    },
                    isError = codeError != null
                )

                if (codeError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = codeError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de reenviar código
                if (!resendEnabled) {
                    Text(
                        text = "Reenviar código en ${countdown}s",
                        fontSize = 14.sp,
                        color = NuColors.TextSecondary
                    )
                } else {
                    NuSecondaryButton(
                        text = "Reenviar código",
                        onClick = {
                            println("📧 ForgotPasswordCode: Reenviando código a $email")
                            viewModel.resendVerificationCode(email)
                        },
                        textColor = NuColors.Primary,
                        enabled = resendCodeState !is UiState.Loading
                    )
                }
            }

            // ✅ Espaciador inferior (botón eliminado - verificación automática)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Loading indicator
    if (verificationState is UiState.Loading || resendCodeState is UiState.Loading) {
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

/**
 * Componente de 6 cajas para el código de verificación
 */
@Composable
fun CodeInputBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean = false
) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = 2.dp,
                                color = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    code.length > index -> NuColors.Primary
                                    else -> NuColors.TextSecondary.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (code.length > index) NuColors.Primary.copy(alpha = 0.05f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (index < code.length) code[index].toString() else "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}
