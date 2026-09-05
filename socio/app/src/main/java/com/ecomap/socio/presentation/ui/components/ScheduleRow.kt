package com.ecomap.socio.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecomap.socio.data.model.DaySchedule
import com.ecomap.socio.presentation.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernScheduleRow(
    dayName: String,
    schedule: DaySchedule,
    onScheduleChange: (DaySchedule) -> Unit,
    index: Int = 0
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var editingOpenTime by remember { mutableStateOf(true) }
    var isVisible by remember { mutableStateOf(false) }

    // Animación escalonada de entrada
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 50).toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AppleAnimations.DURATION_NORMAL,
                easing = FastOutSlowInEasing
            )
        ) + slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(
                durationMillis = AppleAnimations.DURATION_NORMAL,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppleSpacing.extraSmall),
            shape = MaterialTheme.shapes.large,
            color = if (schedule.isOpen)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shadowElevation = if (schedule.isOpen) AppleElevation.small else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleSpacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (schedule.isOpen) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(AppleSpacing.small))
                    }

                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (schedule.isOpen) FontWeight.Bold else FontWeight.Normal,
                        color = if (schedule.isOpen)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Switch or Time display
                AnimatedContent(
                    targetState = schedule.isOpen,
                    transitionSpec = {
                        if (targetState) {
                            fadeIn() + slideInHorizontally(initialOffsetX = { it }) with
                                    fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
                        } else {
                            fadeIn() + slideInHorizontally(initialOffsetX = { -it }) with
                                    fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                        }.using(SizeTransform(clip = false))
                    },
                    label = "schedule_content"
                ) { isOpen ->
                    if (isOpen) {
                        // Time buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mostrar solo el primer turno (este componente no soporta horario partido)
                            val firstShift = schedule.shifts.firstOrNull()

                            TimeButton(
                                time = firstShift?.openTime ?: "09:00",
                                onClick = {
                                    editingOpenTime = true
                                    showTimePicker = true
                                }
                            )

                            Text(
                                text = "-",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            TimeButton(
                                time = firstShift?.closeTime ?: "18:00",
                                onClick = {
                                    editingOpenTime = false
                                    showTimePicker = true
                                }
                            )

                            Spacer(modifier = Modifier.width(AppleSpacing.small))

                            // Toggle off button
                            Surface(
                                onClick = {
                                    onScheduleChange(
                                        schedule.copy(
                                            isOpen = false,
                                            shifts = emptyList()
                                        )
                                    )
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Cerrar",
                                    modifier = Modifier
                                        .padding(AppleSpacing.extraSmall)
                                        .size(AppleIconSize.small),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        // Toggle on button
                        Surface(
                            onClick = {
                                // Al abrir, crear un turno por defecto
                                val defaultShift = com.ecomap.socio.data.model.TimeRange(
                                    openTime = "09:00",
                                    closeTime = "18:00"
                                )
                                onScheduleChange(
                                    schedule.copy(
                                        isOpen = true,
                                        shifts = listOf(defaultShift)
                                    )
                                )
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Abrir",
                                modifier = Modifier.padding(
                                    horizontal = AppleSpacing.medium,
                                    vertical = AppleSpacing.small
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val firstShift = schedule.shifts.firstOrNull()
        val currentTime = if (editingOpenTime) {
            firstShift?.openTime ?: "09:00"
        } else {
            firstShift?.closeTime ?: "18:00"
        }
        val parts = currentTime.split(":")
        val hour = parts[0].toIntOrNull() ?: 9
        val minute = parts[1].toIntOrNull() ?: 0

        ModernTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { newHour, newMinute ->
                val newTime = String.format("%02d:%02d", newHour, newMinute)

                // Actualizar el primer turno o crear uno nuevo
                val updatedShift = if (editingOpenTime) {
                    com.ecomap.socio.data.model.TimeRange(
                        openTime = newTime,
                        closeTime = firstShift?.closeTime ?: "18:00"
                    )
                } else {
                    com.ecomap.socio.data.model.TimeRange(
                        openTime = firstShift?.openTime ?: "09:00",
                        closeTime = newTime
                    )
                }

                onScheduleChange(schedule.copy(shifts = listOf(updatedShift)))
                showTimePicker = false
            }
        )
    }
}

@Composable
fun TimeButton(
    time: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = AppleAnimations.springBouncy,
        label = "time_button_scale"
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppleElevation.small,
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppleSpacing.medium,
                vertical = AppleSpacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.extraSmall)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(AppleIconSize.small),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Composable para mostrar todos los días de la semana con el nuevo diseño
 */
@Composable
fun ModernWeekSchedule(
    operatingHours: com.ecomap.socio.data.model.OperatingHours,
    onScheduleChange: (String, DaySchedule) -> Unit
) {
    val days = listOf(
        "Lunes" to operatingHours.monday,
        "Martes" to operatingHours.tuesday,
        "Miércoles" to operatingHours.wednesday,
        "Jueves" to operatingHours.thursday,
        "Viernes" to operatingHours.friday,
        "Sábado" to operatingHours.saturday,
        "Domingo" to operatingHours.sunday
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppleSpacing.small)
    ) {
        days.forEachIndexed { index, (dayName, schedule) ->
            ModernScheduleRow(
                dayName = dayName,
                schedule = schedule,
                onScheduleChange = { newSchedule ->
                    onScheduleChange(dayName.lowercase(), newSchedule)
                },
                index = index
            )
        }
    }
}
