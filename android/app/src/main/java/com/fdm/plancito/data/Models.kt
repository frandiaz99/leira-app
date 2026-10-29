package com.fdm.plancito.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class Group(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("created_by") val createdBy: String? = null,
)

/** Fila de group_members del usuario logueado, con su grupo. */
@Serializable
data class Membership(
    @SerialName("group_id") val groupId: String,
    val role: String, // owner | member
    @SerialName("groups") val group: Group,
)

/** Miembro de un grupo, con su perfil. */
@Serializable
data class Member(
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("profiles") val profile: Profile,
)

/** Lo que muestra la pantalla "Unirte a …" antes de aceptar (RPC invite_preview). */
@Serializable
data class InvitePreview(
    val id: String,
    val name: String,
    val members: Int,
    @SerialName("already_member") val alreadyMember: Boolean,
)

@Serializable
data class Place(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("created_by") val createdBy: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val category: String = "otro",
    val tags: List<String> = emptyList(),
    @SerialName("planned_date") val plannedDate: String? = null,
    val status: String = STATUS_WISHLIST, // wishlist | visited
    @SerialName("visited_at") val visitedAt: String? = null,
    val link: String? = null,
    @SerialName("created_at") val createdAt: String,
    val reviews: List<Review> = emptyList(),
    val photos: List<Photo> = emptyList(),
) {
    val visited: Boolean get() = status == STATUS_VISITED
    val hasLocation: Boolean get() = lat != null && lng != null
    val averageRating: Double? get() = averageRating(reviews.map { it.rating })

    /** Portada: la primera foto que se subió. */
    val cover: Photo? get() = photos.minByOrNull { it.createdAt }

    companion object {
        const val STATUS_WISHLIST = "wishlist"
        const val STATUS_VISITED = "visited"
    }
}

/** Campos editables de un lugar (formulario de alta y edición). */
data class PlaceInput(
    val name: String,
    val category: String,
    val description: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val tags: List<String> = emptyList(),
    val plannedDate: String? = null,
    val link: String? = null,
)

@Serializable
data class Review(
    val id: String,
    @SerialName("place_id") val placeId: String,
    @SerialName("user_id") val userId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("would_return") val wouldReturn: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class Photo(
    val id: String,
    @SerialName("place_id") val placeId: String,
    @SerialName("uploaded_by") val uploadedBy: String,
    @SerialName("storage_path") val storagePath: String,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String,
)

fun averageRating(ratings: List<Int>): Double? = if (ratings.isEmpty()) null else ratings.average()
