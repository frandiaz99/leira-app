package com.fdm.plancito.ui.places

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.Geocode
import com.fdm.plancito.data.Images
import com.fdm.plancito.data.Photo
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.Profile
import com.fdm.plancito.data.Review
import com.fdm.plancito.data.averageRating
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.Avatar
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.ConfirmDialog
import com.fdm.plancito.ui.common.DateDialog
import com.fdm.plancito.ui.common.EmptyState
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.Pill
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.SectionCard
import com.fdm.plancito.ui.common.StarRating
import com.fdm.plancito.ui.common.friendlyError
import com.fdm.plancito.ui.common.icon
import com.fdm.plancito.ui.common.rememberCategories
import com.fdm.plancito.ui.map.MapPin
import com.fdm.plancito.ui.map.PlaceMap
import com.fdm.plancito.ui.photos.PhotoViewer
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

class PlaceDetailViewModel(private val placeId: String) : BaseViewModel() {
    val place = MutableStateFlow<Place?>(null)
    val notFound = MutableStateFlow(false)
    val members = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val photoUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val uploading = MutableStateFlow(0)
    val deleted = MutableStateFlow(false)

    init {
        refresh()
        observeChanges()
    }

    override suspend fun load() {
        val p = repo.place(placeId)
        place.value = p
        notFound.value = p == null
        if (p == null) return
        if (members.value.isEmpty()) members.value = repo.members(p.groupId).associate { it.userId to it.profile }
        photoUrls.value = repo.signedUrls(p.photos.map { it.storagePath })
    }

    fun setVisited(date: String?) = launchSafe {
        repo.setVisited(placeId, date)
        load()
    }

    fun delete() = launchSafe {
        place.value?.let { repo.deletePlace(it) }
        deleted.value = true
    }

    fun uploadPhotos(context: Context, uris: List<Uri>) {
        val p = place.value ?: return
        uploading.value += uris.size
        viewModelScope.launch {
            for (uri in uris) {
                try {
                    repo.uploadPhoto(p.groupId, p.id, Images.compress(context, uri))
                } catch (e: Exception) {
                    error.value = friendlyError(e)
                } finally {
                    uploading.value -= 1
                }
            }
            runCatching { load() }
        }
    }

    fun deletePhoto(photo: Photo) = launchSafe {
        repo.deletePhoto(photo)
        load()
    }
}

@Composable
fun PlaceDetailScreen(placeId: String, onBack: () -> Unit, onEdit: () -> Unit, onReview: () -> Unit) {
    val vm: PlaceDetailViewModel = viewModel(key = "place-$placeId") { PlaceDetailViewModel(placeId) }
    val place by vm.place.collectAsState()
    val notFound by vm.notFound.collectAsState()
    val members by vm.members.collectAsState()
    val photoUrls by vm.photoUrls.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val deleted by vm.deleted.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(onBack = onBack) {
            if (place != null) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = { confirmDelete = true }) { Text("Borrar", color = Plancito.colors.danger) }
            }
        }
        LoadingBar(loading && place == null)
        val p = place
        if (p == null) {
            if (notFound) EmptyState(Icons.Outlined.LocationOff, "Este lugar no existe", "Puede que alguien del grupo lo haya borrado.")
            ErrorText(error, Modifier.padding(16.dp))
            return@Column
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(p)
            VisitedControl(p, onSet = vm::setVisited)
            Info(p, members[p.createdBy]?.displayName)
            if (p.lat != null && p.lng != null) MiniMap(p)
            ErrorText(error)
            if (p.visited) {
                ReviewsSection(p.reviews, members, vm.repo.userId, onReview)
                PhotosSection(p.photos, photoUrls, members, vm.repo.userId, uploading, vm::uploadPhotos, vm::deletePhoto)
            } else {
                Text(
                    "Cuando vayan, toquen ¡Fuimos! para dejar sus reseñas y subir fotos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Plancito.colors.textMuted,
                    modifier = Modifier.fillMaxWidth().border(1.dp, Plancito.colors.border, RoundedCornerShape(16.dp)).padding(16.dp),
                )
            }
        }

        if (confirmDelete) {
            ConfirmDialog(
                title = "¿Borrar \"${p.name}\"?",
                text = "Se borran también las reseñas y fotos de todo el grupo.",
                confirmLabel = "Borrar",
                onConfirm = vm::delete,
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(p: Place) {
    val cat = categoryInfo(p.category, rememberCategories())
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(cat.icon, null, tint = Plancito.colors.primaryText, modifier = Modifier.size(18.dp))
            Text(cat.label, style = MaterialTheme.typography.labelLarge, color = Plancito.colors.primaryText)
        }
        Text(p.name, style = MaterialTheme.typography.headlineMedium)
        if (p.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.tags.forEach { Pill("#$it") }
            }
        }
    }
}

@Composable
private fun VisitedControl(p: Place, onSet: (String?) -> Unit) {
    val t = Plancito.colors
    var picking by remember { mutableStateOf(false) }
    var confirmUnset by remember { mutableStateOf(false) }
    if (p.visited) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(t.visitedTint).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Check, null, tint = t.visitedText, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            Text(
                "Fueron" + (p.visitedAt?.let { " el ${Dates.format(it)}" } ?: ""),
                style = MaterialTheme.typography.titleSmall,
                color = t.visitedText,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Desmarcar",
                style = MaterialTheme.typography.labelMedium,
                color = t.visitedText,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { confirmUnset = true }.padding(8.dp),
            )
        }
    } else {
        PrimaryButton("¡Fuimos!", onClick = { picking = true }, icon = Icons.Filled.Check)
    }
    if (picking) DateDialog(initial = null, onlyPast = true, onPick = onSet, onDismiss = { picking = false })
    if (confirmUnset) {
        ConfirmDialog(
            title = "¿Volver a pendientes?",
            text = "Las reseñas y fotos se mantienen.",
            confirmLabel = "Desmarcar",
            danger = false,
            onConfirm = { onSet(null) },
            onDismiss = { confirmUnset = false },
        )
    }
}

@Composable
private fun Info(p: Place, addedBy: String?) {
    val context = LocalContext.current
    val t = Plancito.colors
    SectionCard {
        if (p.address != null) {
            InfoRow(Icons.Outlined.Place) {
                Text(
                    p.address,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { open(context, Geocode.mapsUrl(p)) },
                )
            }
        }
        if (!p.visited && p.plannedDate != null) {
            InfoRow(Icons.Outlined.Event) { Text("${Dates.format(p.plannedDate)} (${Dates.relativeDays(p.plannedDate)})", style = MaterialTheme.typography.bodyMedium) }
        }
        if (p.link != null) {
            InfoRow(Icons.Outlined.Link) {
                Text(
                    p.link.removePrefix("https://").removePrefix("http://").removePrefix("www."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = t.primaryText,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { open(context, p.link) },
                )
            }
        }
        if (p.description != null) InfoRow(Icons.AutoMirrored.Outlined.Notes) { Text(p.description, style = MaterialTheme.typography.bodyMedium) }
        InfoRow(Icons.Outlined.PersonAdd) {
            Text(
                "Agregado por ${addedBy ?: "…"} el ${Dates.format(p.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = t.textMuted,
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Plancito.colors.textMuted, modifier = Modifier.width(22.dp).size(20.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun MiniMap(p: Place) {
    val context = LocalContext.current
    val point = LatLng(p.lat!!, p.lng!!)
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))) {
        PlaceMap(
            pins = listOf(MapPin(p.id, point.latitude, point.longitude, p.category, if (p.visited) Plancito.colors.visited else Plancito.colors.primary, p.name)),
            focus = point,
            interactive = false,
            // Tocar la vista previa abre el lugar en la app de mapas.
            onTap = { _, _ -> open(context, Geocode.mapsUrl(p)) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ReviewsSection(reviews: List<Review>, members: Map<String, Profile>, me: String, onReview: () -> Unit) {
    val t = Plancito.colors
    val mine = reviews.firstOrNull { it.userId == me }
    val avg = averageRating(reviews.map { it.rating })
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reseñas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (avg != null) {
                StarRating(avg.roundToInt(), size = 16.dp)
                Text(" %.1f".format(avg), style = MaterialTheme.typography.labelLarge, color = t.textMuted)
            }
        }
        reviews.sortedByDescending { it.userId == me }.forEach { r ->
            val who = members[r.userId]
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(who?.displayName ?: "?", who?.avatarUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (r.userId == me) "Vos" else who?.displayName ?: "Alguien", style = MaterialTheme.typography.titleSmall)
                        StarRating(r.rating, size = 14.dp)
                    }
                    if (r.comment != null) Text(r.comment, style = MaterialTheme.typography.bodyMedium)
                    when (r.wouldReturn) {
                        true -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Check, null, tint = t.visitedText, modifier = Modifier.size(14.dp))
                            Text("Volvería", style = MaterialTheme.typography.labelMedium, color = t.visitedText)
                        }
                        false -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Close, null, tint = t.textMuted, modifier = Modifier.size(14.dp))
                            Text("No volvería", style = MaterialTheme.typography.labelMedium, color = t.textMuted)
                        }
                        null -> {}
                    }
                }
            }
        }
        if (mine == null) PrimaryButton("Dejar mi reseña", onClick = onReview, icon = Icons.Filled.Star)
        else OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Editar mi reseña") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotosSection(
    photos: List<Photo>,
    urls: Map<String, String>,
    members: Map<String, Profile>,
    me: String,
    uploading: Int,
    onUpload: (Context, List<Uri>) -> Unit,
    onDelete: (Photo) -> Unit,
) {
    val context = LocalContext.current
    val t = Plancito.colors
    val sorted = photos.sortedBy { it.createdAt }
    var viewing by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) onUpload(context, uris)
    }
    SectionCard(title = "Fotos") {
        if (sorted.isEmpty() && uploading == 0) {
            Text("Todavía no hay fotos.", style = MaterialTheme.typography.bodyMedium, color = t.textMuted)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 3) {
            sorted.forEachIndexed { i, photo ->
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(t.surfaceMuted).clickable { viewing = i },
                ) {
                    AsyncImage(urls[photo.storagePath], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            repeat(uploading) {
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(t.surfaceMuted), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
            // Relleno para que la última fila no estire las fotos.
            repeat((3 - (sorted.size + uploading) % 3) % 3) { Box(Modifier.weight(1f)) }
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
            Text("Subir fotos")
        }
    }
    viewing?.let { start ->
        PhotoViewer(
            photos = sorted,
            urls = urls,
            start = start,
            authorName = { id -> if (id == me) "Vos" else members[id]?.displayName ?: "" },
            canDelete = { it.uploadedBy == me },
            onDelete = onDelete,
            onDismiss = { viewing = null },
        )
    }
}

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
