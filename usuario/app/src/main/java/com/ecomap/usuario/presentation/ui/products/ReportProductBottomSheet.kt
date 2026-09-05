package com.ecomap.usuario.presentation.ui.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ecomap.usuario.data.model.ComplaintReason
import com.ecomap.usuario.data.model.Product
import com.ecomap.usuario.presentation.viewmodel.ComplaintUiState
import com.ecomap.usuario.presentation.viewmodel.ProductComplaintViewModel

/**
 * Bottom Sheet para reportar problemas con productos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProductBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    viewModel: ProductComplaintViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedReason by remember { mutableStateOf<ComplaintReason?>(null) }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showReasonDropdown by remember { mutableStateOf(false) }

    val complaintState by viewModel.complaintState.collectAsState()

    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Manejar estados de éxito/error
    LaunchedEffect(complaintState) {
        when (complaintState) {
            is ComplaintUiState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    (complaintState as ComplaintUiState.Success).message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.resetState()
                onDismiss()
            }
            is ComplaintUiState.Error -> {
                android.widget.Toast.makeText(
                    context,
                    (complaintState as ComplaintUiState.Error).message,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            else -> {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Reportar problema",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Descripción del feature
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Si encontraste algún problema con este producto (precio incorrecto, mala calidad, producto vencido, etc.), repórtalo aquí y será revisado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Información del producto
            Text(
                text = "Producto: ${product.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Divider()

            // Selección de motivo
            Text(
                text = "Motivo del reporte *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = showReasonDropdown,
                onExpandedChange = { showReasonDropdown = !showReasonDropdown }
            ) {
                OutlinedTextField(
                    value = selectedReason?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona un motivo") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (showReasonDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = showReasonDropdown,
                    onDismissRequest = { showReasonDropdown = false }
                ) {
                    ComplaintReason.values().forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason.displayName) },
                            onClick = {
                                selectedReason = reason
                                showReasonDropdown = false
                            }
                        )
                    }
                }
            }

            // Descripción detallada (opcional)
            Text(
                text = "Descripción (opcional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Describe el problema con más detalle...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            // Imagen de evidencia
            Text(
                text = "Foto de evidencia (opcional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (selectedImageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Evidencia",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar imagen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Agregar foto *",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        when {
                            selectedReason == null -> {
                                android.widget.Toast.makeText(
                                    context,
                                    "Por favor selecciona un motivo",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            description.isBlank() -> {
                                android.widget.Toast.makeText(
                                    context,
                                    "Por favor escribe una descripción",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedImageUri == null -> {
                                android.widget.Toast.makeText(
                                    context,
                                    "Por favor agrega una foto de evidencia",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                viewModel.createComplaint(
                                    productId = product.id,
                                    reason = selectedReason!!.displayName,
                                    description = description,
                                    imageUri = selectedImageUri
                                ) { _, _ -> }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedReason != null && complaintState !is ComplaintUiState.Loading
                ) {
                    if (complaintState is ComplaintUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Enviar reporte")
                    }
                }
            }
        }
    }
}
