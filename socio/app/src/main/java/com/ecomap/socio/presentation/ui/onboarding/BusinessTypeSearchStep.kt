package com.ecomap.socio.presentation.ui.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors

data class BusinessCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

val businessCategories = listOf(
    BusinessCategory("abarrotes", "Abarrotes", Icons.Default.ShoppingCart, "Tienda de abarrotes general"),
    BusinessCategory("carniceria", "Carnicería", Icons.Default.Restaurant, "Carnes y embutidos"),
    BusinessCategory("panaderia", "Panadería", Icons.Default.Cake, "Pan y productos de panadería"),
    BusinessCategory("fruteria", "Frutería", Icons.Default.LocalFlorist, "Frutas y verduras frescas"),
    BusinessCategory("polleria", "Pollería", Icons.Default.Restaurant, "Venta de pollo"),
    BusinessCategory("lacteos", "Lácteos", Icons.Default.LocalDrink, "Leche, queso, yogurt"),
    BusinessCategory("pescaderia", "Pescadería", Icons.Default.Restaurant, "Pescados y mariscos"),
    BusinessCategory("tortilleria", "Tortillería", Icons.Default.Restaurant, "Tortillas y derivados"),
    BusinessCategory("cremeria", "Cremería", Icons.Default.LocalDrink, "Quesos y lácteos"),
    BusinessCategory("semillas", "Semillas", Icons.Default.Grass, "Semillas y granos"),
    BusinessCategory("especias", "Especias", Icons.Default.Restaurant, "Especias y condimentos"),
    BusinessCategory("dulceria", "Dulcería", Icons.Default.Cake, "Dulces y golosinas"),
    BusinessCategory("bebidas", "Bebidas", Icons.Default.LocalBar, "Refrescos y bebidas"),
    BusinessCategory("botanas", "Botanas", Icons.Default.Fastfood, "Snacks y botanas"),
    BusinessCategory("organicos", "Orgánicos", Icons.Default.Eco, "Productos orgánicos"),
    BusinessCategory("congelados", "Congelados", Icons.Default.AcUnit, "Productos congelados"),
    BusinessCategory("higiene", "Higiene", Icons.Default.CleaningServices, "Productos de limpieza"),
    BusinessCategory("mascotas", "Mascotas", Icons.Default.Pets, "Alimento para mascotas"),
    BusinessCategory("otro", "Otro", Icons.Default.Store, "Otro tipo de negocio")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessTypeSearchStep(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            businessCategories
        } else {
            businessCategories.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Selecciona tu Categoría",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Elige el tipo de negocio que mejor te describa",
            fontSize = 16.sp,
            color = NuColors.TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(23.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isFocused) {
                    NuColors.Primary.copy(alpha = 0.5f)
                } else {
                    NuColors.TextSecondary.copy(alpha = 0.2f)
                }
            )
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = NuColors.TextPrimary
                ),
                cursorBrush = SolidColor(NuColors.Primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NuColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Buscar categoría...",
                                    fontSize = 14.sp,
                                    color = NuColors.TextSecondary.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }

                        if (isFocused) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = NuColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCategories) { category ->
                val isSelected = selectedType.lowercase() == category.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onTypeSelected(category.id)
                            focusManager.clearFocus()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NuColors.Primary.copy(alpha = 0.1f) else NuColors.Surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) NuColors.Primary else NuColors.Surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (isSelected) NuColors.Primary else NuColors.TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) NuColors.Primary else NuColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category.description,
                                fontSize = 13.sp,
                                color = NuColors.TextSecondary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = NuColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (filteredCategories.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = NuColors.TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron categorías",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NuColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
