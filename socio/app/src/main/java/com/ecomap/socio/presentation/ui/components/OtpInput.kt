package com.ecomap.socio.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors
import kotlinx.coroutines.delay

/**
 * Componente OTP estilo minimalista con línea inferior
 * 6 campos para código de verificación
 */
@Composable
fun OtpInput(
    otpLength: Int = 6,
    onOtpComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado único para todo el código OTP
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Verificar si está completo
    LaunchedEffect(otpValue) {
        if (otpValue.length == otpLength) {
            onOtpComplete(otpValue)
        }
    }

    // Auto-focus al montar y mostrar teclado
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Cajas VISUALES que muestran cada dígito
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
        ) {
            repeat(otpLength) { index ->
                OtpDigitBox(
                    digit = otpValue.getOrNull(index)?.toString() ?: "",
                    isFocused = index == otpValue.length,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // TextField INVISIBLE que maneja todo el input
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val filteredValue = newValue.filter { it.isDigit() }.take(otpLength)
                otpValue = filteredValue
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.size(0.dp)) {
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Caja visual individual con estilo minimalista
 * Solo línea inferior, sin bordes
 */
@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .background(NuColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Contenedor para el dígito
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (digit.isNotEmpty()) {
                Text(
                    text = digit,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = NuColors.TextPrimary
                )
            } else if (isFocused) {
                // Cursor visual
                Text(
                    text = "|",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.Primary
                )
            }
        }

        // Línea inferior
        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> NuColors.Primary
                digit.isNotBlank() -> NuColors.Primary
                else -> NuColors.TextFieldLine
            }
        )
    }
}
