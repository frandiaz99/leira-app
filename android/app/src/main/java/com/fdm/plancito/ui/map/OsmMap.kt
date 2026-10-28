package com.fdm.plancito.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

data class MapPin(val id: String, val lat: Double, val lng: Double, val emoji: String, val color: Color, val title: String)

/** Centro por defecto cuando todavía no hay nada marcado (Buenos Aires). */
val DEFAULT_CENTER = GeoPoint(-34.6037, -58.3816)

/**
 * Mapa de OpenStreetMap (osmdroid, sin API key).
 * - [focus]: al cambiar, el mapa se mueve ahí (no en cada recomposición, así no pelea con el dedo).
 * - [fitAll]: al cambiar los pines, encuadra todos.
 * - [onTap]: tocar el mapa (elegir ubicación). [interactive] = false para la vista previa fija.
 */
@Composable
fun OsmMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    focus: GeoPoint? = null,
    focusZoom: Double = 16.0,
    fitAll: Boolean = false,
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
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(pin.lat, pin.lng)
                    icon = pinDrawable(map.context, pin.emoji, pin.color)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = pin.title
                    setOnMarkerClickListener { _, _ ->
                        pinClick?.invoke(pin.id)
                        true
                    }
                })
            }
            map.invalidate()

            if (focus != null && focus != applied.focus) {
                applied.focus = focus
                map.controller.animateTo(focus, focusZoom, 600L)
            }
            if (fitAll && pins.isNotEmpty() && pins != applied.fitted) {
                applied.fitted = pins
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
}

/** Gota del color del estado con el emoji de la categoría adentro (como el pin de la PWA). */
private fun pinDrawable(context: Context, emoji: String, color: Color): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (36 * d).toInt()
    val h = (46 * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = w / 2f
    val drop = Path().apply {
        addCircle(r, r, r - d, Path.Direction.CW)
        moveTo(r - r * 0.55f, r + r * 0.7f)
        lineTo(r, h.toFloat() - d)
        lineTo(r + r * 0.55f, r + r * 0.7f)
        close()
    }
    c.drawPath(drop, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
    c.drawCircle(r, r, r - 4 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 17 * d; textAlign = Paint.Align.CENTER }
    c.drawText(emoji, r, r - (text.descent() + text.ascent()) / 2, text)
    return BitmapDrawable(context.resources, bmp)
}
