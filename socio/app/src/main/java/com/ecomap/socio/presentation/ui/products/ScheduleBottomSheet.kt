package com.ecomap.socio.presentation.ui.products

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    productName: String,
    productPrice: String,
    productUnit: String,
    productImageUri: Uri?,
    businessOpeningTime: String = "08:00", // ✅ Hora de apertura del negocio
    businessClosingTime: String = "20:00", // ✅ Hora de cierre del negocio
    onSchedule: (LocalDate, String) -> Unit, // ✅ Ahora incluye hora
    onPublishNow: () -> Unit,
    onDismiss: () -> Unit
) {
    // ✅ Estados: 0 = Explicación (50%), 1 = Fecha (100%), 2 = Hora (100%)
    var currentStep by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf(businessOpeningTime) } // ✅ Se ajustará según la fecha seleccionada

    // ✅ Ajustar selectedTime cuando el usuario entra al paso 2 (selección de hora)
    LaunchedEffect(currentStep, selectedDate) {
        if (currentStep == 2 && selectedDate != null) {
            val now = java.time.LocalTime.now()
            val currentHourInMinutes = now.hour * 60 + now.minute
            val (openHour, openMinute) = businessOpeningTime.split(":").map { it.toInt() }
            val businessOpeningInMinutes = openHour * 60 + openMinute

            val isToday = selectedDate == LocalDate.now()

            // Función para redondear al próximo intervalo de 15 minutos
            fun roundToNext15Minutes(minutes: Int): Int {
                val remainder = minutes % 15
                return if (remainder == 0) minutes else minutes + (15 - remainder)
            }

            // Calcular hora mínima con margen de 30 minutos
            val minTimeWithMargin = if (isToday) {
                maxOf(currentHourInMinutes + 30, businessOpeningInMinutes + 30)
            } else {
                businessOpeningInMinutes + 30
            }

            // Redondear al próximo intervalo de 15 minutos
            val minTimeInMinutes = roundToNext15Minutes(minTimeWithMargin)

            // Establecer selectedTime al mínimo permitido
            val hours = minTimeInMinutes / 60
            val minutes = minTimeInMinutes % 60
            selectedTime = String.format("%02d:%02d", hours, minutes)
        }
    }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newValue ->
            // ✅ Bloquear drag manual en pasos 1 y 2
            if (currentStep > 0) {
                // Si está en paso 1 o 2, NO permitir cambiar a PartiallyExpanded o Hidden
                newValue == SheetValue.Expanded
            } else {
                // En paso 0, permitir cualquier cambio
                true
            }
        }
    )

    // ✅ Controlar altura del sheet según el paso
    LaunchedEffect(currentStep) {
        if (currentStep > 0) {
            sheetState.expand()
        } else {
            sheetState.partialExpand()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // ✅ Sin drag handle para evitar drag manual
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (currentStep == 0) {
                        Modifier.height(350.dp)
                    } else {
                        Modifier.fillMaxSize() // ✅ Ocupa toda la pantalla
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Botón de volver atrás (solo visible en pasos 1 y 2)
                if (currentStep > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = {
                                if (currentStep == 1) {
                                    currentStep = 0 // Volver a explicación
                                } else if (currentStep == 2) {
                                    currentStep = 1 // Volver a selección de fecha
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

            when (currentStep) {
                0 -> {
                // ============================================
                // ESTADO 1: EXPLICACIÓN (50% altura)
                // ============================================

                // 🎨 Icono decorativo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 📄 Título
                Text(
                    text = "¿Cuándo publicar tu producto?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 📝 Descripción
                Text(
                    text = "Puedes publicarlo ahora mismo o programarlo para una fecha futura (hasta 30 días).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 🎯 Botones de acción
                Button(
                    onClick = {
                        currentStep = 1 // ✅ Ir a selección de fecha
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sí, programar para después",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        onPublishNow()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No, publicar ahora",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                }

                1 -> {
                // ============================================
                // PASO 2: SELECCIÓN DE FECHA (100% altura)
                // ============================================

                // 📦 Resumen del producto - Compacto y elegante
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Imagen del producto (compacta)
                    if (productImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(productImageUri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Nombre + Precio
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$$productPrice / $productUnit",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 📅 Selector de fecha
                Text(
                    text = "Selecciona la fecha de publicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                DateRangePicker(
                    maxDays = 30,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = date
                    },
                    businessClosingTime = businessClosingTime // ✅ Pasar hora de cierre
                )

                // ✅ Empujar botón al fondo
                Spacer(modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Botón "Siguiente"
                Button(
                    onClick = {
                        currentStep = 2 // ✅ Ir a selección de hora
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedDate != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (selectedDate != null) "Siguiente" else "Selecciona una fecha",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                }

                2 -> {
                // ============================================
                // PASO 3: SELECCIÓN DE HORA (100% altura)
                // ============================================

                // 📦 Resumen del producto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (productImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(productImageUri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$$productPrice $productUnit",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ⏰ Selector de hora minimalista
                Text(
                    text = "Hora de publicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ⏰ Selector de hora con Slider minimalista
                val (openHour, openMinute) = businessOpeningTime.split(":").map { it.toInt() }
                val (closeHour, closeMinute) = businessClosingTime.split(":").map { it.toInt() }

                // ✅ VALIDACIÓN MEJORADA: Margen de 30 min + Intervalos de 15 min
                val now = java.time.LocalTime.now()
                val currentHourInMinutes = now.hour * 60 + now.minute
                val businessOpeningInMinutes = openHour * 60 + openMinute
                val businessClosingInMinutes = closeHour * 60 + closeMinute

                val isToday = selectedDate == LocalDate.now()

                // Función para redondear al próximo intervalo de 15 minutos
                fun roundToNext15Minutes(minutes: Int): Int {
                    val remainder = minutes % 15
                    return if (remainder == 0) minutes else minutes + (15 - remainder)
                }

                // Calcular hora mínima con margen de 30 minutos
                val minTimeWithMargin = if (isToday) {
                    // Si es HOY → hora actual + 30 min O apertura + 30 min (el mayor)
                    maxOf(currentHourInMinutes + 30, businessOpeningInMinutes + 30)
                } else {
                    // Si es FUTURO → apertura + 30 min
                    businessOpeningInMinutes + 30
                }

                // Redondear al próximo intervalo de 15 minutos
                val minTimeInMinutes = roundToNext15Minutes(minTimeWithMargin)

                // Hora máxima = cierre - 30 minutos
                val maxTimeInMinutes = businessClosingInMinutes - 30

                val currentTimeInMinutes = selectedTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hora seleccionada (grande)
                    Text(
                        text = selectedTime,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ Rango permitido (dinámico según si es HOY o futuro)
                    val minHour = minTimeInMinutes / 60
                    val minMinute = minTimeInMinutes % 60
                    val displayMinTime = String.format("%02d:%02d", minHour, minMinute)

                    Text(
                        text = if (isToday) {
                            "Horario disponible hoy: $displayMinTime - $businessClosingTime"
                        } else {
                            "Horario: $businessOpeningTime - $businessClosingTime"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Slider minimalista
                    Slider(
                        value = currentTimeInMinutes.toFloat(),
                        onValueChange = { newValue ->
                            val totalMinutes = newValue.toInt()
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60
                            selectedTime = String.format("%02d:%02d", hours, minutes)
                        },
                        valueRange = minTimeInMinutes.toFloat()..maxTimeInMinutes.toFloat(),
                        steps = ((maxTimeInMinutes - minTimeInMinutes) / 15) - 1, // Cada 15 minutos
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // ✅ Empujar SwipeButton al fondo
                Spacer(modifier = Modifier.weight(1f))

                // ✅ Swipe Button para confirmar (al fondo)
                SwipeButton(
                    text = "Desliza para agendar",
                    onSwipeComplete = {
                        selectedDate?.let { date ->
                            onSchedule(date, selectedTime)
                            onDismiss()
                        }
                    }
                )
                }
            }
            }
        }
    }
}
