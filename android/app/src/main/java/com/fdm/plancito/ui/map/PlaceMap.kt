package com.fdm.plancito.ui.map

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.fdm.plancito.BuildConfig
import com.fdm.plancito.data.CATEGORY_ICONS
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.categoryIcon
import com.fdm.plancito.ui.common.rememberCategories
import com.fdm.plancito.ui.theme.Plancito
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** [category] es el valor del lugar ("restaurante" o el id de una categoría propia): define el ícono del pin. */
data class MapPin(val id: String, val lat: Double, val lng: Double, val category: String, val color: Color, val title: String)

/** Centro por defecto cuando todavía no hay nada marcado (Buenos Aires). */
val DEFAULT_CENTER = LatLng(-34.6037, -58.3816)

/**
 * Mapa de Google.
 * - [focus]: al cambiar, el mapa se mueve ahí.
 * - [fitAll]: al cambiar los pines (o [refitKey]), encuadra todos.
 * - [onTap]: tocar el mapa (elegir ubicación). [interactive] = false para la vista previa fija.
 */
@Composable
fun PlaceMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    focus: LatLng? = null,
    focusZoom: Float = 16f,
    fitAll: Boolean = false,
    refitKey: Int = 0,
    interactive: Boolean = true,
    onTap: ((Double, Double) -> Unit)? = null,
    onPinClick: ((String) -> Unit)? = null,
) {
    if (BuildConfig.MAPS_API_KEY.isBlank()) {
        Box(modifier.background(Plancito.colors.surfaceMuted), contentAlignment = Alignment.Center) {
            Text(
                "Falta configurar la clave de Google Maps (google.mapsApiKey, ver docs/setup.md).",
                style = MaterialTheme.typography.bodySmall,
                color = Plancito.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val tap by rememberUpdatedState(onTap)
    val pinClick by rememberUpdatedState(onPinClick)
    val start = focus ?: pins.firstOrNull()?.let { LatLng(it.lat, it.lng) } ?: DEFAULT_CENTER
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, if (focus != null || pins.size == 1) focusZoom else 12f)
    }
    var loaded by remember { mutableStateOf(false) }
    val categories = rememberCategories()
    val density = LocalDensity.current
    // Un painter por ícono posible (la lista es fija, así que el orden de los remember no cambia).
    val painters = CATEGORY_ICONS.associateWith { rememberVectorPainter(categoryIcon(it)) }

    // La vista previa (lite mode) es una imagen fija: no se anima.
    LaunchedEffect(loaded, focus) {
        if (loaded && interactive && focus != null) camera.animate(CameraUpdateFactory.newLatLngZoom(focus, focusZoom), 600)
    }
    LaunchedEffect(loaded, pins, refitKey) {
        if (!loaded || !fitAll || pins.isEmpty()) return@LaunchedEffect
        val update = if (pins.size == 1) {
            CameraUpdateFactory.newLatLngZoom(LatLng(pins[0].lat, pins[0].lng), 15f)
        } else {
            val bounds = LatLngBounds.builder().apply { pins.forEach { include(LatLng(it.lat, it.lng)) } }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, with(density) { 64.dp.roundToPx() })
        }
        camera.animate(update, 600)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(!interactive) },
        uiSettings = MapUiSettings(
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            scrollGesturesEnabled = interactive,
            zoomGesturesEnabled = interactive,
        ),
        onMapClick = { tap?.invoke(it.latitude, it.longitude) },
        onMapLoaded = { loaded = true },
    ) {
        pins.forEach { pin ->
            key(pin.id) {
                val iconName = categoryInfo(pin.category, categories).iconName
                val icon = remember(iconName, pin.color) {
                    BitmapDescriptorFactory.fromBitmap(pinBitmap(painters.getValue(iconName), pin.color, density))
                }
                val state = remember(pin.lat, pin.lng) { MarkerState(LatLng(pin.lat, pin.lng)) }
                Marker(
                    state = state,
                    title = pin.title,
                    icon = icon,
                    anchor = Offset(0.5f, 1f),
                    onClick = {
                        pinClick?.invoke(pin.id)
                        true
                    },
                )
            }
        }
    }
}

/** Gota del color del estado con el ícono de la categoría adentro, del mismo color. */
private fun pinBitmap(icon: VectorPainter, color: Color, density: Density): Bitmap {
    val d = density.density
    val w = (36 * d).toInt()
    val h = (46 * d).toInt()
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
        val r = w / 2f
        val drop = Path().apply {
            addOval(Rect(Offset(r, r), r - d))
            moveTo(r - r * 0.55f, r + r * 0.7f)
            lineTo(r, h - d)
            lineTo(r + r * 0.55f, r + r * 0.7f)
            close()
        }
        drawPath(drop, color)
        drawCircle(Color.White, radius = r - 4 * d, center = Offset(r, r))
        val s = 20 * d
        translate(r - s / 2, r - s / 2) {
            with(icon) { draw(Size(s, s), colorFilter = ColorFilter.tint(color)) }
        }
    }
    return bitmap.asAndroidBitmap()
}
