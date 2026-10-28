package com.fdm.plancito.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.fdm.plancito.data.Dates
import com.fdm.plancito.ui.theme.Plancito

/** Confirmación para acciones que no se deshacen (borrar, salir del grupo). */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (danger) Plancito.colors.danger else Plancito.colors.primaryText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Elegir una fecha (ISO "2026-10-28"). [onlyPast]: no deja elegir días futuros ("¿Cuándo fueron?"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: String?, onPick: (String) -> Unit, onDismiss: () -> Unit, onlyPast: Boolean = false) {
    val todayMillis = Dates.toPickerMillis(Dates.today().toString())
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let { Dates.toPickerMillis(it) } ?: todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !onlyPast || utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Dates.fromPickerMillis(it)) }
                onDismiss()
            }) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) { DatePicker(state) }
}
