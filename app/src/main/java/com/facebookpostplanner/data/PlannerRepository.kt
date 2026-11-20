package com.facebookpostplanner.data

import android.content.Context
import android.net.Uri
import com.facebookpostplanner.data.local.AppDatabase
import com.facebookpostplanner.data.local.InsightEntity
import com.facebookpostplanner.data.local.Post
import com.facebookpostplanner.data.local.PostStatus
import com.facebookpostplanner.data.local.PostType
import com.facebookpostplanner.data.remote.ApiFactory
import com.facebookpostplanner.data.remote.CreatePostRequest
import com.facebookpostplanner.data.remote.InsightsDto
import com.facebookpostplanner.data.remote.PlannerApi
import com.facebookpostplanner.data.remote.toApi
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Local-first repository. The queue/drafts live in Room so the Today list
 * works offline; every create / publish-now / pause also goes to the backend,
 * which owns the 1-minute publisher and Object Storage.
 * If the backend is unreachable, the local copy is kept and the error is
 * surfaced to the UI — nothing is silently dropped.
 */
class PlannerRepository(context: Context) {
    private val db = AppDatabase.get(context.applicationContext)
    private val dao = db.postDao()
    private val insightDao = db.insightDao()
    private val settings = com.facebookpostplanner.data.remote.SettingsStore(context.applicationContext)
    private val appContext = context.applicationContext

    fun queue(): Flow<List<Post>> = dao.queue()
    fun drafts(): Flow<List<Post>> = dao.drafts()
    fun published(): Flow<List<Post>> = dao.published()
    fun insights(): Flow<List<InsightEntity>> = insightDao.all()

    private fun api(): PlannerApi = ApiFactory.create(settings.currentBackendUrl())

    val selectedPageId: Flow<String?> = settings.pageId
    val selectedPageName: Flow<String?> = settings.pageName
    fun currentPageId(): String? = settings.currentPageId()
    fun currentPageName(): String? = settings.currentPageName()
    suspend fun setPage(id: String?, name: String?) = settings.setPage(id, name)

    /** Pages the backend knows about (from the Page token it holds). Empty when the backend is unreachable or has no Page configured yet. */
    suspend fun pages(): List<com.facebookpostplanner.data.remote.PageDto> =
        runCatching { api().pages() }.getOrDefault(emptyList())

    suspend fun setBackendUrl(url: String) = settings.setBackendUrl(url)
    fun backendUrl(): Flow<String> = settings.backendUrl

    suspend fun refreshQueue() {
        runCatching { api().queue() }.onSuccess { dtos ->
            dtos.forEach { dao.upsert(it.toLocal()) }
        }
    }

    suspend fun createPost(
        type: PostType,
        caption: String,
        hashtags: String,
        mediaUris: List<Uri>,
        scheduledAtMillis: Long?,
        publishNow: Boolean
    ): Post {
        val post = Post(
            id = UUID.randomUUID().toString(),
            type = type,
            caption = caption.trim(),
            hashtags = hashtags.trim(),
            mediaUris = mediaUris.joinToString(",") { it.toString() },
            mediaKind = if (type == PostType.REEL) "video" else if (mediaUris.isNotEmpty()) "photo" else "",
            scheduledAtMillis = scheduledAtMillis,
            status = when {
                publishNow -> PostStatus.PUBLISHING
                scheduledAtMillis != null -> PostStatus.SCHEDULED
                else -> PostStatus.DRAFT
            }
        )
        dao.upsert(post)
        // Best-effort backend sync; local copy stays even if this fails.
        runCatching {
            val dto = api().createPost(
                CreatePostRequest(
                    type = type.toApi(), caption = post.caption,
                    hashtags = post.hashtags.ifBlank { null },
                    scheduledAt = scheduledAtMillis?.let { iso(it) },
                    status = post.status.toApi(), publishNow = publishNow,
                    pageId = currentPageId()
                )
            )
            dao.upsert(post.copy(backendId = dto.id))
            // Upload media files one by one; backend stores them in Object Storage.
            mediaUris.forEach { uri ->
                runCatching { uploadMedia(dto.id, uri) }
            }
            if (publishNow) {
                runCatching { api().publishNow(dto.id) }
            }
        }.onFailure { e ->
            dao.upsert(post.copy(status = PostStatus.FAILED, failureReason = e.message))
            throw e
        }
        return post
    }

    private suspend fun uploadMedia(backendId: String, uri: Uri) {
        val file = File(appContext.cacheDir, "upload-${UUID.randomUUID()}")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        api().uploadMedia(backendId, part)
        file.delete()
    }

    suspend fun publishNow(post: Post) {
        dao.upsert(post.copy(status = PostStatus.PUBLISHING))
        runCatching {
            val backendId = post.backendId ?: error("Not synced to backend yet")
            api().publishNow(backendId)
            dao.upsert(post.copy(status = PostStatus.PUBLISHED))
        }.onFailure { e ->
            dao.upsert(post.copy(status = PostStatus.FAILED, failureReason = e.message))
            throw e
        }
    }

    suspend fun deletePost(id: String) = dao.delete(id)

    suspend fun setQueuePaused(paused: Boolean): Boolean {
        return runCatching {
            if (paused) api().pauseQueue() else api().resumeQueue()
            paused
        }.getOrElse { throw it }
    }

    suspend fun queuePaused(): Boolean =
        runCatching { api().queueState().paused }.getOrDefault(false)

    suspend fun refreshInsights(): InsightsDto? {
        return runCatching { api().insights() }.onSuccess { dto ->
            insightDao.upsertAll(dto.posts.map {
                InsightEntity(it.postId, it.likes, it.comments, it.shares, it.views, it.earningsUsd)
            })
        }.getOrNull()
    }

    companion object {
        fun iso(millis: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.format(Date(millis))
        }
    }
}

private fun com.facebookpostplanner.data.remote.PostDto.toLocal(): Post {
    val type = runCatching { PostType.valueOf(type.uppercase()) }.getOrDefault(PostType.POST)
    val status = runCatching { PostStatus.valueOf(status.uppercase()) }.getOrDefault(PostStatus.SCHEDULED)
    val millis = scheduledAt?.let {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(it)?.time
        }.getOrNull()
    }
    return Post(
        id = id, type = type, caption = caption, hashtags = hashtags ?: "",
        scheduledAtMillis = millis, status = status,
        failureReason = failureReason, backendId = id, publishedPostId = facebookPostId
    )
}
