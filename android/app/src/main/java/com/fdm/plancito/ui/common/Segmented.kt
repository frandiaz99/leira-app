package com.fdm.plancito.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fdm.plancito.ui.theme.Plancito

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * Control segmentado (Pendientes/Visitados, ¿Volverías?): pista redondeada de 14
 * y la opción elegida resaltada. `selectedColor` cambia el color del texto elegido.
 */
@Composable
fun <T> SegmentedControl(
    segments: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    selectedColor: (T) -> Color? = { null },
) {
    val t = Plancito.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.surfaceMuted).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEach { s ->
            val on = s.value == selected
            val fg = if (on) selectedColor(s.value) ?: t.text else t.textMuted
            Box(
                Modifier.weight(1f).height(height).clip(RoundedCornerShape(10.dp))
                    .background(if (on) t.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(s.value) }),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.icon != null) {
                        Icon(s.icon, null, tint = fg, modifier = Modifier.size(16.dp))
                        Box(Modifier.width(6.dp))
                    }
                    Text(s.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fg)
                }
            }
        }
    }
}
