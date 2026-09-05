package com.ecomap.socio.presentation.ui.offers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecomap.socio.data.model.Offer
import com.ecomap.socio.data.model.UnitType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfferDialog(
    onDismiss: () -> Unit,
    onConfirm: (Offer) -> Unit,
    businessId: String,
    ownerId: String
) {
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(UnitType.KILOGRAMO) }
    var selectedValidity by remember { mutableStateOf("today") }
    var customDate by remember { mutableStateOf<String?>(null) }

    var showUnitDropdown by remember { mutableStateOf(false) }
    var showValidityDropdown by remember { mutableStateOf(false) }
    var showProductSuggestions by remember { mutableStateOf(false) }

    val productSuggestions = listOf(
        "Tomate", "Cebolla", "Papa", "Zanahoria", "Lechuga",
        "Pollo", "Carne de Res", "Pescado", "Huevo",
        "Leche", "Pan", "Tortillas", "Arroz", "Frijol"
    )

    val filteredSuggestions = remember(productName) {
        if (productName.isBlank()) emptyList()
        else productSuggestions.filter { it.contains(productName, ignoreCase = true) }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nueva Oferta") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = {
                            productName = it
                            showProductSuggestions = it.isNotBlank()
                        },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (showProductSuggestions && filteredSuggestions.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                filteredSuggestions.take(5).forEach { suggestion ->
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                productName = suggestion
                                                showProductSuggestions = false
                                            }
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (suggestion != filteredSuggestions.last()) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

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

                ExposedDropdownMenuBox(
                    expanded = showUnitDropdown,
                    onExpandedChange = { showUnitDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitDropdown) }
                    )
                    ExposedDropdownMenu(
                        expanded = showUnitDropdown,
                        onDismissRequest = { showUnitDropdown = false }
                    ) {
                        UnitType.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedUnit = unit
                                    showUnitDropdown = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showValidityDropdown,
                    onExpandedChange = { showValidityDropdown = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedValidity) {
                            "today" -> "Solo Hoy"
                            "weekend" -> "Fin de Semana"
                            "until_stock" -> "Hasta Agotar"
                            "custom_date" -> "Fecha Personalizada"
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vigencia") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showValidityDropdown) }
                    )
                    ExposedDropdownMenu(
                        expanded = showValidityDropdown,
                        onDismissRequest = { showValidityDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Solo Hoy") },
                            onClick = { selectedValidity = "today"; showValidityDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Fin de Semana") },
                            onClick = { selectedValidity = "weekend"; showValidityDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Hasta Agotar") },
                            onClick = { selectedValidity = "until_stock"; showValidityDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Fecha Personalizada") },
                            onClick = { selectedValidity = "custom_date"; showValidityDropdown = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val now = Clock.System.now()
                    val timeZone = TimeZone.currentSystemDefault()

                    val validUntil = when (selectedValidity) {
                        "today" -> now.plus(1, DateTimeUnit.DAY, timeZone).toString()
                        "weekend" -> now.plus(7, DateTimeUnit.DAY, timeZone).toString()
                        "until_stock" -> null
                        "custom_date" -> customDate
                        else -> null
                    }

                    val offer = Offer(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        ownerId = ownerId,
                        productName = productName,
                        price = priceValue,
                        unit = selectedUnit.name.lowercase(),
                        validityType = selectedValidity,
                        validUntil = validUntil,
                        createdAt = now.toString(),
                        updatedAt = now.toString()
                    )
                    onConfirm(offer)
                },
                enabled = productName.isNotBlank() && price.isNotBlank() && price.toDoubleOrNull() != null && price.toDoubleOrNull()!! > 0
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
