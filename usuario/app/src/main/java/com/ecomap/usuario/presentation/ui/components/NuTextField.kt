package com.ecomap.usuario.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.usuario.ui.theme.AppleColors

/**
 * TextField minimalista estilo Nu
 * Solo línea inferior en gris claro (siempre), sin bordes
 * Placeholder transparente (32.sp), fijo encima de la línea
 */
@Composable
fun NuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val lineColor = Color.Gray.copy(alpha = 0.5f) // Línea siempre gris

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leading icon
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = AppleColors.Gray1, // Gris para consistencia
                    modifier = Modifier.size(24.dp)
                )
            }

            // TextField
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppleColors.Label,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(lineColor),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    decorationBox = { innerTextField ->
                        Box {
                            // Placeholder transparente fijo encima de la línea
                            Text(
                                text = placeholder,
                                fontSize = 32.sp, // Mismo tamaño que el título
                                fontWeight = FontWeight.Bold,
                                color = Color.Transparent, // Invisible
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.offset(y = (-12).dp) // Encima de la línea
                            )
                            innerTextField()
                        }
                    }
                )
            }

            // Trailing icon
            if (trailingIcon != null && onTrailingIconClick != null) {
                IconButton(onClick = onTrailingIconClick) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = AppleColors.Gray1,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Línea inferior, siempre gris
        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = lineColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
