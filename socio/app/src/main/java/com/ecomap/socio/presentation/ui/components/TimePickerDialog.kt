package com.ecomap.socio.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog
import com.ecomap.socio.presentation.ui.theme.*

@Composable
fun ModernTimePickerDialog(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        showPicker = true
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = showPicker,
            enter = fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = AppleAnimations.springBouncy),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = AppleElevation.floating
            ) {
                Column(
                    modifier = Modifier.padding(AppleSpacing.large)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(AppleSpacing.small)
                                        .size(AppleIconSize.medium),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(AppleSpacing.medium))
                            Text(
                                text = "Seleccionar Hora",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.large))

                    // Time Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeUnitDisplay(
                            value = selectedHour,
                            label = "Hora"
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = AppleSpacing.small)
                        )
                        TimeUnitDisplay(
                            value = selectedMinute,
                            label = "Min"
                        )
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.large))

                    // Time Pickers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Hour Picker
                        TimeUnitPicker(
                            range = 0..23,
                            selectedValue = selectedHour,
                            onValueSelected = { selectedHour = it },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(AppleSpacing.medium))

                        // Minute Picker
                        TimeUnitPicker(
                            range = 0..59,
                            step = 15, // Cada 15 minutos
                            selectedValue = selectedMinute,
                            onValueSelected = { selectedMinute = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppleSpacing.large))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = { onConfirm(selectedHour, selectedMinute) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = AppleElevation.small
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(AppleIconSize.small)
                            )
                            Spacer(modifier = Modifier.width(AppleSpacing.small))
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeUnitDisplay(
    value: Int,
    label: String
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = AppleAnimations.springBouncy,
        label = "time_display_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = AppleElevation.small,
            modifier = Modifier.scale(scale)
        ) {
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = AppleSpacing.large,
                    vertical = AppleSpacing.medium
                )
            )
        }
        Spacer(modifier = Modifier.height(AppleSpacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TimeUnitPicker(
    range: IntRange,
    step: Int = 1,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val values = remember(range, step) {
        range.step(step).toList()
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Fondo de selección
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {}

        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Padding items at top
            items(2) {
                Spacer(modifier = Modifier.height(56.dp))
            }

            items(values) { value ->
                TimePickerItem(
                    value = value,
                    isSelected = value == selectedValue,
                    onClick = { onValueSelected(value) }
                )
            }

            // Padding items at bottom
            items(2) {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

@Composable
fun TimePickerItem(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = AppleAnimations.springBouncy,
        label = "item_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.4f,
        animationSpec = tween(AppleAnimations.DURATION_FAST),
        label = "item_alpha"
    )

    Text(
        text = String.format("%02d", value),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .wrapContentHeight()
            .scale(scale)
            .clickable(onClick = onClick)
            .padding(vertical = AppleSpacing.small),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
