package com.ecomap.usuario.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    private val _favoritesFlow = MutableStateFlow(getFavorites())
    val favoritesFlow: Flow<Set<String>> = _favoritesFlow

    private fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorite_business_ids", emptySet()) ?: emptySet()
    }

    fun isFavorite(businessId: String): Boolean {
        return getFavorites().contains(businessId)
    }

    fun toggleFavorite(businessId: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.contains(businessId)) {
            favorites.remove(businessId)
        } else {
            favorites.add(businessId)
        }
        prefs.edit().putStringSet("favorite_business_ids", favorites).apply()
        _favoritesFlow.value = favorites
    }

    fun getFavoritesList(): List<String> {
        return getFavorites().toList()
    }
}
