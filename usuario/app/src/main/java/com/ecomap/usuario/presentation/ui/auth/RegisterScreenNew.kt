package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.NuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de Registro Única - Estilo Nu para Usuarios
 * Todos los campos en una sola pantalla con fondo blanco
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreenNew(
    onNavigateBack: () -> Unit,
    onNavigateToBiometric: (String, String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // FocusRequesters para navegar entre campos
    val emailFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val passwordFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val confirmPasswordFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Navegación automática después del registro
    LaunchedEffect(authState) {
        when (authState) {
            is com.ecomap.usuario.presentation.viewmodel.AuthUiState.Success -> {
                isLoading = false
                focusManager.clearFocus()
                delay(500)
                // Navegar a BiometricSetup
                onNavigateToBiometric(email, password, fullName)
            }
            is com.ecomap.usuario.presentation.viewmodel.AuthUiState.Error -> {
                isLoading = false
                errorMessage = (authState as com.ecomap.usuario.presentation.viewmodel.AuthUiState.Error).message
            }
            is com.ecomap.usuario.presentation.viewmodel.AuthUiState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Validar formulario
    val isFormValid = fullName.trim().length >= 3 &&
            email.isNotBlank() &&
            password.length >= 6 &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = NuColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.White
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Formulario desplazable
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState)
                        .imePadding(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // TÍTULO
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                            initialOffsetY = { -50 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
                    ) {
                        val titleString = buildAnnotatedString {
                            withStyle(style = SpanStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A00A8),
                                fontFamily = FontFamily.SansSerif
                            )) {
                                append("Continúa con tu solicitud ")
                            }
                            withStyle(style = SpanStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF000000),
                                fontFamily = FontFamily.SansSerif
                            )) {
                                append("con el mismo correo y contraseña")
                            }
                        }
                        Text(
                            text = titleString,
                            lineHeight = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // FORMULARIO
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Column {
                            // Campo 1: Nombre completo
                            MinimalistTextField(
                                value = fullName,
                                onValueChange = { newValue ->
                                    fullName = newValue.split(" ").joinToString(" ") { word ->
                                        word.replaceFirstChar { it.uppercase() }
                                    }
                                },
                                label = "Nombre completo",
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                                onImeAction = {
                                    emailFocusRequester.requestFocus()
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.value + 200)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(30.dp))

                            // Campo 2: Email
                            MinimalistTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Escribe tu correo electrónico",
                                keyboardType = KeyboardType.Email,
                                focusRequester = emailFocusRequester,
                                imeAction = ImeAction.Next,
                                onImeAction = {
                                    passwordFocusRequester.requestFocus()
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.value + 200)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(30.dp))

                            // Campo 3: Contraseña
                            MinimalistPasswordField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Crear contraseña",
                                passwordVisible = passwordVisible,
                                onVisibilityToggle = { passwordVisible = !passwordVisible },
                                focusRequester = passwordFocusRequester,
                                imeAction = ImeAction.Next,
                                onImeAction = {
                                    confirmPasswordFocusRequester.requestFocus()
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.value + 200)
                                    }
                                }
                            )

                            // Indicador de longitud de contraseña
                            if (password.isNotBlank() && password.length < 6) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "La contraseña debe tener al menos 6 caracteres",
                                    fontSize = 12.sp,
                                    color = NuColors.Error,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            // Campo 4: Confirmar contraseña
                            MinimalistPasswordField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Confirmar contraseña",
                                passwordVisible = confirmPasswordVisible,
                                onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                                focusRequester = confirmPasswordFocusRequester,
                                imeAction = ImeAction.Done,
                                onImeAction = {
                                    focusManager.clearFocus()
                                    if (isFormValid && !isLoading) {
                                        isLoading = true
                                        viewModel.signUp(email, password, fullName)
                                    }
                                }
                            )

                            // Validación de coincidencia de contraseñas
                            if (confirmPassword.isNotBlank() && password != confirmPassword) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Las contraseñas no coinciden",
                                    fontSize = 12.sp,
                                    color = NuColors.Error,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            // Mensaje de error
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 14.sp,
                                    color = NuColors.Error,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Texto Legal con FAB al lado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Texto Legal
                                val annotatedString = buildAnnotatedString {
                                    withStyle(style = SpanStyle(
                                        color = NuColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )) {
                                        append("Al continuar aceptas nuestro ")
                                    }

                                    pushStringAnnotation(tag = "privacy", annotation = "privacy_policy")
                                    withStyle(style = SpanStyle(
                                        color = NuColors.Primary,
                                        fontSize = 12.sp,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.SansSerif
                                    )) {
                                        append("Aviso de Privacidad")
                                    }
                                    pop()

                                    withStyle(style = SpanStyle(
                                        color = NuColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )) {
                                        append(" y confirmas que has leído nuestros términos y condiciones.")
                                    }
                                }

                                ClickableText(
                                    text = annotatedString,
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(
                                            tag = "privacy",
                                            start = offset,
                                            end = offset
                                        ).firstOrNull()?.let {
                                            // TODO: Navegar a pantalla de Aviso de Privacidad
                                            println("Click en Aviso de Privacidad")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    style = TextStyle(
                                        textAlign = TextAlign.Start,
                                        lineHeight = 16.sp
                                    )
                                )

                                // FAB fijo al lado del texto
                                FloatingActionButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (isFormValid && !isLoading) {
                                            isLoading = true
                                            viewModel.signUp(email, password, fullName)
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = if (isFormValid && !isLoading)
                                        NuColors.Primary else NuColors.Divider,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(56.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp,
                                        focusedElevation = 0.dp,
                                        hoveredElevation = 0.dp
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Continuar"
                                        )
                                    }
                                }
                            }

                            // Espacio extra para scroll
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Campo de texto minimalista - Línea gris y etiqueta arriba
 */
@Composable
private fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Etiqueta arriba
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Campo de entrada
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester)
                    else Modifier
                ),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontFamily = FontFamily.SansSerif
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.Gray),
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                    // Línea inferior siempre gris
                    HorizontalDivider(
                        thickness = 1.5.dp,
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        )
    }
}

/**
 * Campo de contraseña minimalista - Línea gris con ícono de ojo
 */
@Composable
private fun MinimalistPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                        else Modifier
                    ),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    fontFamily = FontFamily.SansSerif
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() }
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                cursorBrush = SolidColor(Color.Gray),
                decorationBox = { innerTextField ->
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                        // Línea inferior siempre gris
                        HorizontalDivider(
                            thickness = 1.5.dp,
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                }
            )

            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (passwordVisible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible)
                        "Ocultar contraseña"
                    else
                        "Mostrar contraseña",
                    tint = Color(0xFF777777)
                )
            }
        }
    }
}
