package com.fdm.plancito.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.PlaceCategory
import com.fdm.plancito.data.groupColorIndex
import com.fdm.plancito.data.groupInitials

/** Dibujo de cada nombre de [com.fdm.plancito.data.CATEGORY_ICONS]. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "restaurant" -> Icons.Outlined.Restaurant
    "pizza" -> Icons.Outlined.LocalPizza
    "icecream" -> Icons.Outlined.Icecream
    "cafe" -> Icons.Outlined.LocalCafe
    "bar" -> Icons.Outlined.LocalBar
    "nightlife" -> Icons.Outlined.Nightlife
    "music" -> Icons.Outlined.MusicNote
    "theater" -> Icons.Outlined.TheaterComedy
    "movie" -> Icons.Outlined.Movie
    "museum" -> Icons.Outlined.Museum
    "library" -> Icons.Outlined.LocalLibrary
    "shopping" -> Icons.Outlined.ShoppingBag
    "park" -> Icons.Outlined.Park
    "beach" -> Icons.Outlined.BeachAccess
    "hiking" -> Icons.Outlined.Hiking
    "sports" -> Icons.Outlined.SportsSoccer
    "fitness" -> Icons.Outlined.FitnessCenter
    "flight" -> Icons.Outlined.Flight
    "hotel" -> Icons.Outlined.Hotel
    "ticket" -> Icons.Outlined.ConfirmationNumber
    "pets" -> Icons.Outlined.Pets
    "star" -> Icons.Outlined.StarOutline
    else -> Icons.Outlined.Place
}

/** Ícono de la categoría: en la lista, los filtros, el formulario y los pines del mapa. */
val PlaceCategory.icon: ImageVector get() = categoryIcon(iconName)

/** Categorías del grupo activo (fijas + propias). */
@Composable
fun rememberCategories(): List<PlaceCategory> = PlancitoApp.instance.categories.all.collectAsState().value

/** Fondos del avatar de grupo; con texto blanco pasan contraste en claro y oscuro. */
private val GROUP_COLORS = listOf(
    Color(0xFFBE123C), // rosa
    Color(0xFF0F766E), // verde azulado
    Color(0xFF4338CA), // índigo
    Color(0xFFB45309), // ámbar
    Color(0xFF6D28D9), // violeta
    Color(0xFF0369A1), // celeste
)

/** Iniciales del grupo sobre un color que sale de su id. */
@Composable
fun GroupAvatar(groupId: String, name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(GROUP_COLORS[groupColorIndex(groupId, GROUP_COLORS.size)]),
        contentAlignment = Alignment.Center,
    ) {
        Text(groupInitials(name), color = Color.White, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}
