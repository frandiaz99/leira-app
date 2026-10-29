package com.fdm.plancito.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** ViewModel base: estado de carga/errores y recarga automática ante cambios en la base. */
abstract class BaseViewModel : ViewModel() {
    val repo = PlancitoApp.instance.repository
    val groups = PlancitoApp.instance.groups
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    protected abstract suspend fun load()

    fun refresh() = launchSafe { load() }

    protected fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading.value = true
            try {
                repo.ensureFreshSession()
                try {
                    block()
                } catch (e: Exception) {
                    // Token vencido: se renueva y se reintenta una vez antes de mostrar el error.
                    if (!repo.isJwtExpired(e)) throw e
                    repo.forceRefreshSession()
                    block()
                }
                error.value = null
            } catch (e: Exception) {
                error.value = friendlyError(e)
            } finally {
                loading.value = false
            }
        }
    }

    protected fun observeChanges() {
        viewModelScope.launch { PlancitoApp.instance.dbChanges.collect { runCatching { load() } } }
    }
}

/**
 * Los errores de las RPC llegan como el texto del `raise exception` ("Código inválido")
 * más detalles de la request; se muestra solo la primera línea.
 */
fun friendlyError(e: Throwable): String {
    val msg = e.message ?: return e.toString()
    if (msg.contains("Unable to resolve host", true) || msg.contains("timeout", true)) return "Sin conexión. Probá de nuevo."
    return msg.lineSequence().first().trim()
}

/** Encabezado de pantalla: volver (opcional), título y acciones a la derecha. */
@Composable
fun ScreenHeader(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        } else {
            Box(Modifier.size(12.dp))
        }
        Text(
            title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Plancito.colors.surface, contentColor = Plancito.colors.text),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun LoadingBar(loading: Boolean) {
    Box(Modifier.fillMaxWidth().heightIn(min = 4.dp)) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
fun ErrorText(error: String?, modifier: Modifier = Modifier) {
    if (error != null) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = Plancito.colors.danger, modifier = Modifier.size(16.dp))
            Text(error, color = Plancito.colors.danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = Plancito.colors.textMuted, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Plancito.colors.textMuted, textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    danger: Boolean = false,
    icon: ImageVector? = null,
) {
    val t = Plancito.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.small,
        colors = if (danger) ButtonDefaults.buttonColors(containerColor = t.danger, contentColor = t.onPrimary)
        else ButtonDefaults.buttonColors(containerColor = t.primary, contentColor = t.onPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = t.onPrimary)
        else {
            if (icon != null) Icon(icon, null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Estrellas de 1 a 5; con [onChange] se pueden tocar. */
@Composable
fun StarRating(value: Int, onChange: ((Int) -> Unit)? = null, size: Dp = 18.dp) {
    Row(
        Modifier.semantics { contentDescription = "$value de 5 estrellas" },
        horizontalArrangement = Arrangement.spacedBy(if (onChange != null) 6.dp else 1.dp),
    ) {
        (1..5).forEach { i ->
            val on = i <= value
            Icon(
                if (on) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (on) Plancito.colors.star else Plancito.colors.border,
                modifier = Modifier.size(size).then(
                    if (onChange != null) Modifier.clip(CircleShape).clickable { onChange(i) } else Modifier,
                ),
            )
        }
    }
}

/** Foto de perfil o, si no tiene, la inicial sobre el color de acento. */
@Composable
fun Avatar(name: String, url: String?, size: Dp = 36.dp) {
    val t = Plancito.colors
    Box(Modifier.size(size).clip(CircleShape).background(t.primaryTint), contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                color = t.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}

/** Etiqueta chica redondeada ("#pizza", "Pendiente"). */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Plancito.colors.surfaceMuted,
    color: Color = Plancito.colors.textMuted,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
