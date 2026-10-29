package com.fdm.plancito.ui.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fdm.plancito.data.CATEGORIES
import com.fdm.plancito.data.categoryInfo
import com.fdm.plancito.ui.common.icon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** [category] es el valor de la base ("restaurante", "bar"…): define el ícono del pin. */
data class MapPin(val id: String, val lat: Double, val lng: Double, val category: String, val color: Color, val title: String)

/** Centro por defecto cuando todavía no hay nada marcado (Buenos Aires). */
val DEFAULT_CENTER = GeoPoint(-34.6037, -58.3816)

/**
 * Mapa de OpenStreetMap (osmdroid, sin API key).
 * - [focus]: al cambiar, el mapa se mueve ahí (no en cada recomposición, así no pelea con el dedo).
 * - [fitAll]: al cambiar los pines (o [refitKey]), encuadra todos.
 * - [onTap]: tocar el mapa (elegir ubicación). [interactive] = false para la vista previa fija.
 */
@Composable
fun OsmMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    focus: GeoPoint? = null,
    focusZoom: Double = 16.0,
    fitAll: Boolean = false,
    refitKey: Int = 0,
    interactive: Boolean = true,
    onTap: ((Double, Double) -> Unit)? = null,
    onPinClick: ((String) -> Unit)? = null,
) {
    val tap by rememberUpdatedState(onTap)
    val pinClick by rememberUpdatedState(onPinClick)
    // Último foco y encuadre aplicados; no son estado de Compose porque se escriben en `update`.
    val applied = remember { Applied() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    val density = LocalDensity.current
    // Un painter por categoría (la lista es fija, así que el orden de los remember no cambia).
    val painters = CATEGORIES.associate { it.value to rememberVectorPainter(it.icon) }
    val pinIcons = remember { HashMap<Pair<String, Color>, BitmapDrawable>() }

    DisposableEffect(lifecycle, mapRef) {
        val map = mapRef
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map?.onResume()
                Lifecycle.Event.ON_PAUSE -> map?.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map?.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(interactive)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isTilesScaledToDpi = true
                minZoomLevel = 3.0
                controller.setZoom(12.0)
                controller.setCenter(focus ?: DEFAULT_CENTER)
                // Vista previa: el mapa no consume toques (el padre los maneja).
                if (!interactive) setOnTouchListener { _, _: MotionEvent -> true }
                mapRef = this
            }
        },
        update = { map ->
            map.overlays.clear()
            map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    tap?.invoke(p.latitude, p.longitude) ?: return false
                    return true
                }

                override fun longPressHelper(p: GeoPoint) = false
            }))
            pins.forEach { pin ->
                val category = categoryInfo(pin.category).value
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(pin.lat, pin.lng)
                    icon = pinIcons.getOrPut(category to pin.color) {
                        pinDrawable(map.context, painters.getValue(category), pin.color, density)
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = pin.title
                    setOnMarkerClickListener { _, _ ->
                        pinClick?.invoke(pin.id)
                        true
                    }
                })
            }
            // La política de uso de OpenStreetMap pide mostrar la atribución.
            map.overlays.add(CopyrightOverlay(map.context).apply { setAlignRight(true) })
            map.invalidate()

            if (focus != null && focus != applied.focus) {
                applied.focus = focus
                map.controller.animateTo(focus, focusZoom, 600L)
            }
            if (fitAll && pins.isNotEmpty() && (pins != applied.fitted || refitKey != applied.refitKey)) {
                applied.fitted = pins
                applied.refitKey = refitKey
                // Hay que esperar a que el mapa tenga tamaño para encuadrar.
                map.post {
                    if (pins.size == 1) {
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(GeoPoint(pins[0].lat, pins[0].lng))
                    } else {
                        val box = BoundingBox.fromGeoPoints(pins.map { GeoPoint(it.lat, it.lng) })
                        map.zoomToBoundingBox(box.increaseByScale(1.3f), false, 64)
                    }
                }
            }
        },
    )
}

private class Applied {
    var focus: GeoPoint? = null
    var fitted: List<MapPin>? = null
    var refitKey: Int = 0
}

/** Gota del color del estado con el ícono de la categoría adentro, del mismo color. */
private fun pinDrawable(context: Context, icon: VectorPainter, color: Color, density: Density): BitmapDrawable {
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
    return BitmapDrawable(context.resources, bitmap.asAndroidBitmap())
}
