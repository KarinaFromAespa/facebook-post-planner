package com.facebookpostplanner.ui.drafts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebookpostplanner.data.PlannerRepository
import com.facebookpostplanner.data.local.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DraftsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlannerRepository(app)

    val drafts: Flow<List<Post>> = repo.drafts()

    fun delete(post: Post) = viewModelScope.launch {
        repo.deletePost(post.id)
    }

    /** Publish a draft right now, bypassing the queue. */
    fun publishNow(post: Post) = viewModelScope.launch {
        runCatching { repo.publishNow(post) }
    }
}
