package com.fdm.plancito.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.GeoResult
import com.fdm.plancito.data.Geocode
import com.fdm.plancito.data.Place
import com.fdm.plancito.data.PlaceInput
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.BaseViewModel
import com.fdm.plancito.ui.common.DateDialog
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.LoadingBar
import com.fdm.plancito.ui.common.PrimaryButton
import com.fdm.plancito.ui.common.ScreenHeader
import com.fdm.plancito.ui.common.icon
import com.fdm.plancito.ui.common.rememberCategories
import com.fdm.plancito.ui.map.MapPin
import com.fdm.plancito.ui.map.PlaceMap
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng

class PlaceFormViewModel(private val placeId: String?) : BaseViewModel() {
    /** El lugar a editar; null mientras carga o si es nuevo. */
    val initial = MutableStateFlow<Place?>(null)
    val tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    /** Id del lugar guardado: la pantalla navega a su detalle (alta) o vuelve (edición). */
    val saved = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    override suspend fun load() {
        val groupId = if (placeId != null) {
            val p = repo.place(placeId) ?: error("Este lugar no existe")
            initial.value = p
            p.groupId
        } else {
            groups.active.value?.id ?: return
        }
        tagSuggestions.value = repo.places(groupId).flatMap { it.tags }.distinct().sorted()
    }

    fun save(input: PlaceInput) = launchSafe {
        if (placeId != null) {
            repo.updatePlace(placeId, input)
            saved.value = placeId
        } else {
            val groupId = groups.active.value?.id ?: error("Elegí un grupo primero")
            saved.value = repo.createPlace(groupId, input).id
        }
    }
}

/** Estado del formulario; se arma desde el lugar a editar (o vacío). */
private class FormState(p: Place?) {
    var name by mutableStateOf(p?.name ?: "")
    var category by mutableStateOf(p?.category ?: "restaurante")
    var address by mutableStateOf(p?.address ?: "")
    var lat by mutableStateOf(p?.lat)
    var lng by mutableStateOf(p?.lng)
    var tags by mutableStateOf(p?.tags ?: emptyList())
    var plannedDate by mutableStateOf(p?.plannedDate)
    var link by mutableStateOf(p?.link ?: "")
    var description by mutableStateOf(p?.description ?: "")

    fun toInput(): PlaceInput {
        val l = link.trim()
        return PlaceInput(
            name = name.trim(),
            category = category,
            address = address.trim().ifBlank { null },
            lat = lat,
            lng = lng,
            tags = tags,
            plannedDate = plannedDate,
            link = if (l.isEmpty()) null else if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(l)) l else "https://$l",
            description = description.trim().ifBlank { null },
        )
    }
}

/** Alta ([placeId] null) o edición de un lugar. [onSaved] recibe el id guardado. */
@Composable
fun PlaceFormScreen(placeId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val vm: PlaceFormViewModel = viewModel(key = "form-${placeId ?: "new"}") { PlaceFormViewModel(placeId) }
    val initial by vm.initial.collectAsState()
    val suggestions by vm.tagSuggestions.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.saved.collectAsState()

    LaunchedEffect(saved) { saved?.let(onSaved) }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (placeId == null) "Nuevo lugar" else "Editar lugar", onBack = onBack)
        LoadingBar(loading)
        if (placeId != null && initial == null) {
            ErrorText(error, Modifier.padding(16.dp))
            return@Column
        }
        // key: el formulario se arma una vez que llegó el lugar a editar.
        val form = remember(initial?.id) { FormState(initial) }
        Form(form, suggestions, isNew = placeId == null, saving = loading, error = error, onSubmit = { vm.save(form.toInput()) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Form(form: FormState, suggestions: List<String>, isNew: Boolean, saving: Boolean, error: String?, onSubmit: () -> Unit) {
    val t = Plancito.colors
    val scope = rememberCoroutineScope()
    var pickingDate by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { form.name = it },
            label = { Text("Nombre") },
            placeholder = { Text("Ej: La Cabrera, Bariloche, Teatro Colón…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Label("Categoría")
        var creatingCategory by remember { mutableStateOf(false) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
            rememberCategories().forEach { c ->
                val on = form.category == c.value
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (on) t.primaryTint else t.surface)
                        .border(1.dp, if (on) t.primary else t.border, RoundedCornerShape(12.dp))
                        .clickable { form.category = c.value }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(c.icon, null, tint = if (on) t.primaryText else t.text, modifier = Modifier.size(22.dp))
                    Text(
                        c.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) t.primaryText else t.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, t.border, RoundedCornerShape(12.dp))
                    .clickable { creatingCategory = true }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Add, null, tint = t.primaryText, modifier = Modifier.size(22.dp))
                Text("Nueva", style = MaterialTheme.typography.labelSmall, color = t.primaryText, maxLines = 1)
            }
        }
        if (creatingCategory) {
            NewCategoryDialog(
                onCreated = { c -> form.category = c.value; creatingCategory = false },
                onDismiss = { creatingCategory = false },
            )
        }

        Label("Ubicación")
        AddressSearch { r ->
            form.lat = r.lat
            form.lng = r.lng
            form.address = r.label
        }
        val point = if (form.lat != null && form.lng != null) LatLng(form.lat!!, form.lng!!) else null
        PlaceMap(
            pins = point?.let { listOf(MapPin("new", it.latitude, it.longitude, form.category, t.primary, form.name)) }.orEmpty(),
            focus = point,
            onTap = { la, lo ->
                form.lat = la
                form.lng = lo
                // Solo autocompletar la dirección si está vacía, para no pisar lo que escribieron.
                if (form.address.isBlank()) scope.launch { Geocode.reverse(la, lo)?.let { form.address = it } }
            },
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
        )
        Text("Buscá arriba o tocá el mapa para marcar el punto.", style = MaterialTheme.typography.bodySmall, color = t.textMuted)
        OutlinedTextField(
            value = form.address,
            onValueChange = { form.address = it },
            label = { Text("Dirección (opcional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (point != null) {
            TextButton(onClick = { form.lat = null; form.lng = null }) { Text("Quitar ubicación del mapa") }
        }

        Label("¿Cuándo piensan ir?")
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { pickingDate = true }) {
                Icon(Icons.Outlined.Event, null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                Text(form.plannedDate?.let { Dates.format(it) } ?: "Elegir fecha (opcional)")
            }
            if (form.plannedDate != null) TextButton(onClick = { form.plannedDate = null }) { Text("Quitar", color = t.textMuted) }
        }

        Label("Etiquetas")
        TagInput(form.tags, suggestions, onChange = { form.tags = it })

        OutlinedTextField(
            value = form.link,
            onValueChange = { form.link = it },
            label = { Text("Link") },
            placeholder = { Text("Instagram, web, reserva…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = { form.description = it },
            label = { Text("Notas") },
            placeholder = { Text("Por qué queremos ir, qué pedir, quién lo recomendó…") },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        ErrorText(error)
        PrimaryButton(
            if (isNew) "Agregar a la lista" else "Guardar cambios",
            onClick = onSubmit,
            enabled = form.name.isNotBlank(),
            loading = saving,
        )
    }

    if (pickingDate) DateDialog(initial = form.plannedDate, onPick = { form.plannedDate = it }, onDismiss = { pickingDate = false })
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

/** Búsqueda de direcciones (Nominatim) con debounce de 600 ms. */
@Composable
private fun AddressSearch(onSelect: (GeoResult) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        results = emptyList()
        if (query.trim().length < 3) return@LaunchedEffect
        delay(600)
        searching = true
        results = runCatching { Geocode.search(query.trim()) }.getOrDefault(emptyList())
        searching = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar dirección o lugar…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth(),
        )
        if (searching) Text("Buscando…", style = MaterialTheme.typography.bodySmall, color = Plancito.colors.textMuted)
        results.forEach { r ->
            Text(
                r.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                    onSelect(r)
                    query = ""
                }.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

/** Etiquetas como chips: las puestas se sacan tocándolas; se agregan escribiendo o de las sugerencias. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagInput(tags: List<String>, suggestions: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun add(raw: String) {
        val tag = raw.trim().removePrefix("#").lowercase()
        if (tag.isNotEmpty() && tag !in tags) onChange(tags + tag)
        text = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag -> InputChip(selected = true, onClick = { onChange(tags - tag) }, label = { Text("#$tag") },
                        trailingIcon = { Icon(Icons.Filled.Close, "Quitar", modifier = Modifier.size(16.dp)) },
                    ) }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { v -> if (v.endsWith(",") || v.endsWith(" ")) add(v.dropLast(1)) else text = v },
            placeholder = { Text("pizza, cumple, con amigos…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        val rest = suggestions.filter { it !in tags && (text.isBlank() || it.startsWith(text.trim().lowercase())) }.take(8)
        if (rest.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rest.forEach { s -> FilterChip(selected = false, onClick = { add(s) }, label = { Text("#$s") }) }
            }
        }
    }
}
