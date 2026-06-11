package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.M3uParser
import com.example.data.PlaylistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiziUiState {
    object Loading : DiziUiState
    data class Success(val items: List<PlaylistItem>) : DiziUiState
    data class Error(val message: String) : DiziUiState
}

class DiziViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("dizi_serveri_prefs", Context.MODE_PRIVATE)

    private val _rawUiState = MutableStateFlow<DiziUiState>(DiziUiState.Loading)
    
    // List of favorites stored as URLs
    val favorites = MutableStateFlow<Set<String>>(emptySet())

    val searchQuery = MutableStateFlow("")
    val selectedGroup = MutableStateFlow("Hamısı")
    val selectedVideo = MutableStateFlow<PlaylistItem?>(null)

    init {
        loadFavorites()
        fetchDiziler()
    }

    // Fetches items from the M3U server URL provided by the user
    fun fetchDiziler() {
        viewModelScope.launch {
            _rawUiState.value = DiziUiState.Loading
            try {
                val items = M3uParser.fetchAndParseM3u(
                    "https://raw.githubusercontent.com/faridisgame77-svg/ServerFilmApp82.194.0.345/refs/heads/main/DiziServeri.m3u"
                )
                if (items.isEmpty()) {
                    _rawUiState.value = DiziUiState.Error("Serverdən məlumat çəkmək alınmadı və ya siyahı boşdur.")
                } else {
                    _rawUiState.value = DiziUiState.Success(items)
                }
            } catch (e: Exception) {
                _rawUiState.value = DiziUiState.Error("M3U serverinə qoşulan zaman xəta baş verdi: ${e.localizedMessage}")
            }
        }
    }

    // Combined stream filtering by search query, groups/categories, and favorites tab
    val filteredState: StateFlow<DiziUiState> = combine(
        _rawUiState,
        searchQuery,
        selectedGroup,
        favorites
    ) { state, query, group, favSet ->
        when (state) {
            is DiziUiState.Loading -> DiziUiState.Loading
            is DiziUiState.Error -> state
            is DiziUiState.Success -> {
                var filteredList = state.items

                // Filter by Category
                if (group == "Seçilmişlər") {
                    filteredList = filteredList.filter { favSet.contains(it.url) }
                } else if (group != "Hamısı" && group.isNotEmpty()) {
                    filteredList = filteredList.filter { it.group == group }
                }

                // Filter by Search text query (case insensitive)
                if (query.isNotEmpty()) {
                    filteredList = filteredList.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        (it.group?.contains(query, ignoreCase = true) ?: false)
                    }
                }

                DiziUiState.Success(filteredList)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiziUiState.Loading
    )

    // Extracts all unique non-empty category groups directly from the fetched M3U
    val availableGroups: StateFlow<List<String>> = combine(_rawUiState, favorites) { state, favSet ->
        if (state is DiziUiState.Success) {
            val groups = state.items.mapNotNull { it.group }.distinct().sorted().toMutableList()
            groups.add(0, "Hamısı")
            if (favSet.isNotEmpty()) {
                groups.add(1, "Seçilmişlər")
            }
            groups
        } else {
            listOf("Hamısı")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Hamısı")
    )

    // Favorite Management
    private fun loadFavorites() {
        val favs = sharedPrefs.getStringSet("favorite_urls", emptySet()) ?: emptySet()
        favorites.value = favs
    }

    fun toggleFavorite(item: PlaylistItem) {
        val currentFavs = favorites.value.toMutableSet()
        if (currentFavs.contains(item.url)) {
            currentFavs.remove(item.url)
        } else {
            currentFavs.add(item.url)
        }
        favorites.value = currentFavs
        sharedPrefs.edit().putStringSet("favorite_urls", currentFavs).apply()
    }

    fun isFavorite(item: PlaylistItem): Boolean {
        return favorites.value.contains(item.url)
    }
}
