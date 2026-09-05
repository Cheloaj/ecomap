package com.ecomap.socio.presentation.ui.auth

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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import com.ecomap.socio.utils.rememberSingleExecution
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de Registro - Rediseñada
 *
 * ESPECIFICACIONES:
 * - Fondo blanco, TopAppBar transparente con flecha <
 * - Título: "Continúa con tu solicitud con el mismo correo y contraseña" (morado para la primera parte, negro para la segunda, en una sola línea)
 * - Tipografía: SansSerif (Roboto, por defecto en Android, similar a Gellix)
 * - Campos minimalistas: Solo línea inferior en gris claro, SIN íconos (excepto ojo en contraseñas)
 * - Etiquetas arriba de los campos (4.dp de separación con el campo, 16.dp entre campos)
 * - FAB circular perfecto, fijo al lado del texto "Al continuar aceptas nuestro Aviso de Privacidad y confirmas que has leído nuestros términos y condiciones"
 * - Formulario desplazable para facilitar la entrada de datos sin cerrar el teclado
 *
 * NOTA: Asegúrate de configurar la actividad con `window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)`
 * en el `onCreate` para que `imePadding()` funcione correctamente en el formulario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // FocusRequesters para navegar entre campos con Enter
    val emailFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val passwordFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val confirmPasswordFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val signUpState by viewModel.signUpState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 🛡️ FASE 1: Protección contra clics rápidos (500ms cooldown)
    val executeSafely = rememberSingleExecution()

    // Navegación automática al verificar email CON DELAY
    LaunchedEffect(signUpState) {
        when (signUpState) {
            is UiState.Success -> {
                // ✅ CERRAR TECLADO
                focusManager.clearFocus()

                // ✅ GUARDAR CREDENCIALES AUTOMÁTICAMENTE al registrarse
                println("🔐 GUARDANDO CREDENCIALES AUTOMÁTICAMENTE (REGISTRO)")
                println("🔐 Email: $email")
                println("🔐 FullName: $fullName")
                com.ecomap.socio.utils.BiometricHelper.saveBiometricCredentials(context, email, password, fullName)

                // Verificar que se guardaron correctamente
                val saved = com.ecomap.socio.utils.BiometricHelper.getSavedCredentials(context)
                if (saved != null) {
                    println("✅ Credenciales guardadas exitosamente (email: ${saved.first}, fullName: ${saved.third})")
                } else {
                    println("❌ ERROR: Las credenciales NO se guardaron")
                }

                // ✅ ESPERAR 6 SEGUNDOS para que la barra llegue al 100%
                delay(6000)
                showSuccessAnimation = true
            }
            else -> {}
        }
    }

    // Navegación después de animación de éxito
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            delay(300) // ✅ 300ms - transición rápida
            onNavigateToVerification(email)
        }
    }

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Verificar si todos los campos están llenos y las contraseñas coinciden
    val isFormValid = fullName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            password.length >= 8 &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword

    // ✅ Box para que la animación cubra TODA la pantalla incluyendo TopAppBar
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // TopAppBar transparente con flecha <
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
                    .imePadding(), // Asegura que el formulario no quede tapado por el teclado
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // TÍTULO Y SUBTÍTULO COMO ORACIÓN FLUIDA
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                        initialOffsetY = { -50 },
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                ) {
                    // Usamos AnnotatedString para combinar título y subtítulo en una línea
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

                // FORMULARIO MINIMALISTA
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column {
                        // Campo 1: Nombre completo (etiqueta arriba)
                        MinimalistTextField(
                            value = fullName,
                            onValueChange = { newValue ->
                                // ✅ Capitalizar primera letra de cada palabra
                                fullName = newValue.split(" ").joinToString(" ") { word ->
                                    word.replaceFirstChar { it.uppercase() }
                                }
                            },
                            label = "Nombre completo",
                            keyboardType = KeyboardType.Text,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            onImeAction = {
                                emailFocusRequester.requestFocus()
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.value + 200)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(30.dp)) // Espacio reducido entre campos

                        // Campo 2: Correo electrónico (etiqueta arriba)
                        MinimalistTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Escribe tu correo electrónico",
                            keyboardType = KeyboardType.Email,
                            focusRequester = emailFocusRequester,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            onImeAction = {
                                passwordFocusRequester.requestFocus()
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.value + 200)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(30.dp)) // Espacio reducido entre campos

                        // Campo 3: Crear contraseña (etiqueta arriba)
                        MinimalistPasswordField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Crear contraseña",
                            passwordVisible = passwordVisible,
                            onVisibilityToggle = { passwordVisible = !passwordVisible },
                            focusRequester = passwordFocusRequester,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            onImeAction = {
                                confirmPasswordFocusRequester.requestFocus()
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.value + 200)
                                }
                            }
                        )

                        // Indicador de longitud de contraseña
                        if (password.isNotBlank() && password.length < 8) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "La contraseña debe tener al menos 8 caracteres",
                                fontSize = 12.sp,
                                color = NuColors.Error,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp)) // Espacio reducido entre campos

                        // Campo 4: Confirmar contraseña (etiqueta arriba)
                        MinimalistPasswordField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirmar contraseña",
                            passwordVisible = confirmPasswordVisible,
                            onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                            focusRequester = confirmPasswordFocusRequester,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                            onImeAction = {
                                focusManager.clearFocus()
                                if (isFormValid) {
                                    viewModel.signUp(email, password, confirmPassword, fullName)
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

                        Spacer(modifier = Modifier.height(30.dp)) // Espacio reducido antes del mensaje de error

                        // Mensaje de error
                        if (signUpState is UiState.Error) {
                            Text(
                                text = ErrorMessageHelper.getFriendlyMessage(
                                    (signUpState as UiState.Error).message
                                ),
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
                                    // 🛡️ FASE 1: Protección contra doble clic (evita registros duplicados)
                                    executeSafely {
                                        // ✅ CERRAR TECLADO
                                        focusManager.clearFocus()

                                        if (isFormValid) {
                                            viewModel.signUp(email, password, confirmPassword, fullName)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                containerColor = if (isFormValid && signUpState !is UiState.Loading)
                                    NuColors.Primary else NuColors.ButtonDisabled,
                                contentColor = Color.White,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Continuar"
                                )
                            }
                        }

                        // Espacio extra para asegurar que el formulario sea desplazable
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }

        }
    }

        // ✅ Animación FUERA del Scaffold para cubrir TODO incluyendo TopAppBar
        val shouldShowLoading = signUpState is UiState.Loading ||
                               (signUpState is UiState.Success && !showSuccessAnimation)

        // ✅ Bloquear retroceder durante la animación
        BackHandler(enabled = shouldShowLoading || showSuccessAnimation) {
            // No hacer nada - bloquear navegación hacia atrás
        }

        com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView(
            isLoading = shouldShowLoading,
            isSuccess = showSuccessAnimation,
            loadingMessage = "Creando tu cuenta...",
            successMessage = "¡Cuenta creada!",
            loadingDuration = 6000L,
            successDuration = 6000L,
            onComplete = {
                // La navegación se maneja en LaunchedEffect
            }
        )
    }
}

/**
 * Campo de texto minimalista - Solo línea inferior en gris claro, sin bordes, sin fondo, SIN íconos, con etiqueta arriba
 */
/**
 * Campo de texto minimalista - Línea gris y texto más grueso
 */
@Composable
private fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
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
                fontWeight = FontWeight.SemiBold, // ← Texto más grueso
                color = Color.Black,
                fontFamily = FontFamily.SansSerif
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.Gray), // ← Cursor gris
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
 * Campo de contraseña minimalista - Línea gris y texto más grueso
 */
@Composable
private fun MinimalistPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
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
                    fontWeight = FontWeight.SemiBold, // ← Texto más grueso
                    color = Color.Black,
                    fontFamily = FontFamily.SansSerif
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = imeAction
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() }
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                cursorBrush = SolidColor(Color.Gray), // ← Cursor gris
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
