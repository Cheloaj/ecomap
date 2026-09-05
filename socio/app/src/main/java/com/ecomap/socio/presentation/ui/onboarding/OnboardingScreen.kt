package com.ecomap.socio.presentation.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.data.model.OperatingHours
import com.ecomap.socio.data.model.DaySchedule
import com.ecomap.socio.presentation.ui.components.ModernWeekSchedule
import com.ecomap.socio.presentation.ui.components.QuickScheduleSelector
import com.ecomap.socio.presentation.ui.components.NuFloatingActionButton
import com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.ui.components.OSMMapView
import com.ecomap.socio.presentation.viewmodel.OnboardingViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    userId: String,
    isNewUser: Boolean, // ✅ true = usuario nuevo, false = usuario Pro agregando negocio
    onNavigateToDocumentVerification: () -> Unit,
    onNavigateBack: () -> Unit = {}, // Para usuarios Pro que cancelan
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val createBusinessState by viewModel.createBusinessState.collectAsState()

    // Observar los estados para validación reactiva
    val businessName by viewModel.businessName.collectAsState()
    val businessType by viewModel.businessType.collectAsState()
    val location by viewModel.location.collectAsState()
    val address by viewModel.address.collectAsState()

    // Estado para el bottom sheet de confirmación
    var showConfirmationSheet by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(createBusinessState) {
        if (createBusinessState is UiState.Success) {
            delay(3000) // ✅ Esperar 3 segundos para que termine la animación de carga
            showSuccessAnimation = true
        }
    }

    // ✅ Calcular si debemos mostrar la animación
    val shouldShowLoading = createBusinessState is UiState.Loading ||
                           (createBusinessState is UiState.Success && !showSuccessAnimation)

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Solo mostrar Scaffold si NO está en animación
        if (!shouldShowLoading && !showSuccessAnimation) {
            Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = {
                    Column {
                        Text(
                            text = "Configuración de Negocio",
                            color = NuColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Paso ${currentStep + 1} de 4",
                            color = NuColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    // Mostrar botón atrás si:
                    // - Está en paso > 0, O
                    // - Está en paso 0 pero NO es nuevo usuario (usuario Pro agregando negocio)
                    if (currentStep > 0 || (currentStep == 0 && !isNewUser)) {
                        IconButton(onClick = {
                            if (currentStep == 0) {
                                // En paso 0, mostrar modal de confirmación
                                showCancelDialog = true
                            } else {
                                // En otros pasos, volver al anterior
                                println("⬅️ Botón atrás presionado")
                                viewModel.previousStep()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = NuColors.Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NuColors.Background
                )
            )
        },
        containerColor = NuColors.Background,
        floatingActionButton = {
            when (currentStep) {
                0 -> {
                    // Validación reactiva: observa directamente el estado
                    val canProceed = businessName.length >= 3
                    println("📊 Paso 0 - canProceed: $canProceed, businessName: '$businessName', length: ${businessName.length}")
                    NuFloatingActionButton(
                        onClick = {
                            println("🚀 Botón Siguiente presionado en Paso 0")
                            viewModel.nextStep()
                        },
                        enabled = canProceed,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
                1 -> {
                    // Validación reactiva: observa directamente el estado
                    val canProceed = businessType.isNotBlank()
                    println("📊 Paso 1 - canProceed: $canProceed, businessType: '$businessType'")
                    NuFloatingActionButton(
                        onClick = {
                            println("🚀 Botón Siguiente presionado en Paso 1")
                            viewModel.nextStep()
                        },
                        enabled = canProceed,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
                2 -> {
                    // Sin botón FAB - la navegación se maneja automáticamente al confirmar ubicación
                }
                3 -> {
                    val isLoading = createBusinessState is UiState.Loading
                    val operatingHours by viewModel.operatingHours.collectAsState()
                    // ✅ Calcular reactivamente cuando operatingHours cambie
                    val hasHours = remember(operatingHours) {
                        listOf(
                            operatingHours.monday.isOpen,
                            operatingHours.tuesday.isOpen,
                            operatingHours.wednesday.isOpen,
                            operatingHours.thursday.isOpen,
                            operatingHours.friday.isOpen,
                            operatingHours.saturday.isOpen,
                            operatingHours.sunday.isOpen
                        ).any { it }
                    }
                    println("📊 Paso 3 - canProceed: $hasHours, isLoading: $isLoading")
                    NuFloatingActionButton(
                        onClick = {
                            println("✅ Botón Siguiente presionado - Mostrando confirmación")
                            showConfirmationSheet = true
                        },
                        enabled = hasHours && !isLoading,
                        isLoading = isLoading,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(NuColors.Background)
        ) {
            // Barra de progreso (movida fuera de TopAppBar)
            val progress by animateFloatAsState(
                targetValue = (currentStep + 1) / 4f,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = NuColors.Primary,
                trackColor = NuColors.Surface
            )

            when (currentStep) {
                0 -> Step1BusinessName(viewModel)
                1 -> Step2BusinessType(viewModel)
                2 -> Step3Location(
                    viewModel = viewModel,
                    onNavigateToNextStep = {
                        viewModel.nextStep()
                    }
                )
                3 -> Step4OperatingHours(viewModel)
            }
        }
    } // Cierra Scaffold
    } // Cierra if

    // Bottom Sheet de confirmación
    if (showConfirmationSheet) {
        val operatingHours by viewModel.operatingHours.collectAsState()
        ScheduleConfirmationBottomSheet(
            businessName = businessName,
            businessType = businessType,
            address = address,
            operatingHours = operatingHours,
            onDismiss = { showConfirmationSheet = false },
            onConfirm = {
                showConfirmationSheet = false
                println("✅ Usuario confirmó - Creando negocio")
                viewModel.createBusiness(userId, isNewUser)
            }
        )
    }

    // ✅ Manejar botón atrás en paso 0 (solo si NO es nuevo usuario)
    BackHandler(enabled = currentStep == 0 && !isNewUser && !shouldShowLoading && !showSuccessAnimation) {
        showCancelDialog = true
    }

    // ✅ Bloquear retroceder durante la animación
    BackHandler(enabled = shouldShowLoading || showSuccessAnimation) {
        // No hacer nada - bloquear navegación hacia atrás
    }

    // ✅ Animación de carga cuando se está creando el negocio
    if (shouldShowLoading) {
        NuLoadingStateView(
            isLoading = true,
            message = "Creando tu negocio...",
            duration = 6000L // ✅ 6 segundos para la barra
        )
    }

    // ✅ Navegación después de completar animación
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            onNavigateToDocumentVerification()
        }
    }

    // ✅ Modal de confirmación para cancelar (solo usuario Pro)
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = NuColors.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(NuColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = NuColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "¿Cancelar creación?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    "Si regresas ahora, perderás todos los datos que has ingresado. ¿Estás seguro de que deseas cancelar?",
                    fontSize = 15.sp,
                    color = NuColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NuColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar creando", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.resetCreateBusinessState()
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, cancelar", fontWeight = FontWeight.Medium, color = NuColors.Error)
                }
            }
        )
    }
    } // Cierra Box
} // Cierra función

@Composable
fun Step1BusinessName(viewModel: OnboardingViewModel) {
    val businessName by viewModel.businessName.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "¿Cómo se llama\ntu negocio?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Este nombre será visible para tus clientes",
            fontSize = 16.sp,
            color = NuColors.TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // TextField estilo Nubank (limpio, sin íconos)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = businessName,
                onValueChange = {
                    println("📝 Texto cambiado: $it")
                    viewModel.updateBusinessName(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = NuColors.TextPrimary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                singleLine = true,
                cursorBrush = SolidColor(NuColors.Primary),
                decorationBox = { innerTextField ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (businessName.isEmpty()) {
                                Text(
                                    text = "Nombre del negocio",
                                    fontSize = 18.sp,
                                    color = NuColors.TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = if (businessName.isNotEmpty()) NuColors.Primary else NuColors.Surface
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = businessName.isNotBlank() && businessName.length < 3,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "El nombre debe tener al menos 3 caracteres",
                fontSize = 14.sp,
                color = NuColors.Error
            )
        }

        AnimatedVisibility(
            visible = businessName.length >= 3,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "✓ Perfecto",
                fontSize = 14.sp,
                color = NuColors.Primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun Step2BusinessType(viewModel: OnboardingViewModel) {
    val businessType by viewModel.businessType.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        BusinessTypeSearchStep(
            selectedType = businessType,
            onTypeSelected = { type ->
                println("🏪 Tipo seleccionado: $type")
                viewModel.updateBusinessType(type)
            }
        )
    }
}

@Composable
fun Step3Location(
    viewModel: OnboardingViewModel,
    onNavigateToNextStep: () -> Unit = {}
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val location by viewModel.location.collectAsState()
    val address by viewModel.address.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    var isManualMode by remember { mutableStateOf(false) }
    var tempLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var tempAddress by remember { mutableStateOf<String?>(null) }
    var isSavingLocation by remember { mutableStateOf(false) }
    var locationSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (isSavingLocation) {
        // ✅ Bloquear retroceder durante la animación
        BackHandler(enabled = true) {
            // No hacer nada - bloquear navegación hacia atrás
        }

        // ✅ Nueva animación: fondo blanco, barra 6s, texto éxito 6s
        NuLoadingSuccessView(
            isLoading = !locationSaved,
            isSuccess = locationSaved,
            loadingMessage = "Guardando ubicación...",
            successMessage = "¡Ubicación guardada!",
            loadingDuration = 6000L, // ✅ 6 segundos para la barra
            successDuration = 6000L, // ✅ 6 segundos para el mensaje de éxito
            onComplete = {
                onNavigateToNextStep()
            }
        )
    } else {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "¿Dónde está tu negocio?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (isManualMode) "Arrastra el mapa para seleccionar la ubicación" else "Elige cómo establecer la ubicación de tu negocio",
                fontSize = 16.sp,
                color = NuColors.TextSecondary,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dos botones: Automático y Manual
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LocationModeButton(
                text = "Automático",
                icon = Icons.Default.MyLocation,
                isSelected = !isManualMode,
                onClick = {
                    // Descartar ubicación temporal al cambiar de modo
                    tempLocation = null
                    tempAddress = null
                    isManualMode = false
                },
                modifier = Modifier.weight(1f)
            )
            LocationModeButton(
                text = "Manual",
                icon = Icons.Default.Edit,
                isSelected = isManualMode,
                onClick = {
                    // Descartar ubicación temporal al cambiar de modo
                    tempLocation = null
                    tempAddress = null
                    isManualMode = true
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            OSMMapView(
                modifier = Modifier.fillMaxSize(),
                location = location,
                businessName = businessName,
                isManualMode = isManualMode,
                onLocationObtained = { lat, lon, addressText ->
                    println("📍 Ubicación obtenida: $lat, $lon - $addressText")
                    // Guardar temporalmente (ambos modos)
                    tempLocation = Pair(lat, lon)
                    tempAddress = addressText
                },
                onConfirmLocation = {
                    // Solo guardar cuando se confirme (ambos modos)
                    tempLocation?.let { (lat, lon) ->
                        tempAddress?.let { addr ->
                            // ✅ CERRAR TECLADO
                            focusManager.clearFocus()

                            isSavingLocation = true
                            viewModel.updateLocation(lat, lon)
                            println("✅ Ubicación confirmada: $lat, $lon - $addr")

                            // ✅ Esperar los 3 segundos completos de la animación de carga
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(3000) // Esperar 3s para que la barra llegue al 100%
                                locationSaved = true
                            }
                        }
                    }
                },
                onModeChange = { isManual ->
                    // Descartar ubicación temporal al cambiar de modo
                    tempLocation = null
                    tempAddress = null
                    isManualMode = isManual
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LocationModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) NuColors.Primary else NuColors.Surface,
            contentColor = if (isSelected) Color.White else NuColors.TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun Step4OperatingHours(viewModel: OnboardingViewModel) {
    val operatingHours by viewModel.operatingHours.collectAsState()
    val createBusinessState by viewModel.createBusinessState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Horarios de Operación",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(12.dp))


        QuickScheduleSelector(
            currentSchedule = operatingHours,
            onScheduleChange = { newOperatingHours ->
                viewModel.setOperatingHours(newOperatingHours)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = createBusinessState is UiState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NuColors.Error.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (createBusinessState is UiState.Error) {
                        ErrorMessageHelper.getFriendlyMessage(
                            (createBusinessState as UiState.Error).message
                        )
                    } else "",
                    fontSize = 14.sp,
                    color = NuColors.Error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleConfirmationBottomSheet(
    businessName: String,
    businessType: String,
    address: String,
    operatingHours: OperatingHours,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Validaciones lógicas
    val daySchedules = listOf(
        "Lunes" to operatingHours.monday,
        "Martes" to operatingHours.tuesday,
        "Miércoles" to operatingHours.wednesday,
        "Jueves" to operatingHours.thursday,
        "Viernes" to operatingHours.friday,
        "Sábado" to operatingHours.saturday,
        "Domingo" to operatingHours.sunday
    )

    val openDays = daySchedules.filter { it.second.isOpen }
    val hasAtLeastOneOpenDay = openDays.isNotEmpty()
    val allDaysClosed = openDays.isEmpty()

    // Validar horarios (apertura antes que cierre) para cada turno
    val invalidSchedules = openDays.filter { (_, schedule) ->
        // Verificar que todos los turnos tengan horarios válidos
        schedule.shifts.any { shift ->
            val openParts = shift.openTime.split(":")
            val closeParts = shift.closeTime.split(":")
            val openMinutes = openParts[0].toInt() * 60 + openParts[1].toInt()
            val closeMinutes = closeParts[0].toInt() * 60 + closeParts[1].toInt()
            openMinutes >= closeMinutes
        }
    }

    val hasValidHours = invalidSchedules.isEmpty()

    // ✅ Validaciones de lógica real de negocios mexicanos
    // Identificar días abiertos/cerrados
    val mondayOpen = operatingHours.monday.isOpen
    val tuesdayOpen = operatingHours.tuesday.isOpen
    val wednesdayOpen = operatingHours.wednesday.isOpen
    val thursdayOpen = operatingHours.thursday.isOpen
    val fridayOpen = operatingHours.friday.isOpen
    val saturdayOpen = operatingHours.saturday.isOpen
    val sundayOpen = operatingHours.sunday.isOpen

    val weekdaysOpen = listOf(mondayOpen, tuesdayOpen, wednesdayOpen, thursdayOpen, fridayOpen)
    val weekendOpen = listOf(saturdayOpen, sundayOpen)

    // Contar días laborales abiertos (lunes-viernes)
    val weekdaysOpenCount = weekdaysOpen.count { it }
    val weekendOpenCount = weekendOpen.count { it }
    val totalDaysOpen = openDays.size

    // Validación 1: Solo fin de semana abierto (sin días laborales)
    val onlyWeekendOpen = weekdaysOpenCount == 0 && weekendOpenCount > 0

    // Validación 2: Mayoría de días laborales cerrados (menos de 2 días laborales abiertos)
    val mostWeekdaysClosed = weekdaysOpenCount > 0 && weekdaysOpenCount < 2 && weekendOpenCount > 0

    // Validación 3: Solo 1 o 2 días abiertos en total
    val tooFewDaysOpen = totalDaysOpen in 1..2

    // Validación 4: Solo domingo abierto
    val onlySundayOpen = sundayOpen && totalDaysOpen == 1

    // Mensaje de advertencia según el patrón ilógico detectado
    val logicWarning: String? = when {
        onlySundayOpen -> "⚠️ Solo abrir el domingo es muy inusual para un negocio. Los negocios típicamente abren de lunes a sábado."
        onlyWeekendOpen -> "⚠️ Solo abrir los fines de semana (sin días laborales) es muy poco común. La mayoría de negocios en México abren de lunes a viernes."
        mostWeekdaysClosed -> "⚠️ Tener cerrado la mayoría de días laborales (lunes-viernes) no es común para un negocio. Verifica tus horarios."
        tooFewDaysOpen && weekdaysOpenCount == 0 -> "⚠️ Abrir solo ${totalDaysOpen} día${if (totalDaysOpen > 1) "s" else ""} a la semana es muy poco. Los negocios suelen abrir al menos de lunes a viernes."
        else -> null
    }

    val hasLogicWarning = logicWarning != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = NuColors.TextSecondary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        // ✅ Scroll al inicio cuando se abre el BottomSheet
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                scrollState.scrollTo(0)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Título
            Text(
                text = "¿Confirmar información?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )

            // Resumen del negocio
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NuColors.Background
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Nombre",
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary
                            )
                            Text(
                                text = businessName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = NuColors.TextPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Tipo",
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary
                            )
                            Text(
                                text = businessType,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = NuColors.TextPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Ubicación",
                                fontSize = 12.sp,
                                color = NuColors.TextSecondary
                            )
                            Text(
                                text = address,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = NuColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // Horarios
            Text(
                text = "Horarios de atención",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NuColors.TextPrimary
            )

            // Advertencias
            if (allDaysClosed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NuColors.Error
                        )
                        Text(
                            text = "No puedes tener todos los días cerrados. Configura al menos un día de atención.",
                            fontSize = 14.sp,
                            color = NuColors.Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!hasValidHours && !allDaysClosed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NuColors.Error
                        )
                        Column {
                            Text(
                                text = "Horarios inválidos",
                                fontSize = 14.sp,
                                color = NuColors.Error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "El horario de apertura debe ser antes del horario de cierre.",
                                fontSize = 13.sp,
                                color = NuColors.Error
                            )
                        }
                    }
                }
            }

            // ✅ Advertencia de lógica de negocio
            if (hasLogicWarning && !allDaysClosed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = logicWarning ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFFFF6F00),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Lista de horarios
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NuColors.Background
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    daySchedules.forEach { (dayName, schedule) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = NuColors.TextPrimary
                            )
                            if (schedule.isOpen && schedule.shifts.isNotEmpty()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    schedule.shifts.forEach { shift ->
                                        Text(
                                            text = "${formatTime(shift.openTime)} - ${formatTime(shift.closeTime)}",
                                            fontSize = 13.sp,
                                            color = NuColors.Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Cerrado",
                                    fontSize = 13.sp,
                                    color = NuColors.Error
                                )
                            }
                        }
                    }
                }
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Editar", fontSize = 15.sp)
                }

                Button(
                    onClick = onConfirm,
                    enabled = hasAtLeastOneOpenDay && hasValidHours && !hasLogicWarning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NuColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar", fontSize = 15.sp)
                }
            }
        }
    }
}

private fun formatTime(time: String): String {
    val parts = time.split(":")
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}