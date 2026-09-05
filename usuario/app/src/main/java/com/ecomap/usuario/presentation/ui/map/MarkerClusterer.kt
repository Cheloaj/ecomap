package com.ecomap.usuario.presentation.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import androidx.core.content.ContextCompat
import com.ecomap.usuario.R
import com.ecomap.usuario.data.model.Business
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.sqrt

class MarkerClusterer(
    private val context: Context,
    private val businesses: List<Business>,
    private val onBusinessClick: (Business) -> Unit,
    private val onClusterClick: (List<Business>) -> Unit
) : Overlay() {

    private val clusterRadius = 80 // Distancia en píxeles para agrupar markers
    private val clusters = mutableListOf<Cluster>()

    private val paint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    data class Cluster(
        val businesses: MutableList<Business>,
        val position: GeoPoint
    ) {
        fun add(business: Business) {
            businesses.add(business)
        }

        fun size() = businesses.size

        fun center(): GeoPoint {
            var lat = 0.0
            var lon = 0.0
            businesses.forEach {
                lat += it.latitude
                lon += it.longitude
            }
            return GeoPoint(lat / businesses.size, lon / businesses.size)
        }
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null || shadow) return

        // Recalcular clusters según el nivel de zoom actual
        updateClusters(mapView)

        // Dibujar clusters
        clusters.forEach { cluster ->
            val point = Point()
            val projection = mapView.projection
            projection.toPixels(cluster.center(), point)

            if (cluster.size() == 1) {
                // Marker individual
                val marker = ContextCompat.getDrawable(context, R.drawable.ic_location_pin_apple)
                marker?.let {
                    val width = 60
                    val height = 80
                    it.setBounds(
                        point.x - width / 2,
                        point.y - height,
                        point.x + width / 2,
                        point.y
                    )
                    it.draw(canvas)
                }
            } else {
                // Cluster con múltiples markers
                val radius = 50f + (cluster.size() * 2f).coerceAtMost(30f)

                // Círculo de fondo
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, paint)

                // Borde blanco
                val borderPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    isAntiAlias = true
                }
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, borderPaint)

                // Número de negocios
                val yOffset = textPaint.textSize / 3
                canvas.drawText(
                    cluster.size().toString(),
                    point.x.toFloat(),
                    point.y.toFloat() + yOffset,
                    textPaint
                )
            }
        }
    }

    override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false

        val projection = mapView.projection
        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

        // Buscar cluster tocado
        clusters.forEach { cluster ->
            val clusterPoint = Point()
            projection.toPixels(cluster.center(), clusterPoint)

            val distance = sqrt(
                (e.x - clusterPoint.x) * (e.x - clusterPoint.x) +
                (e.y - clusterPoint.y) * (e.y - clusterPoint.y)
            )

            val radius = if (cluster.size() == 1) 40f else 50f + (cluster.size() * 2f).coerceAtMost(30f)

            if (distance < radius) {
                if (cluster.size() == 1) {
                    onBusinessClick(cluster.businesses.first())
                } else {
                    onClusterClick(cluster.businesses)
                }
                return true
            }
        }

        return false
    }

    private fun updateClusters(mapView: MapView) {
        clusters.clear()

        val zoomLevel = mapView.zoomLevelDouble

        // Si el zoom es muy alto, no agrupar
        if (zoomLevel >= 16) {
            businesses.forEach { business ->
                clusters.add(Cluster(
                    mutableListOf(business),
                    GeoPoint(business.latitude, business.longitude)
                ))
            }
            return
        }

        val projection = mapView.projection
        val processedBusinesses = mutableSetOf<Business>()

        businesses.forEach { business1 ->
            if (business1 in processedBusinesses) return@forEach

            val cluster = Cluster(
                mutableListOf(business1),
                GeoPoint(business1.latitude, business1.longitude)
            )
            processedBusinesses.add(business1)

            // Buscar negocios cercanos
            businesses.forEach { business2 ->
                if (business2 in processedBusinesses) return@forEach

                val point1 = Point()
                val point2 = Point()
                projection.toPixels(GeoPoint(business1.latitude, business1.longitude), point1)
                projection.toPixels(GeoPoint(business2.latitude, business2.longitude), point2)

                val distance = sqrt(
                    ((point1.x - point2.x) * (point1.x - point2.x) +
                    (point1.y - point2.y) * (point1.y - point2.y)).toDouble()
                )

                if (distance < clusterRadius) {
                    cluster.add(business2)
                    processedBusinesses.add(business2)
                }
            }

            clusters.add(cluster)
        }
    }
}
