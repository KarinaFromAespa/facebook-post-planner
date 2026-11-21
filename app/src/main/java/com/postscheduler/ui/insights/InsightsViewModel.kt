package com.postscheduler.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.postscheduler.data.PlannerRepository
import com.postscheduler.data.remote.InsightsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)

    private val _monetized = MutableStateFlow<Boolean?>(null)
    val monetized: StateFlow<Boolean?> = _monetized.asStateFlow()
    private val _criteria = MutableStateFlow<String?>(null)
    val criteria: StateFlow<String?> = _criteria.asStateFlow()
    private val _totalEarnings = MutableStateFlow<Double?>(null)
    val totalEarnings: StateFlow<Double?> = _totalEarnings.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        // The earnings belong to the currently selected Page — pass its id
        // so switching Pages shows that Page's total instead of the first
        // connected Page's.
        val dto: InsightsDto? = runCatching { repo.refreshInsights(repo.currentPageId()) }.getOrNull()
        dto?.let {
            _monetized.value = it.monetized
            _criteria.value = it.monetizationCriteria
            _totalEarnings.value = it.totalEarningsUsd
        }
    }
}
