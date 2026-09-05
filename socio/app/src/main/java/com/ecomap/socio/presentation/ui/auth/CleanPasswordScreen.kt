package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors

/**
 * CleanPasswordScreen - Componente reutilizable para ingreso de contraseña
 *
 * ESPECIFICACIONES:
 * - Título: "Ahora ingresa tu contraseña" (FontWeight.Bold)
 * - TextField: Solo línea inferior, sin bordes ni fondo
 * - Placeholder: "8 dígitos o más" (FontWeight.Bold, 18.sp, Color.Gray)
 * - NO ícono de candado, NO texto "Contraseña"
 * - Solo ícono de ojo para visibilidad
 * - Enlace: "Olvidé la contraseña ➔" (morado)
 * - FAB: Círculo perfecto, fuera del Column
 *
 * @param isDarkMode true para fondo negro, false para fondo blanco
 * @param onNavigateBack callback para flecha de atrás
 * @param onPasswordSubmit callback cuando se presiona el FAB con la contraseña
 * @param onForgotPassword callback cuando se presiona "Olvidé la contraseña ➔"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanPasswordScreen(
    isDarkMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onPasswordSubmit: (String) -> Unit,
    onForgotPassword: (() -> Unit)? = null,
    onPasswordValidation: ((String) -> Boolean)? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Colores adaptativos según isDarkMode
    val backgroundColor = if (isDarkMode) Color.Black else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF000000)
    val iconColor = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF000000)
    val placeholderColor = Color.Gray
    val lineColor = if (errorMessage != null) NuColors.Error else Color.Gray.copy(alpha = 0.5f) // ✅ Rojo si hay error
    val lineFocusedColor = if (errorMessage != null) NuColors.Error else Color.Gray.copy(alpha = 0.5f) // ✅ Rojo si hay error
    val linkColor = Color(0xFF6A00A8)

    val isPasswordValid = password.isNotBlank() && password.length >= 8

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isPasswordValid && !isLoading) {
                        onPasswordSubmit(password)
                    }
                },
                shape = CircleShape,
                containerColor = if (isPasswordValid && !isLoading) NuColors.Primary else NuColors.ButtonDisabled,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                // ✅ Mostrar CircularProgressIndicator o flecha
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continuar"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
                    .imePadding(),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Ahora ingresa tu contraseña del app",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                MinimalistPasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        // Error se resetea automáticamente en los wrappers
                    },
                    passwordVisible = passwordVisible,
                    onVisibilityToggle = { passwordVisible = !passwordVisible },
                    textColor = textColor,
                    placeholderColor = placeholderColor,
                    lineColor = lineColor,
                    lineFocusedColor = lineFocusedColor,
                    iconColor = placeholderColor,
                    isError = errorMessage != null
                )

                // ✅ Mensaje de error
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 14.sp,
                        color = NuColors.Error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (onForgotPassword != null) {
                    TextButton(
                        onClick = onForgotPassword,
                        modifier = Modifier.padding(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Olvidé la contraseña ➜",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = linkColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    )
}

@Composable
private fun MinimalistPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    textColor: Color,
    placeholderColor: Color,
    lineColor: Color,
    lineFocusedColor: Color,
    iconColor: Color,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val animatedLineColor by animateColorAsState(
        targetValue = if (isFocused) lineFocusedColor else lineColor,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "line_color"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                cursorBrush = SolidColor(lineFocusedColor),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "8 dígitos o más",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = placeholderColor
                            )
                        }
                        innerTextField()
                    }
                },
                onTextLayout = {
                    isFocused = true
                }
            )

            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = iconColor
                )
            }
        }

        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = animatedLineColor
        )
    }
}