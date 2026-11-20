package com.facebookpostplanner.data.remote

import com.facebookpostplanner.data.local.PostStatus
import com.facebookpostplanner.data.local.PostType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class PostDto(
    val id: String,
    val type: String,
    val caption: String,
    val hashtags: String?,
    val scheduledAt: String?, // ISO-8601
    val status: String,
    val failureReason: String?,
    val facebookPostId: String?
)

data class CreatePostRequest(
    val type: String,
    val caption: String,
    val hashtags: String?,
    val scheduledAt: String?, // ISO-8601, null = draft
    val status: String,
    val publishNow: Boolean = false,
    val pageId: String? = null
)

data class PageDto(
    val id: String,
    val name: String
)

data class QueueStateDto(val paused: Boolean)
data class InsightsDto(
    val monetized: Boolean,
    val monetizationCriteria: String?,
    val totalEarningsUsd: Double?,
    val posts: List<InsightDto>
)
data class InsightDto(
    val postId: String,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val views: Int,
    val earningsUsd: Double?
)

interface PlannerApi {
    @GET("api/pages")
    suspend fun pages(): List<PageDto>

    @GET("api/queue")
    suspend fun queue(): List<PostDto>

    @GET("api/drafts")
    suspend fun drafts(): List<PostDto>

    @POST("api/posts")
    suspend fun createPost(@Body req: CreatePostRequest): PostDto

    @Multipart
    @POST("api/posts/{id}/media")
    suspend fun uploadMedia(
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): PostDto

    @POST("api/posts/{id}/publish-now")
    suspend fun publishNow(@Path("id") id: String): PostDto

    @GET("api/queue/state")
    suspend fun queueState(): QueueStateDto

    @POST("api/queue/pause")
    suspend fun pauseQueue(): QueueStateDto

    @POST("api/queue/resume")
    suspend fun resumeQueue(): QueueStateDto

    @GET("api/insights")
    suspend fun insights(): InsightsDto
}

fun PostType.toApi(): String = name.lowercase()
fun PostStatus.toApi(): String = name.lowercase()
