package com.ecomap.socio.presentation.ui.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecomap.socio.data.model.Offer

@Composable
fun EditOfferDialog(
    offer: Offer,
    onDismiss: () -> Unit,
    onConfirm: (Offer) -> Unit
) {
    var productName by remember { mutableStateOf(offer.productName) }
    var price by remember { mutableStateOf(offer.price.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Oferta") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                            price = newValue
                        }
                    },
                    label = { Text("Precio") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("$") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedOffer = offer.copy(
                        productName = productName,
                        price = price.toDoubleOrNull() ?: offer.price
                    )
                    onConfirm(updatedOffer)
                },
                enabled = productName.isNotBlank() && price.isNotBlank() && price.toDoubleOrNull() != null
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

