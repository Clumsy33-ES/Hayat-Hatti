package com.hayathatti.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hayathatti.app.HayatHattiApp
import com.hayathatti.app.data.OutboxStatus
import com.hayathatti.app.ui.screens.LoginScreen
import com.hayathatti.app.ui.screens.NearbyScreen
import com.hayathatti.app.ui.screens.NewSignalScreen
import com.hayathatti.app.ui.screens.PermissionsScreen
import com.hayathatti.app.ui.screens.ProfileScreen
import com.hayathatti.app.ui.screens.QueueScreen
import com.hayathatti.app.ui.screens.RegisterScreen
import com.hayathatti.app.ui.screens.ServerScreen
import com.hayathatti.app.ui.screens.SignalDetailScreen
import com.hayathatti.app.ui.screens.SignalsScreen
import com.hayathatti.app.ui.screens.SosScreen
import com.hayathatti.app.ui.screens.SplashScreen
import com.hayathatti.app.ui.screens.SummaryScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object PermissionsRoute
@Serializable data object SosRoute
@Serializable data object NearbyRoute
@Serializable data object SignalsRoute
@Serializable data object SignalDetailRoute
@Serializable data object NewSignalRoute
@Serializable data object SummaryRoute
@Serializable data object ProfileRoute
@Serializable data object QueueRoute
@Serializable data object ServerRoute

private data class Tab(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(SosRoute, "SOS", Icons.Rounded.Sos),
    Tab(SignalsRoute, "Sinyaller", Icons.AutoMirrored.Rounded.FormatListBulleted),
    Tab(SummaryRoute, "Özet", Icons.Rounded.Insights),
    Tab(ProfileRoute, "Profil", Icons.Rounded.Person),
)

private fun NavHostController.goToTab(route: Any) {
    navigate(route) {
        popUpTo(SosRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Geri tuşuyla giriş ekranına dönülmesin diye yığın temizlenir. */
private fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppRoot() {
    val container = (LocalContext.current.applicationContext as HayatHattiApp).container
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = destination?.hierarchy?.any { d ->
        d.hasRoute(SosRoute::class) || d.hasRoute(SignalsRoute::class) || d.hasRoute(SummaryRoute::class) || d.hasRoute(ProfileRoute::class)
    } == true

    // Sekmeler arasında paylaşılır: Özet'ten seçilen tür Sinyaller'de açılır.
    val signalsVm = viewModel { SignalsViewModel(container) }
    val session by container.prefs.session.collectAsState()
    val outbox by container.outbox.all.collectAsState(initial = emptyList())
    val relays by container.outbox.relays.collectAsState(initial = emptyList())
    val nearbyCount = remember(scope) {
        container.outbox.relays.map { it.size }.stateIn(scope, SharingStarted.Eagerly, 0)
    }

    fun afterEntry() {
        nav.resetTo(if (container.prefs.permsSeen.value) SosRoute else PermissionsRoute)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = SplashRoute) {
                composable<SplashRoute> {
                    SplashScreen(viewModel { SplashViewModel(container) }) { entered ->
                        if (entered) afterEntry() else nav.resetTo(LoginRoute)
                    }
                }
                composable<LoginRoute> {
                    val vm = viewModel { AuthViewModel(container) }
                    LoginScreen(
                        vm = vm,
                        onSignedIn = ::afterEntry,
                        onRegister = { nav.navigate(RegisterRoute) },
                        onGuest = {
                            scope.launch {
                                container.prefs.enterAsGuest()
                                afterEntry()
                            }
                        },
                        onServer = { nav.navigate(ServerRoute) },
                    )
                }
                composable<RegisterRoute> {
                    RegisterScreen(viewModel { AuthViewModel(container) }, ::afterEntry) { nav.popBackStack() }
                }
                composable<PermissionsRoute> {
                    PermissionsScreen {
                        scope.launch {
                            container.prefs.markPermsSeen()
                            nav.resetTo(SosRoute)
                        }
                    }
                }
                composable<SosRoute> {
                    SosScreen(viewModel { SosViewModel(container) }, { nav.navigate(NearbyRoute) }, nearbyCount)
                }
                composable<NearbyRoute> {
                    NearbyScreen(relays, container.ble.isConnected) { nav.popBackStack() }
                }
                composable<SignalsRoute> {
                    SignalsScreen(
                        vm = signalsVm,
                        onOpen = { nav.navigate(SignalDetailRoute) },
                        onNew = { nav.navigate(NewSignalRoute) },
                        onLogin = { nav.navigate(LoginRoute) },
                    )
                }
                composable<SignalDetailRoute> {
                    val ui by signalsVm.ui.collectAsState()
                    SignalDetailScreen(ui.selected) { nav.popBackStack() }
                }
                composable<NewSignalRoute> {
                    NewSignalScreen(
                        vm = viewModel { NewSignalViewModel(container) },
                        onDone = { nav.popBackStack() },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable<SummaryRoute> {
                    SummaryScreen(viewModel { SummaryViewModel(container) }) { source, code ->
                        signalsVm.setSource(source)
                        signalsVm.setType(code)
                        nav.goToTab(SignalsRoute)
                    }
                }
                composable<ProfileRoute> {
                    ProfileScreen(
                        session = session,
                        waiting = outbox.count { it.status == OutboxStatus.PENDING.name },
                        onQueue = { nav.navigate(QueueRoute) },
                        onServer = { nav.navigate(ServerRoute) },
                        onLogin = { nav.navigate(LoginRoute) },
                        onLogout = {
                            scope.launch {
                                container.prefs.logout()
                                nav.resetTo(LoginRoute)
                            }
                        },
                    )
                }
                composable<QueueRoute> {
                    QueueScreen(
                        items = outbox,
                        onRetry = { scope.launch { container.outbox.retryNow() } },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable<ServerRoute> {
                    ServerScreen(viewModel { ServerViewModel(container) }) { nav.popBackStack() }
                }
            }
        }
    }
}
