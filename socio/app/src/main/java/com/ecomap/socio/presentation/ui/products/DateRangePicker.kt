package com.ecomap.socio.presentation.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DateRangePicker(
    maxDays: Int = 30,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    businessClosingTime: String = "20:00", // ✅ Hora de cierre del negocio
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val maxDate = today.plusDays(maxDays.toLong())

    // ✅ Verificar si el negocio ya cerró hoy
    val currentTime = LocalTime.now()
    val (closeHour, closeMinute) = businessClosingTime.split(":").map { it.toInt() }
    val closingTime = LocalTime.of(closeHour, closeMinute)
    val isBusinessClosed = currentTime.isAfter(closingTime)

    // ✅ Fecha mínima: Si el negocio cerró, desde mañana. Si no, desde hoy.
    val minDate = if (isBusinessClosed) today.plusDays(1) else today

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // 📅 Header con mes y año
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val previousMonth = currentMonth.minusMonths(1)
                    // Solo permitir retroceder si el mes anterior incluye la fecha mínima
                    if (previousMonth.atDay(1) >= minDate.withDayOfMonth(1) ||
                        previousMonth.atEndOfMonth() >= minDate) {
                        currentMonth = previousMonth
                    }
                },
                enabled = currentMonth > YearMonth.from(minDate)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Mes anterior"
                )
            }

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es"))} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val nextMonth = currentMonth.plusMonths(1)
                    // Solo permitir avanzar si el siguiente mes está dentro del rango
                    if (nextMonth.atDay(1) <= maxDate) {
                        currentMonth = nextMonth
                    }
                },
                enabled = currentMonth < YearMonth.from(maxDate)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Mes siguiente"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📆 Días de la semana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("D", "L", "M", "M", "J", "V", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🗓️ Grid de días
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Domingo

        val days = mutableListOf<LocalDate?>()

        // Espacios vacíos antes del primer día
        repeat(firstDayOfWeek) {
            days.add(null)
        }

        // Días del mes
        for (day in 1..daysInMonth) {
            days.add(currentMonth.atDay(day))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days) { date ->
                if (date != null) {
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isInRange = date >= minDate && date <= maxDate, // ✅ Usar minDate en lugar de today
                        isToday = date == today,
                        daysUntil = if (date >= minDate) ChronoUnit.DAYS.between(minDate, date).toInt() else -1,
                        onClick = {
                            if (date >= minDate && date <= maxDate) { // ✅ Usar minDate
                                onDateSelected(date)
                            }
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }

        // ℹ️ Información de días restantes
        if (selectedDate != null) {
            Spacer(modifier = Modifier.height(16.dp))

            val daysUntil = ChronoUnit.DAYS.between(minDate, selectedDate).toInt() // ✅ Usar minDate

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        selectedDate == today && !isBusinessClosed -> "Se publicará hoy"
                        daysUntil == 0 -> "Se publicará mañana" // Si minDate es mañana
                        daysUntil == 1 -> if (isBusinessClosed) "Se publicará en 2 días" else "Se publicará mañana"
                        else -> "Se publicará en ${daysUntil + if (isBusinessClosed) 1 else 0} días"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isInRange: Boolean,
    isToday: Boolean,
    daysUntil: Int,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        !isInRange -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // ✅ Fondo gris para días deshabilitados
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        !isInRange -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) // ✅ Texto gris tenue
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isInRange && !isSelected) {
                    Modifier.border(
                        width = if (isToday) 2.dp else 0.dp,
                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                } else Modifier
            )
            .clickable(enabled = isInRange, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
