package com.fdm.plancito.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.CATEGORY_ICONS
import com.fdm.plancito.data.PlaceCategory
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.common.categoryIcon
import com.fdm.plancito.ui.common.friendlyError
import com.fdm.plancito.ui.theme.Plancito
import kotlinx.coroutines.launch

/** Crear una categoría propia del grupo activo: nombre + ícono. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewCategoryDialog(onCreated: (PlaceCategory) -> Unit, onDismiss: () -> Unit) {
    val store = PlancitoApp.instance.categories
    val scope = rememberCoroutineScope()
    val t = Plancito.colors
    var label by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(CATEGORY_ICONS.first()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Heladería, Museo, Plan con chicos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Ícono", style = MaterialTheme.typography.labelLarge, color = t.textMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CATEGORY_ICONS.forEach { name ->
                        val on = name == icon
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (on) t.primaryTint else Color.Transparent)
                                .border(1.dp, if (on) t.primary else t.border, RoundedCornerShape(10.dp))
                                .clickable { icon = name },
                            contentAlignment = Alignment.Center,
                        ) { Icon(categoryIcon(name), null, tint = if (on) t.primaryText else t.text, modifier = Modifier.size(22.dp)) }
                    }
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            onCreated(store.create(label, icon))
                        } catch (e: Exception) {
                            error = categoryError(e)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

fun categoryError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        msg.contains("duplicate key", true) -> "Ya hay una categoría con ese nombre."
        msg.contains("categories", true) && (msg.contains("does not exist", true) || msg.contains("schema cache", true)) ->
            "Falta aplicar la migración de categorías en Supabase (ver docs/setup.md)."
        else -> friendlyError(e)
    }
}
