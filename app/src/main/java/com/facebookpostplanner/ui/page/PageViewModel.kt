package com.facebookpostplanner.ui.page

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebookpostplanner.data.PlannerRepository
import com.facebookpostplanner.data.remote.PageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PageViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)

    val selectedPageId: StateFlow<String?> =
        repo.selectedPageId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val selectedPageName: StateFlow<String?> =
        repo.selectedPageName.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val backendUrl: StateFlow<String> =
        repo.backendUrl().stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _pages = MutableStateFlow<List<PageDto>>(emptyList())
    val pages: StateFlow<List<PageDto>> = _pages.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        runCatching { repo.pages() }
            .onSuccess { _pages.value = it }
            .onFailure { _error.value = "Could not reach the backend — check the backend URL below." }
        _loading.value = false
    }

    fun selectPage(page: PageDto) = viewModelScope.launch {
        repo.setPage(page.id, page.name)
    }

    fun addPageManually(id: String, name: String) = viewModelScope.launch {
        if (id.isBlank()) {
            _error.value = "Enter the Page ID from Facebook."
            return@launch
        }
        repo.setPage(id.trim(), name.ifBlank { id.trim() })
        _error.value = null
    }

    fun disconnect() = viewModelScope.launch {
        repo.setPage(null, null)
    }

    fun saveBackendUrl(url: String) = viewModelScope.launch {
        if (url.isBlank()) return@launch
        repo.setBackendUrl(url)
        refresh()
    }

    fun clearError() { _error.value = null }
}
