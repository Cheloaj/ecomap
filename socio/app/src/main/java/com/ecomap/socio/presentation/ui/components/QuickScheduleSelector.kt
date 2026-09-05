package com.ecomap.socio.presentation.ui.components

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.data.model.DaySchedule
import com.ecomap.socio.data.model.OperatingHours
import com.ecomap.socio.ui.theme.NuColors

@Composable
fun QuickScheduleSelector(
    currentSchedule: OperatingHours,
    onScheduleChange: (OperatingHours) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<SchedulePreset?>(null) }
    var showCustomSchedule by remember { mutableStateOf(false) }
    var customSchedules by remember { mutableStateOf<List<CustomScheduleRange>>(emptyList()) }
    var isSplitShift by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPreset) {
        if (selectedPreset != null) {
            val preset = selectedPreset!!
            customSchedules = listOf(
                CustomScheduleRange(
                    daysActive = preset.daysActive,
                    openTime = preset.openTime,
                    closeTime = preset.closeTime
                )
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header con animación
        AnimatedHeader()

        // Presets con animaciones stagger
        val presets = remember {
            listOf(
                SchedulePreset("weekday", "Lun - Vie", "7:00 AM - 8:00 PM", Icons.Default.Work, "07:00", "20:00",
                    listOf(true, true, true, true, true, false, false)),
                SchedulePreset("full_week", "Toda la semana", "7:00 AM - 9:00 PM", Icons.Default.CalendarMonth, "07:00", "21:00",
                    listOf(true, true, true, true, true, true, true)),
                SchedulePreset("weekend", "Fin de semana", "8:00 AM - 3:00 PM", Icons.Default.Weekend, "08:00", "15:00",
                    listOf(false, false, false, false, false, true, true))
            )
        }

        presets.chunked(2).forEachIndexed { rowIndex, rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPresets.forEachIndexed { colIndex, preset ->
                    val delay = (rowIndex * 2 + colIndex) * 50
                    AnimatedPresetCard(
                        preset = preset,
                        isSelected = selectedPreset?.id == preset.id && !showCustomSchedule,
                        onClick = {
                            selectedPreset = preset
                            showCustomSchedule = false
                            onScheduleChange(preset.toOperatingHours())
                        },
                        modifier = Modifier.weight(1f),
                        animationDelay = delay
                    )
                }
                if (rowPresets.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botón personalizado con animación
        AnimatedCustomButton(
            isExpanded = showCustomSchedule,
            onClick = {
                showCustomSchedule = !showCustomSchedule
                if (showCustomSchedule) selectedPreset = null
            }
        )

        // Panel personalizado
        AnimatedVisibility(
            visible = showCustomSchedule,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                    expandVertically(tween(400, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            CleanCustomPanel(
                customSchedules = customSchedules,
                isSplitShift = isSplitShift,
                onSplitShiftChange = { isSplitShift = it },
                onSchedulesChange = { newSchedules ->
                    customSchedules = newSchedules
                    onScheduleChange(customSchedulesToOperatingHours(newSchedules))
                }
            )
        }

        // Configuración actual con animaciones
        AnimatedVisibility(
            visible = customSchedules.isNotEmpty(),
            enter = fadeIn(tween(400)) + expandVertically(tween(400)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = NuColors.TextSecondary.copy(alpha = 0.15f), thickness = 1.dp)

                Text(
                    text = "Configuración actual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextSecondary
                )

                val sortedSchedules = customSchedules.sortedBy { schedule ->
                    schedule.daysActive.indexOfFirst { it }
                }

                sortedSchedules.forEachIndexed { index, schedule ->
                    AnimatedScheduleItem(
                        schedule = schedule,
                        index = index,
                        onDelete = {
                            // Eliminar el turno
                            customSchedules = customSchedules.filter { it != schedule }

                            // Si no quedan turnos, resetear todo
                            if (customSchedules.isEmpty()) {
                                selectedPreset = null
                                isSplitShift = false
                            }

                            // ✅ Sincronizar isSplitShift: verificar si algún día tiene 2 turnos
                            val dayShifts = Array(7) { mutableListOf<CustomScheduleRange>() }
                            customSchedules.forEach { s ->
                                s.daysActive.forEachIndexed { dayIndex, isActive ->
                                    if (isActive) dayShifts[dayIndex].add(s)
                                }
                            }
                            val anyDayHasTwoShifts = dayShifts.any { it.size >= 2 }
                            if (!anyDayHasTwoShifts) {
                                isSplitShift = false
                            }

                            onScheduleChange(customSchedulesToOperatingHours(customSchedules))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedHeader() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -20 }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Horarios",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )
        }
    }
}

@Composable
fun AnimatedPresetCard(
    preset: SchedulePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 2.dp,
        label = "elevation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) NuColors.Primary else NuColors.Surface
            ),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    NuColors.Primary,
                                    NuColors.Primary.copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(NuColors.Surface, NuColors.Surface)
                            )
                        }
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preset.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else NuColors.TextPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = preset.subtitle,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.95f) else NuColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCustomButton(
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isExpanded) NuColors.Primary.copy(alpha = 0.08f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isExpanded) 2.dp else 1.dp,
            color = if (isExpanded) NuColors.Primary.copy(alpha = 0.4f) else NuColors.TextSecondary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (isExpanded) NuColors.Primary else NuColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Personalizar",
                    fontSize = 15.sp,
                    fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isExpanded) NuColors.Primary else NuColors.TextPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) NuColors.Primary else NuColors.TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
fun CleanCustomPanel(
    customSchedules: List<CustomScheduleRange>,
    isSplitShift: Boolean,
    onSplitShiftChange: (Boolean) -> Unit,
    onSchedulesChange: (List<CustomScheduleRange>) -> Unit
) {
    val context = LocalContext.current
    var selectedDays by remember { mutableStateOf(listOf(false, false, false, false, false, false, false)) }
    var openTime by remember { mutableStateOf("07:00") }
    var closeTime by remember { mutableStateOf("20:00") }
    var shift1Open by remember { mutableStateOf("07:00") }
    var shift1Close by remember { mutableStateOf("14:00") }
    var shift2Open by remember { mutableStateOf("16:00") }
    var shift2Close by remember { mutableStateOf("21:00") }

    val occupiedDays = customSchedules
        .flatMap { it.daysActive.mapIndexed { index, active -> if (active) index else -1 } }
        .filter { it != -1 }
        .toSet()

    val hasShiftOverlap = if (isSplitShift) {
        val shift1EndMinutes = shift1Close.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val shift2StartMinutes = shift2Open.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        shift1EndMinutes >= shift2StartMinutes
    } else false

    val hasInvalidSimpleHours = if (!isSplitShift) {
        val openMinutes = openTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val closeMinutes = closeTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        openMinutes >= closeMinutes
    } else false

    val hasInvalidShift1 = if (isSplitShift) {
        val openMinutes = shift1Open.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val closeMinutes = shift1Close.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        openMinutes >= closeMinutes
    } else false

    val hasInvalidShift2 = if (isSplitShift) {
        val openMinutes = shift2Open.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val closeMinutes = shift2Close.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        openMinutes >= closeMinutes
    } else false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NuColors.Surface,
                        NuColors.Surface.copy(alpha = 0.95f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, NuColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Switch horario partido con animación
        AnimatedSplitShiftToggle(
            isSplitShift = isSplitShift,
            onToggle = onSplitShiftChange
        )

        // Selector de días
        Text(
            text = "Selecciona los días",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NuColors.TextPrimary
        )

        SimpleWeekDaySelector(
            selectedDays = selectedDays,
            occupiedDays = occupiedDays,
            onDaysChange = { selectedDays = it }
        )

        // Info ayuda
        AnimatedInfoCard(text = "Los días sin horario permanecerán cerrados")

        // Horarios
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + expandVertically(tween(400))
        ) {
            if (!isSplitShift) {
                SimpleHoursSection(
                    openTime = openTime,
                    closeTime = closeTime,
                    onOpenTimeChange = { openTime = it },
                    onCloseTimeChange = { closeTime = it },
                    context = context
                )
            } else {
                SplitShiftsSection(
                    shift1Open = shift1Open,
                    shift1Close = shift1Close,
                    shift2Open = shift2Open,
                    shift2Close = shift2Close,
                    onShift1OpenChange = { shift1Open = it },
                    onShift1CloseChange = { shift1Close = it },
                    onShift2OpenChange = { shift2Open = it },
                    onShift2CloseChange = { shift2Close = it },
                    context = context
                )
            }
        }

        // Errores animados
        AnimatedVisibility(
            visible = hasInvalidSimpleHours || hasShiftOverlap || hasInvalidShift1 || hasInvalidShift2,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            ErrorCard(
                message = when {
                    hasInvalidSimpleHours -> "El horario de apertura debe ser antes del cierre"
                    hasShiftOverlap -> "El Turno 1 debe terminar antes de que inicie el Turno 2"
                    hasInvalidShift1 || hasInvalidShift2 -> "Verifica que cada turno tenga horarios válidos"
                    else -> ""
                }
            )
        }

        // Botón agregar con animación
        AnimatedAddButton(
            enabled = selectedDays.any { it } && !hasShiftOverlap && !hasInvalidSimpleHours && !hasInvalidShift1 && !hasInvalidShift2,
            isSplitShift = isSplitShift,
            onClick = {
                if (selectedDays.any { it }) {
                    if (isSplitShift) {
                        onSchedulesChange(
                            customSchedules +
                                    CustomScheduleRange(selectedDays, shift1Open, shift1Close) +
                                    CustomScheduleRange(selectedDays, shift2Open, shift2Close)
                        )
                    } else {
                        onSchedulesChange(customSchedules + CustomScheduleRange(selectedDays, openTime, closeTime))
                    }
                    selectedDays = listOf(false, false, false, false, false, false, false)
                    openTime = "07:00"
                    closeTime = "20:00"
                    shift1Open = "07:00"
                    shift1Close = "14:00"
                    shift2Open = "16:00"
                    shift2Close = "21:00"
                }
            }
        )
    }
}

@Composable
fun AnimatedSplitShiftToggle(isSplitShift: Boolean, onToggle: (Boolean) -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSplitShift) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toggle_scale"
    )

    Card(
        modifier = Modifier.scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isSplitShift) NuColors.Primary.copy(alpha = 0.1f) else NuColors.Background
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSplitShift) 2.dp else 1.dp,
            color = if (isSplitShift) NuColors.Primary.copy(alpha = 0.3f) else NuColors.TextSecondary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Horario partido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSplitShift) NuColors.Primary else NuColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dos turnos en el mismo día",
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary
                )
            }
            Switch(
                checked = isSplitShift,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NuColors.Primary,
                    uncheckedThumbColor = Color(0xFF8E8E93),
                    uncheckedTrackColor = Color(0xFFE5E5EA),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SimpleWeekDaySelector(
    selectedDays: List<Boolean>,
    occupiedDays: Set<Int>,
    onDaysChange: (List<Boolean>) -> Unit
) {
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays[index]
            val isOccupied = occupiedDays.contains(index)

            AnimatedDayButton(
                day = day,
                isSelected = isSelected,
                isOccupied = isOccupied,
                onClick = {
                    if (!isOccupied) {
                        val newDays = selectedDays.toMutableList()
                        newDays[index] = !newDays[index]
                        onDaysChange(newDays)
                    }
                }
            )
        }
    }
}

@Composable
fun AnimatedDayButton(
    day: String,
    isSelected: Boolean,
    isOccupied: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "day_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    when {
                        isOccupied -> NuColors.TextSecondary.copy(alpha = 0.15f)
                        isSelected -> NuColors.Primary
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = 2.dp,
                    color = when {
                        isOccupied -> Color.Transparent
                        isSelected -> Color.Transparent
                        else -> NuColors.TextSecondary.copy(alpha = 0.25f)
                    },
                    shape = CircleShape
                )
                .clickable(enabled = !isOccupied, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.first().toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isOccupied -> NuColors.TextSecondary.copy(alpha = 0.4f)
                    isSelected -> Color.White
                    else -> NuColors.TextPrimary
                }
            )
        }
        Text(
            text = day,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isOccupied -> NuColors.TextSecondary.copy(alpha = 0.5f)
                isSelected -> NuColors.Primary
                else -> NuColors.TextSecondary
            }
        )
    }
}

@Composable
fun SimpleHoursSection(
    openTime: String,
    closeTime: String,
    onOpenTimeChange: (String) -> Unit,
    onCloseTimeChange: (String) -> Unit,
    context: android.content.Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Horario de operación",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NuColors.TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeButton(
                time = openTime,
                label = "Apertura",
                onClick = {
                    val parts = openTime.split(":")
                    TimePickerDialog(context, { _, h, m ->
                        onOpenTimeChange(String.format("%02d:%02d", h, m))
                    }, parts[0].toInt(), parts[1].toInt(), false).show()
                },
                modifier = Modifier.weight(1f)
            )
            TimeButton(
                time = closeTime,
                label = "Cierre",
                onClick = {
                    val parts = closeTime.split(":")
                    TimePickerDialog(context, { _, h, m ->
                        onCloseTimeChange(String.format("%02d:%02d", h, m))
                    }, parts[0].toInt(), parts[1].toInt(), false).show()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SplitShiftsSection(
    shift1Open: String, shift1Close: String,
    shift2Open: String, shift2Close: String,
    onShift1OpenChange: (String) -> Unit, onShift1CloseChange: (String) -> Unit,
    onShift2OpenChange: (String) -> Unit, onShift2CloseChange: (String) -> Unit,
    context: android.content.Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShiftCard(
            title = "Turno 1 - Mañana",
            openTime = shift1Open,
            closeTime = shift1Close,
            onOpenTimeChange = onShift1OpenChange,
            onCloseTimeChange = onShift1CloseChange,
            context = context,
            color = NuColors.Primary.copy(alpha = 0.08f)
        )
        ShiftCard(
            title = "Turno 2 - Tarde",
            openTime = shift2Open,
            closeTime = shift2Close,
            onOpenTimeChange = onShift2OpenChange,
            onCloseTimeChange = onShift2CloseChange,
            context = context,
            color = Color(0xFF4CAF50).copy(alpha = 0.08f)
        )
    }
}

@Composable
fun ShiftCard(
    title: String,
    openTime: String,
    closeTime: String,
    onOpenTimeChange: (String) -> Unit,
    onCloseTimeChange: (String) -> Unit,
    context: android.content.Context,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeButton(
                    time = openTime,
                    label = "Inicio",
                    onClick = {
                        val parts = openTime.split(":")
                        TimePickerDialog(context, { _, h, m ->
                            onOpenTimeChange(String.format("%02d:%02d", h, m))
                        }, parts[0].toInt(), parts[1].toInt(), false).show()
                    },
                    modifier = Modifier.weight(1f)
                )
                TimeButton(
                    time = closeTime,
                    label = "Fin",
                    onClick = {
                        val parts = closeTime.split(":")
                        TimePickerDialog(context, { _, h, m ->
                            onCloseTimeChange(String.format("%02d:%02d", h, m))
                        }, parts[0].toInt(), parts[1].toInt(), false).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TimeButton(
    time: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = NuColors.TextSecondary
        )
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = NuColors.Background,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NuColors.Primary.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTime(time),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.TextPrimary
                )
            }
        }
    }
}

@Composable
fun AnimatedInfoCard(text: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + expandVertically(tween(400))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = NuColors.Primary
            )
            Text(
                text = text,
                fontSize = 13.sp,
                color = NuColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuColors.Error.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = NuColors.Error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            fontSize = 13.sp,
            color = NuColors.Error,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AnimatedAddButton(
    enabled: Boolean,
    isSplitShift: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = NuColors.Primary,
            disabledContainerColor = NuColors.TextSecondary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (isSplitShift) "Agregar 2 turnos" else "Agregar horario",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AnimatedScheduleItem(
    schedule: CustomScheduleRange,
    index: Int,
    onDelete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 100).toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -50 }
    ) {
        val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val activeDays = schedule.daysActive.mapIndexedNotNull { idx, active ->
            if (active) dayNames[idx] else null
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NuColors.Background),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NuColors.Primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NuColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = activeDays.joinToString(", "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${formatTime(schedule.openTime)} - ${formatTime(schedule.closeTime)}",
                            fontSize = 13.sp,
                            color = NuColors.TextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar",
                        tint = NuColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Funciones auxiliares
fun formatTime(time: String): String {
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

fun customSchedulesToOperatingHours(schedules: List<CustomScheduleRange>): OperatingHours {
    val dayShifts = Array(7) { mutableListOf<com.ecomap.socio.data.model.TimeRange>() }

    schedules.forEach { schedule ->
        schedule.daysActive.forEachIndexed { dayIndex, isActive ->
            if (isActive) {
                dayShifts[dayIndex].add(
                    com.ecomap.socio.data.model.TimeRange(
                        openTime = schedule.openTime,
                        closeTime = schedule.closeTime
                    )
                )
            }
        }
    }

    fun createDaySchedule(dayIndex: Int): DaySchedule {
        return if (dayShifts[dayIndex].isEmpty()) {
            DaySchedule(isOpen = false, shifts = emptyList())
        } else {
            val sortedShifts = dayShifts[dayIndex].sortedBy { it.openTime }
            DaySchedule(isOpen = true, shifts = sortedShifts)
        }
    }

    return OperatingHours(
        monday = createDaySchedule(0),
        tuesday = createDaySchedule(1),
        wednesday = createDaySchedule(2),
        thursday = createDaySchedule(3),
        friday = createDaySchedule(4),
        saturday = createDaySchedule(5),
        sunday = createDaySchedule(6)
    )
}

data class SchedulePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val openTime: String,
    val closeTime: String,
    val daysActive: List<Boolean>
) {
    fun toOperatingHours(): OperatingHours {
        val timeRange = com.ecomap.socio.data.model.TimeRange(
            openTime = openTime,
            closeTime = closeTime
        )

        fun createDaySchedule(isActive: Boolean): DaySchedule {
            return if (isActive) {
                DaySchedule(isOpen = true, shifts = listOf(timeRange))
            } else {
                DaySchedule(isOpen = false, shifts = emptyList())
            }
        }

        return OperatingHours(
            monday = createDaySchedule(daysActive[0]),
            tuesday = createDaySchedule(daysActive[1]),
            wednesday = createDaySchedule(daysActive[2]),
            thursday = createDaySchedule(daysActive[3]),
            friday = createDaySchedule(daysActive[4]),
            saturday = createDaySchedule(daysActive[5]),
            sunday = createDaySchedule(daysActive[6])
        )
    }
}

data class CustomScheduleRange(
    val daysActive: List<Boolean>,
    val openTime: String,
    val closeTime: String
)