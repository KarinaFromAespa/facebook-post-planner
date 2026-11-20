package com.facebookpostplanner.ui.publish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebookpostplanner.data.PlannerRepository
import com.facebookpostplanner.data.local.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class PublishViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)
    val queue = repo.queue()
    val drafts = repo.drafts()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { _paused.value = repo.queuePaused() }
            runCatching { repo.refreshQueue() }.onFailure { _error.value = it.message }
        }
    }

    /** Hourly slots 08:00–18:00 for today, as millis. */
    fun todaySlots(): List<Long> {
        val cal = Calendar.getInstance()
        return (8..18).map { hour ->
            cal.apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        }
    }

    fun togglePause() = viewModelScope.launch {
        val target = !_paused.value
        runCatching { repo.setQueuePaused(target) }
            .onSuccess { _paused.value = target }
            .onFailure { _error.value = it.message }
    }

    fun publishNow(post: Post) = viewModelScope.launch {
        runCatching { repo.publishNow(post) }.onFailure { _error.value = it.message }
    }

    fun delete(post: Post) = viewModelScope.launch { repo.deletePost(post.id) }
    fun clearError() { _error.value = null }
}
