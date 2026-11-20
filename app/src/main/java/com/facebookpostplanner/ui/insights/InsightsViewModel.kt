package com.facebookpostplanner.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebookpostplanner.data.PlannerRepository
import com.facebookpostplanner.data.remote.InsightsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)
    val localInsights = repo.insights()
    val published = repo.published()

    private val _monetized = MutableStateFlow<Boolean?>(null)
    val monetized: StateFlow<Boolean?> = _monetized.asStateFlow()
    private val _criteria = MutableStateFlow<String?>(null)
    val criteria: StateFlow<String?> = _criteria.asStateFlow()
    private val _totalEarnings = MutableStateFlow<Double?>(null)
    val totalEarnings: StateFlow<Double?> = _totalEarnings.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val dto: InsightsDto? = runCatching { repo.refreshInsights() }.getOrNull()
        dto?.let {
            _monetized.value = it.monetized
            _criteria.value = it.monetizationCriteria
            _totalEarnings.value = it.totalEarningsUsd
        }
    }
}
