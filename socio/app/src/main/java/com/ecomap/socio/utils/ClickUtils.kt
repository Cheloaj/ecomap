package com.ecomap.socio.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🛡️ Modifier para prevenir múltiples clicks rápidos
 *
 * Uso:
 * ```
 * Button(
 *     modifier = Modifier.debouncedClickable {
 *         navController.navigate("...")
 *     }
 * )
 * ```
 */
fun Modifier.debouncedClickable(
    debounceTime: Long = 500L, // 500ms = medio segundo
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(0L) }

    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 * 🛡️ Composable para clicks únicos (previene doble submit)
 *
 * Uso en Button/Card:
 * ```
 * val onClickOnce = rememberDebouncedClick {
 *     viewModel.createProduct(...)
 * }
 *
 * Button(onClick = onClickOnce) { ... }
 * ```
 */
@Composable
fun rememberDebouncedClick(
    debounceTime: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableStateOf(0L) }

    return remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceTime) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}

/**
 * 🔄 Hook para operaciones que no deben ejecutarse múltiples veces
 *
 * Útil para:
 * - Navegación
 * - Submit de formularios
 * - Creación de recursos
 *
 * Uso:
 * ```
 * val executeOnce = rememberSingleExecution()
 *
 * Button(onClick = {
 *     executeOnce {
 *         navController.navigate("detail")
 *     }
 * })
 * ```
 */
@Composable
fun rememberSingleExecution(cooldown: Long = 500L): (suspend () -> Unit) -> Unit {
    var isExecuting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    return remember(scope) {
        { action ->
            if (!isExecuting) {
                isExecuting = true
                scope.launch {
                    action()
                    delay(cooldown)
                    isExecuting = false
                }
            }
        }
    }
}
