package com.ecomap.socio.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 🖼️ Configuración optimizada de Coil para carga de imágenes
 *
 * Beneficios:
 * - Caché en memoria (25% de RAM disponible)
 * - Caché en disco (2% de almacenamiento)
 * - Precarga automática
 * - Compresión de imágenes
 */
object ImageConfig {

    private var imageLoader: ImageLoader? = null

    /**
     * Obtener ImageLoader singleton con configuración optimizada
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: createImageLoader(context).also { imageLoader = it }
        }
    }

    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // 💾 Caché en memoria (25% de RAM)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% de RAM disponible
                    .strongReferencesEnabled(true) // Previene recolección de basura prematura
                    .build()
            }
            // 💿 Caché en disco (2% de almacenamiento)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% de almacenamiento
                    .build()
            }
            // 🌐 Cliente HTTP optimizado
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            // 📱 Políticas de caché
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // ⚡ Optimizaciones
            .crossfade(true) // Transición suave
            .crossfade(300) // 300ms de fade
            .respectCacheHeaders(false) // Ignorar headers de servidor para mejor caché local
            .build()
    }

    /**
     * Limpiar caché de imágenes (útil para logout o liberar memoria)
     */
    fun clearCache(context: Context) {
        imageLoader?.memoryCache?.clear()
        imageLoader?.diskCache?.clear()
    }
}
