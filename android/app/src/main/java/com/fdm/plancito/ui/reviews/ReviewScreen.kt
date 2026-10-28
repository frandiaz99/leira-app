package com.fdm.plancito.ui.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.Review
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.ConfirmDialog
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.Segment
import com.fdm.plancito.ui.common.SegmentedControl
import com.fdm.plancito.ui.common.SectionCard
import com.fdm.plancito.ui.common.StarRating
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow

class ReviewViewModel(private val placeId: String) : BaseViewModel() {
    val place = MutableStateFlow<Place?>(null)
    val done = MutableStateFlow(false)

    init {
        refresh()
    }

    override suspend fun load() {
        place.value = repo.place(placeId) ?: error("Este lugar no existe")
    }

    fun save(rating: Int, comment: String, wouldReturn: Boolean?) = launchSafe {
        repo.saveReview(placeId, rating, comment.trim().ifBlank { null }, wouldReturn)
        done.value = true
    }

    fun delete(review: Review) = launchSafe {
        repo.deleteReview(review.id)
        done.value = true
    }
}

private val RATING_LABELS = listOf("Tocá las estrellas", "Malo", "Meh", "Bien", "Muy bueno", "¡Increíble!")

/** "¿Volverías?" admite no contestar: el segmento "No sé" guarda null. */
private enum class Return(val value: Boolean?) { Yes(true), No(false), DontKnow(null) }

@Composable
fun ReviewScreen(placeId: String, onDone: () -> Unit) {
    val vm: ReviewViewModel = viewModel(key = "review-$placeId") { ReviewViewModel(placeId) }
    val place by vm.place.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    LaunchedEffect(done) { if (done) onDone() }

    val p = place
    val existing = p?.reviews?.firstOrNull { it.userId == vm.repo.userId }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (existing != null) "Editar reseña" else "Tu reseña", onBack = onDone)
        LoadingBar(loading)
        if (p == null) {
            ErrorText(error, Modifier.padding(16.dp))
            return@Column
        }
        // key: si la reseña existente llega después, el formulario arranca con sus valores.
        var rating by remember(existing?.id) { mutableStateOf(existing?.rating ?: 0) }
        var comment by remember(existing?.id) { mutableStateOf(existing?.comment ?: "") }
        var wouldReturn by remember(existing?.id) { mutableStateOf(Return.entries.first { it.value == existing?.wouldReturn }) }
        var confirmDelete by remember { mutableStateOf(false) }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("¿Qué te pareció ${p.name}?", style = MaterialTheme.typography.bodyLarge, color = Plancito.colors.textMuted)
            SectionCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StarRating(rating, onChange = { rating = it }, size = 40.dp)
                    Text(RATING_LABELS[rating], style = MaterialTheme.typography.bodyMedium, color = Plancito.colors.textMuted)
                }
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Comentario") },
                placeholder = { Text("Lo mejor, lo peor, qué pedirían de nuevo…") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("¿Volverías?", style = MaterialTheme.typography.titleSmall)
            SegmentedControl(
                segments = listOf(Segment(Return.Yes, "🔁 Sí"), Segment(Return.No, "🚫 No"), Segment(Return.DontKnow, "No sé")),
                selected = wouldReturn,
                onSelect = { wouldReturn = it },
            )
            ErrorText(error)
            PrimaryButton("Guardar reseña", onClick = { vm.save(rating, comment, wouldReturn.value) }, enabled = rating > 0, loading = loading)
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Borrar reseña", color = Plancito.colors.danger)
                }
            }
        }
        if (confirmDelete && existing != null) {
            ConfirmDialog("¿Borrar tu reseña?", "No se puede deshacer.", "Borrar", onConfirm = { vm.delete(existing) }, onDismiss = { confirmDelete = false })
        }
    }
}
