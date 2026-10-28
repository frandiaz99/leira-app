package com.fdm.plancito.data

import com.fdm.plancito.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.hours

/** Acceso a Supabase. Todas las consultas pasan por RLS con la sesión del usuario. */
class Repository {

    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    // Lazy: si falta configurar local.properties la app muestra un aviso en vez de cerrarse.
    val client: SupabaseClient by lazy {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            defaultSerializer = KotlinXSerializer(json)
            // La sesión se guarda en el teléfono y se renueva sola; sin internet se mantiene.
            install(Auth) {
                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    val isConfigured: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    // ---------------------------------------------------------------- Auth

    val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    val userId: String get() = client.auth.currentUserOrNull()?.id ?: error("No hay sesión")

    val userEmail: String? get() = client.auth.currentUserOrNull()?.email

    suspend fun awaitSession(): Boolean {
        val ok = client.auth.sessionStatus.first { it !is SessionStatus.Initializing } is SessionStatus.Authenticated
        if (ok) ensureFreshSession()
        return ok
    }

    /**
     * Renueva el token si venció o está por vencer. Al despertar la app la sesión
     * guardada puede tener el token viejo y las consultas salen con 401 antes de que
     * corra la renovación automática. Sin internet se sigue igual.
     */
    suspend fun ensureFreshSession() {
        val session = client.auth.currentSessionOrNull() ?: return
        if (session.expiresAt.toEpochMilliseconds() - System.currentTimeMillis() < 60_000) {
            runCatching { client.auth.refreshCurrentSession() }
        }
    }

    /** La consulta falló porque el token venció (401 / "JWT expired"). */
    fun isJwtExpired(e: Throwable): Boolean =
        (e is RestException && e.statusCode == 401) || e.message?.contains("JWT expired", ignoreCase = true) == true

    suspend fun forceRefreshSession() {
        runCatching { client.auth.refreshCurrentSession() }
    }

    /** Manda un código de 6 dígitos al mail (y crea la cuenta si no existe). */
    suspend fun sendEmailCode(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    /** [rawNonce] es el nonce sin hashear; a Google se le pasa su SHA-256 (ver GoogleSignIn). */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    suspend fun signOut() {
        signedUrlCache.clear()
        client.auth.signOut()
    }

    // ---------------------------------------------------------------- Perfil

    suspend fun myProfile(): Profile =
        client.from("profiles").select { filter { eq("id", userId) } }.decodeSingle()

    suspend fun updateDisplayName(name: String) {
        client.from("profiles").update(buildJsonObject { put("display_name", name) }) { filter { eq("id", userId) } }
    }

    // ---------------------------------------------------------------- Grupos

    suspend fun myGroups(): List<Membership> =
        client.from("group_members").select(Columns.raw("group_id, role, groups(*)")) {
            filter { eq("user_id", userId) }
            order("joined_at", Order.ASCENDING)
        }.decodeList()

    suspend fun members(groupId: String): List<Member> =
        client.from("group_members").select(Columns.raw("user_id, role, joined_at, profiles(id, display_name, avatar_url)")) {
            filter { eq("group_id", groupId) }
            order("joined_at", Order.ASCENDING)
        }.decodeList()

    suspend fun createGroup(name: String, emoji: String): Group =
        client.postgrest.rpc("create_group", buildJsonObject {
            put("group_name", name)
            put("group_emoji", emoji)
        }).decodeAs()

    suspend fun joinGroup(code: String): Group =
        client.postgrest.rpc("join_group", buildJsonObject { put("code", code) }).decodeAs()

    suspend fun invitePreview(code: String): InvitePreview? =
        client.postgrest.rpc("invite_preview", buildJsonObject { put("code", code) }).decodeList<InvitePreview>().firstOrNull()

    suspend fun updateGroup(groupId: String, name: String, emoji: String) {
        client.from("groups").update(buildJsonObject {
            put("name", name)
            put("emoji", emoji)
        }) { filter { eq("id", groupId) } }
    }

    suspend fun regenerateInvite(groupId: String): Group =
        client.postgrest.rpc("regenerate_invite", buildJsonObject { put("gid", groupId) }).decodeAs()

    suspend fun leaveGroup(groupId: String) {
        client.postgrest.rpc("leave_group", buildJsonObject { put("gid", groupId) })
    }

    // ---------------------------------------------------------------- Lugares

    suspend fun places(groupId: String): List<Place> =
        client.from("places").select(Columns.raw("*, reviews(*), photos(*)")) {
            filter { eq("group_id", groupId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun place(id: String): Place? =
        client.from("places").select(Columns.raw("*, reviews(*), photos(*)")) { filter { eq("id", id) } }
            .decodeSingleOrNull()

    // Los nulls van explícitos: al editar, vaciar un campo tiene que borrarlo en la base.
    private fun PlaceInput.toJson() = buildJsonObject {
        put("name", name)
        put("category", category)
        put("description", description)
        put("address", address)
        put("lat", lat)
        put("lng", lng)
        put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
        put("planned_date", plannedDate)
        put("link", link)
    }

    suspend fun createPlace(groupId: String, input: PlaceInput): Place {
        val row = buildJsonObject {
            input.toJson().forEach { (k, v) -> put(k, v) }
            put("group_id", groupId)
            put("created_by", userId)
        }
        return client.from("places").insert(row) { select() }.decodeSingle()
    }

    suspend fun updatePlace(id: String, input: PlaceInput) {
        client.from("places").update(input.toJson()) { filter { eq("id", id) } }
    }

    /** [visitedAt] null = volver a pendientes (las reseñas y fotos se mantienen). */
    suspend fun setVisited(id: String, visitedAt: String?) {
        client.from("places").update(buildJsonObject {
            put("status", if (visitedAt != null) Place.STATUS_VISITED else Place.STATUS_WISHLIST)
            put("visited_at", visitedAt)
        }) { filter { eq("id", id) } }
    }

    suspend fun deletePlace(place: Place) {
        // Las filas de photos se borran en cascada, pero los archivos del storage no.
        if (place.photos.isNotEmpty()) client.storage.from(PHOTOS_BUCKET).delete(place.photos.map { it.storagePath })
        client.from("places").delete { filter { eq("id", place.id) } }
    }

    // ---------------------------------------------------------------- Reseñas

    /** unique(place_id, user_id): crear o editar la reseña propia es la misma llamada. */
    suspend fun saveReview(placeId: String, rating: Int, comment: String?, wouldReturn: Boolean?) {
        client.from("reviews").upsert(buildJsonObject {
            put("place_id", placeId)
            put("user_id", userId)
            put("rating", rating)
            put("comment", comment)
            put("would_return", wouldReturn)
        }) { onConflict = "place_id,user_id" }
    }

    suspend fun deleteReview(id: String) {
        client.from("reviews").delete { filter { eq("id", id) } }
    }

    // ---------------------------------------------------------------- Fotos

    /** El primer segmento del path tiene que ser el group_id: lo exige la policy de storage. */
    suspend fun uploadPhoto(groupId: String, placeId: String, jpeg: ByteArray) {
        val path = "$groupId/$placeId/${UUID.randomUUID()}.jpg"
        val bucket = client.storage.from(PHOTOS_BUCKET)
        bucket.upload(path, jpeg) { contentType = ContentType.Image.JPEG }
        try {
            client.from("photos").insert(buildJsonObject {
                put("place_id", placeId)
                put("uploaded_by", userId)
                put("storage_path", path)
            })
        } catch (e: Exception) {
            // Que no quede un archivo huérfano.
            runCatching { bucket.delete(listOf(path)) }
            throw e
        }
    }

    suspend fun deletePhoto(photo: Photo) {
        client.from("photos").delete { filter { eq("id", photo.id) } }
        runCatching { client.storage.from(PHOTOS_BUCKET).delete(listOf(photo.storagePath)) }
        signedUrlCache.remove(photo.storagePath)
    }

    private data class CachedUrl(val url: String, val expiresAt: Long)

    private val signedUrlCache = java.util.concurrent.ConcurrentHashMap<String, CachedUrl>()

    /** URLs firmadas (el bucket es privado), cacheadas hasta 10 minutos antes de vencer. */
    suspend fun signedUrls(paths: Collection<String>): Map<String, String> {
        val now = System.currentTimeMillis()
        val missing = paths.filter { (signedUrlCache[it]?.expiresAt ?: 0) < now }.distinct()
        if (missing.isNotEmpty()) {
            val expiresAt = now + SIGNED_URL_TTL.inWholeMilliseconds - 10 * 60_000
            client.storage.from(PHOTOS_BUCKET).createSignedUrls(SIGNED_URL_TTL, missing).forEach { s ->
                if (s.error == null) signedUrlCache[s.path] = CachedUrl(absoluteStorageUrl(s.signedURL), expiresAt)
            }
        }
        return paths.mapNotNull { p -> signedUrlCache[p]?.let { p to it.url } }.toMap()
    }

    private fun absoluteStorageUrl(url: String): String =
        if (url.startsWith("http")) url else "${BuildConfig.SUPABASE_URL}/storage/v1/${url.trimStart('/')}"

    // ---------------------------------------------------------------- Realtime

    /** Cambios que hacen los otros miembros (Realtime respeta RLS). */
    fun changes(): Flow<Unit> {
        val channel = client.channel("db-changes")
        val flows = listOf("places", "reviews", "photos", "group_members").map { t ->
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = t }
        }
        return merge(*flows.toTypedArray()).map { }.onStart { channel.subscribe() }
    }

    companion object {
        const val PHOTOS_BUCKET = "photos"
        val SIGNED_URL_TTL = 1.hours
    }
}
