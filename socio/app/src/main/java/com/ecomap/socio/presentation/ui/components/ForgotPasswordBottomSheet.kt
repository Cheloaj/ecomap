package com.ecomap.socio.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors

/**
 * Bottom Sheet que aparece cuando el usuario presiona "Olvidé la contraseña"
 * desde CleanPasswordReturningScreen (pantalla de login con contraseña)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordBottomSheet(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón de cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = NuColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            Text(
                text = "Para restablecer tu contraseña, volverás a iniciar sesión.",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NuColors.TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtítulo
            Text(
                text = "Ingresa tu correo y selecciona la opción de 'Olvidé la contraseña'.",
                fontSize = 16.sp,
                color = NuColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Continuar
            NuPrimaryButton(
                text = "Continuar",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Cancelar
            NuSecondaryButton(
                text = "Cancelar",
                onClick = onDismiss,
                textColor = NuColors.Primary
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
