package com.ecomap.socio.presentation.ui.subscription

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.data.model.PaymentInfo
import com.ecomap.socio.ui.theme.NuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentComplete: (PaymentInfo) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Focus requesters para auto-avance
    val expiryMonthFocus = remember { FocusRequester() }
    val expiryYearFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Finalizar pago",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1F1F1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Resumen minimalista
            MinimalPlanSummary()

            // Formulario de tarjeta limpio
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Información de pago",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F1F1F)
                )

                // Número de tarjeta
                MinimalTextField(
                    value = cardNumber,
                    onValueChange = {
                        val digits = it.replace(" ", "").take(16)
                        cardNumber = digits.chunked(4).joinToString(" ")

                        // Auto-avance cuando completa 16 dígitos
                        if (digits.length == 16) {
                            expiryMonthFocus.requestFocus()
                        }
                    },
                    label = "Número de tarjeta",
                    placeholder = "1234 5678 9012 3456",
                    leadingIcon = Icons.Default.CreditCard,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { expiryMonthFocus.requestFocus() }
                    )
                )

                // Fila de fecha y CVV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mes de expiración
                    MinimalTextField(
                        value = expiryMonth,
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                expiryMonth = it
                                // Auto-avance después de 2 dígitos
                                if (it.length == 2) {
                                    expiryYearFocus.requestFocus()
                                }
                            }
                        },
                        label = "Mes",
                        placeholder = "12",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { expiryYearFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(expiryMonthFocus)
                    )

                    // Año de expiración
                    MinimalTextField(
                        value = expiryYear,
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                expiryYear = it
                                // Auto-avance después de 2 dígitos
                                if (it.length == 2) {
                                    cvvFocus.requestFocus()
                                }
                            }
                        },
                        label = "Año",
                        placeholder = "25",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { cvvFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(expiryYearFocus)
                    )

                    // CVV
                    MinimalTextField(
                        value = cvv,
                        onValueChange = {
                            if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                cvv = it
                                // Auto-avance después de 3 dígitos
                                if (it.length == 3) {
                                    nameFocus.requestFocus()
                                }
                            }
                        },
                        label = "CVV",
                        placeholder = "123",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { nameFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .focusRequester(cvvFocus)
                    )
                }

                // Nombre del titular
                MinimalTextField(
                    value = cardHolderName,
                    onValueChange = {
                        cardHolderName = it.filter { char ->
                            char.isLetter() || char == ' '
                        }.take(50).uppercase()
                    },
                    label = "Nombre del titular",
                    placeholder = "JUAN PÉREZ",
                    leadingIcon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.focusRequester(nameFocus)
                )

                // Mensaje de error animado
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let { error ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF3F3))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFE53935),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicador de seguridad minimalista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pago 100% seguro y encriptado",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }

            // Botón de pago minimalista
            Button(
                onClick = {
                    val paymentInfo = PaymentInfo(
                        cardNumber = cardNumber,
                        expiryMonth = expiryMonth,
                        expiryYear = expiryYear,
                        cvv = cvv,
                        cardHolderName = cardHolderName
                    )

                    if (paymentInfo.isValid()) {
                        errorMessage = null
                        isProcessing = true
                        onPaymentComplete(paymentInfo)
                    } else {
                        errorMessage = "Verifica los datos de tu tarjeta"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F1F1F),
                    disabledContainerColor = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Pagar $29.00",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Disclaimer minimalista
            Text(
                text = "Tu suscripción se renovará automáticamente cada mes. Puedes cancelar en cualquier momento.",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MinimalPlanSummary() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3E5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Plan PRO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
                Text(
                    text = "Mensual",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
        Text(
            text = "$29.00",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
        )
    }
}

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFFCCCCCC)
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1F1F1F),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF1F1F1F)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
