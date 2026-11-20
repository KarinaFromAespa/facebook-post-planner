package com.facebookpostplanner.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebookpostplanner.data.PlannerRepository
import com.facebookpostplanner.data.local.PostType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)

    val pageName: StateFlow<String?> =
        repo.selectedPageName.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _type = MutableStateFlow(PostType.POST)
    val type: StateFlow<PostType> = _type.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun setType(t: PostType) { _type.value = t }

    // The post being edited, if any — set by loadForEdit. When set, save()
    // PATCHes this post instead of creating a new one.
    private var editingPost: com.facebookpostplanner.data.local.Post? = null
    val editing: Boolean get() = editingPost != null

    fun loadForEdit(post: com.facebookpostplanner.data.local.Post) {
        editingPost = post
        _type.value = post.type
    }

    fun clearEdit() { editingPost = null }

    /** Next hourly slot today, defaulting to the next hour (min 2 min ahead is enforced by the backend). */
    fun defaultScheduleMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    fun save(
        caption: String,
        hashtags: String,
        mediaUris: List<Uri>,
        scheduledAtMillis: Long?,
        publishNow: Boolean,
        saveAsDraft: Boolean = false
    ) = viewModelScope.launch {
        if (caption.isBlank() && mediaUris.isEmpty()) {
            _error.value = "Add text or media first."
            return@launch
        }
        if (_type.value == PostType.REEL && mediaUris.size != 1) {
            _error.value = "A reel needs exactly one video (MP4)."
            return@launch
        }
        if (_type.value == PostType.STORY && mediaUris.size != 1) {
            _error.value = "A story needs exactly one photo in v1."
            return@launch
        }
        _saving.value = true
        runCatching {
            val editing = editingPost
            if (editing != null) {
                val updated = repo.updatePost(editing, _type.value, caption, hashtags, mediaUris, scheduledAtMillis, saveAsDraft)
                if (publishNow) repo.publishNow(updated)
                editingPost = null
            } else {
                repo.createPost(_type.value, caption, hashtags, mediaUris, scheduledAtMillis, publishNow, saveAsDraft)
            }
        }.onSuccess { _done.value = true }
            .onFailure { _error.value = "Saved locally, backend sync failed: ${it.message}" }
        _saving.value = false
    }

    fun clearError() { _error.value = null }
    fun resetDone() { _done.value = false }
}
