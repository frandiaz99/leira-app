package com.fdm.plancito.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.fdm.plancito.data.Dates
import com.fdm.plancito.data.Photo
import com.fdm.plancito.ui.common.ConfirmDialog

/** Fotos a pantalla completa, deslizando entre ellas. Cada uno puede borrar las que subió. */
@Composable
fun PhotoViewer(
    photos: List<Photo>,
    urls: Map<String, String>,
    start: Int,
    authorName: (String) -> String,
    canDelete: (Photo) -> Boolean,
    onDelete: (Photo) -> Unit,
    onDismiss: () -> Unit,
) {
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, photos.lastIndex)) { photos.size }
    var confirmDelete by remember { mutableStateOf<Photo?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                AsyncImage(urls[photos[i].storagePath], contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            val current = photos.getOrNull(pager.currentPage)
            Row(Modifier.fillMaxWidth().systemBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) }
                if (current != null) {
                    Text(
                        "${authorName(current.uploadedBy)} · ${Dates.format(current.createdAt)}   ${pager.currentPage + 1}/${photos.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    if (canDelete(current)) TextButton(onClick = { confirmDelete = current }) { Text("Borrar", color = Color.White) }
                }
            }
        }
    }
    confirmDelete?.let { photo ->
        ConfirmDialog(
            title = "¿Borrar esta foto?",
            text = "Se borra para todo el grupo.",
            confirmLabel = "Borrar",
            onConfirm = {
                onDelete(photo)
                if (photos.size == 1) onDismiss()
            },
            onDismiss = { confirmDelete = null },
        )
    }
}
