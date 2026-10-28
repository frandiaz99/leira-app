package com.fdm.plancito.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fdm.plancito.PlancitoApp
import com.fdm.plancito.data.Invite
import com.fdm.plancito.ui.common.ErrorText
import com.fdm.plancito.ui.groups.GroupDetailScreen
import com.fdm.plancito.ui.groups.GroupsScreen
import com.fdm.plancito.ui.groups.JoinScreen
import com.fdm.plancito.ui.login.LoginScreen
import com.fdm.plancito.ui.map.MapScreen
import com.fdm.plancito.ui.places.PlaceDetailScreen
import com.fdm.plancito.ui.places.PlaceFormScreen
import com.fdm.plancito.ui.places.PlacesScreen
import com.fdm.plancito.ui.profile.ProfileScreen
import com.fdm.plancito.ui.reviews.ReviewScreen
import com.fdm.plancito.ui.theme.Plancito
import com.fdm.plancito.ui.theme.PlancitoTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleInvite(intent)
        setContent {
            PlancitoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Root() }
            }
        }
    }

    // launchMode singleTask: un link de invitación con la app abierta llega acá.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInvite(intent)
    }

    /** Guarda el código; se procesa después del login si hace falta. */
    private fun handleInvite(intent: Intent?) {
        Invite.codeFrom(intent?.data?.toString())?.let { PlancitoApp.instance.prefs.setPendingInvite(it) }
    }
}

@Composable
private fun Root() {
    val app = PlancitoApp.instance
    val repo = app.repository
    if (!repo.isConfigured) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Falta configurar supabase.url y supabase.anonKey en android/local.properties (ver docs/setup.md).")
        }
        return
    }
    val status by repo.sessionStatus.collectAsState()
    val pendingInvite by app.prefs.pendingInviteFlow.collectAsState()
    when (status) {
        // RefreshFailure = no se pudo renovar (p. ej. sin internet): se sigue con la sesión guardada.
        is SessionStatus.Authenticated, is SessionStatus.RefreshFailure -> SignedIn()
        is SessionStatus.Initializing -> Centered { CircularProgressIndicator() }
        else -> LoginScreen(pendingInvite = pendingInvite != null)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** Con sesión: invitación pendiente → sin grupos (bienvenida) → la app. */
@Composable
private fun SignedIn() {
    val app = PlancitoApp.instance
    val store = app.groups
    val memberships by store.groups.collectAsState()
    val error by store.error.collectAsState()
    val pendingInvite by app.prefs.pendingInviteFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val userId = runCatching { app.repository.userId }.getOrNull()

    LaunchedEffect(userId) { store.refresh() }

    val list = memberships
    when {
        pendingInvite != null -> JoinScreen(pendingInvite!!, onDone = { app.prefs.setPendingInvite(null) })
        list == null && error != null -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorText(error)
                TextButton(onClick = { scope.launch { store.refresh() } }) { Text("Reintentar") }
            }
        }
        list == null -> Centered { CircularProgressIndicator() }
        // Sin grupos, la pantalla de grupos funciona como bienvenida; al crear o unirse aparece la app.
        list.isEmpty() -> GroupsScreen(onOpen = {}, onJoined = {})
        else -> MainNav()
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("places", "Lugares", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    Tab("map", "Mapa", Icons.Outlined.Map, Icons.Filled.Map),
    Tab("groups", "Grupos", Icons.Outlined.Group, Icons.Filled.Group),
    Tab("profile", "Perfil", Icons.Outlined.Person, Icons.Filled.Person),
)

/** Barra inferior como la de gastos: fondo surface, borde arriba y sin indicador de pastilla. */
@Composable
private fun BottomBar(current: String?, onSelect: (Tab) -> Unit) {
    val t = Plancito.colors
    Column(Modifier.background(t.surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = t.border)
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = current == tab.route
                val color = if (selected) t.primaryText else t.textMuted
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (selected) tab.selectedIcon else tab.icon, null, tint = color, modifier = Modifier.size(22.dp))
                    Text(
                        tab.label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val idArg = listOf(navArgument("id") { type = NavType.StringType })

    Scaffold(
        bottomBar = {
            // Detalle, formularios y reseña ocupan toda la pantalla.
            if (tabs.any { it.route == current }) BottomBar(current) { tab -> nav.openTab(tab.route) }
        },
    ) { padding ->
        NavHost(nav, startDestination = "places", modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable("places") {
                PlacesScreen(
                    onOpen = { id -> nav.navigate("place/$id") },
                    onAdd = { nav.navigate("new-place") },
                    onManageGroups = { nav.openTab("groups") },
                )
            }
            composable("map") {
                MapScreen(onOpen = { id -> nav.navigate("place/$id") }, onManageGroups = { nav.openTab("groups") })
            }
            composable("groups") {
                GroupsScreen(onOpen = { id -> nav.navigate("group/$id") }, onJoined = { nav.openTab("places") })
            }
            composable("profile") { ProfileScreen() }
            composable("new-place") {
                PlaceFormScreen(
                    placeId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = { id -> nav.navigate("place/$id") { popUpTo("new-place") { inclusive = true } } },
                )
            }
            composable("place/{id}", arguments = idArg) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                PlaceDetailScreen(
                    placeId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("place/$id/edit") },
                    onReview = { nav.navigate("place/$id/review") },
                )
            }
            composable("place/{id}/edit", arguments = idArg) { entry ->
                PlaceFormScreen(placeId = entry.arguments?.getString("id"), onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable("place/{id}/review", arguments = idArg) { entry ->
                ReviewScreen(placeId = entry.arguments?.getString("id").orEmpty(), onDone = { nav.popBackStack() })
            }
            composable("group/{id}", arguments = idArg) { entry ->
                GroupDetailScreen(groupId = entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }
    }
}
