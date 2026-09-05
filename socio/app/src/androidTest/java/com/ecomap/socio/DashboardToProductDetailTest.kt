package com.ecomap.socio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de UI instrumentada del flujo principal de EcoMap Socio.
 *
 * Recorrido que valida:
 *   Dashboard ("Mis Negocios")  ->  scroll de la lista
 *                               ->  abrir un negocio
 *                               ->  pantalla de productos
 *                               ->  abrir el detalle de un producto
 *
 * Alcance: es una prueba end-to-end. Levanta la MainActivity real con el grafo
 * de Hilt real y consulta el Supabase real, así que valida también la capa de
 * red y el mapeo de PostgREST a los modelos. A cambio, necesita:
 *   - dispositivo con red,
 *   - sesión iniciada (socio.demo@ecomap.mx),
 *   - los datos semilla de init_demo_db.sql.
 *
 * Para una prueba hermética habría que inyectar un SupabaseClient falso con
 * @TestInstallIn sobre AppModule; se dejó fuera a propósito porque el objetivo
 * aquí es demostrar el flujo completo funcionando.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DashboardToProductDetailTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Espera activa a que un texto aparezca en el árbol de semántica.
     * Se usa en lugar de waitForIdle() porque la app tiene animaciones de carga
     * (skeletons) que nunca dejan la composición en reposo, y porque los datos
     * llegan por red con latencia variable.
     */
    private fun awaitText(text: String, timeoutMillis: Long = TIMEOUT) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Pausa de presentación entre pasos.
     *
     * No aporta nada a la validación: existe solo para que el recorrido sea
     * legible al grabarlo en video. Se controla con -Pdemo=true para que en CI
     * el test siga corriendo a velocidad completa.
     */
    private fun pausaDemo(millis: Long = 1_500) {
        if (MODO_DEMO) Thread.sleep(millis)
    }

    @Test
    fun dashboard_haceScroll_abreNegocio_yLlegaAlDetalleDelProducto() {

        // ---- 1. El dashboard terminó de cargar ----------------------------
        awaitText(TITULO_DASHBOARD)
        compose.onNodeWithText(BUSCADOR_NEGOCIOS).assertIsDisplayed()
        awaitText(NEGOCIO)
        pausaDemo(2_500)

        // ---- 2. Scroll sobre la lista de negocios --------------------------
        // onAllNodes + onFirst porque el árbol tiene más de un contenedor
        // desplazable (la lista y la fila de tabs).
        val listaNegocios = compose.onAllNodes(hasScrollAction()).onFirst()
        listaNegocios.performTouchInput { swipeUp() }
        pausaDemo()
        listaNegocios.performTouchInput { swipeDown() }
        pausaDemo()

        // El negocio sigue visible después del scroll.
        awaitText(NEGOCIO)

        // ---- 3. Abrir el negocio -------------------------------------------
        compose.onAllNodesWithText(NEGOCIO, substring = true).onFirst().performClick()

        // ---- 4. Pantalla de productos --------------------------------------
        awaitText(TAB_PROGRAMADOS)
        compose.onAllNodesWithText(TAB_PRODUCTOS).onFirst().assertIsDisplayed()
        awaitText(PRODUCTO)
        pausaDemo(3_000)

        // ---- 5. Abrir el detalle del producto ------------------------------
        compose.onAllNodesWithText(PRODUCTO, substring = true).onFirst().performClick()

        // ---- 6. Verificar el detalle ---------------------------------------
        awaitText(TITULO_DETALLE)
        pausaDemo(3_500)
        compose.onAllNodesWithText(PRODUCTO, substring = true).onFirst().assertIsDisplayed()
        compose.onAllNodesWithText(PRECIO, substring = true).onFirst().assertIsDisplayed()
        compose.onAllNodesWithText(DESCRIPCION, substring = true).onFirst().assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT = 40_000L

        /** Se activa con: gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.demo=true */
        val MODO_DEMO: Boolean =
            androidx.test.platform.app.InstrumentationRegistry
                .getArguments()
                .getString("demo") == "true"

        const val TITULO_DASHBOARD = "Mis Negocios"
        const val BUSCADOR_NEGOCIOS = "Buscar negocios..."
        const val NEGOCIO = "Abarrotes La Esperanza"

        const val TAB_PRODUCTOS = "Productos"
        const val TAB_PROGRAMADOS = "Programados"

        const val PRODUCTO = "Arroz Súper Extra 1 kg"
        const val TITULO_DETALLE = "Detalles del Producto"
        const val PRECIO = "28.50"
        const val DESCRIPCION = "Arroz blanco grano largo"
    }
}
