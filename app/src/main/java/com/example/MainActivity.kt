package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.api.buildApiService
import com.example.data.repository.AuthRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ScanRepository
import com.example.session.SessionManager
import com.example.ui.components.BottomNavBar
import com.example.ui.components.rememberNotificationPermissionState
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.ScanViewModel

// "edit" defaults to false so callers that just want the plain first-time-setup route can
// navigate("pet_profile_setup") without thinking about the query param.
private const val ROUTE_PET_SETUP = "pet_profile_setup?edit={edit}"

class MainActivity : ComponentActivity() {
    companion object {
        /** Set by [com.example.data.api.AuthInterceptor] when it force-logs-out on a 401, so
         * the restarted Activity can explain why the user landed back on login instead of
         * restarting silently mid-task. */
        const val EXTRA_SESSION_EXPIRED = "session_expired"

        /** Set by [com.example.fcm.PetMoodFirebaseMessagingService] when a "scan complete"/
         * "scan failed" push is tapped, carrying the scan id from the notification's data
         * payload so the routing gate can open straight into that result. MainActivity's
         * launch mode is "standard" (no override in the manifest), and the notification's
         * PendingIntent uses CLEAR_TOP, which for standard mode always finishes any existing
         * instance and creates a fresh one — so reading this once in onCreate (like
         * [EXTRA_SESSION_EXPIRED]) is sufficient; no onNewIntent handling needed. */
        const val EXTRA_DEEPLINK_SCAN_ID = "deeplink_scan_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        enableEdgeToEdge()

        if (intent.getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            Toast.makeText(this, "Your session expired — please log in again.", Toast.LENGTH_LONG).show()
        }
        val deepLinkScanId = intent.getStringExtra(EXTRA_DEEPLINK_SCAN_ID)

        // 2. Network client setup with interceptor
        val apiService = buildApiService(applicationContext)

        // 3. Repositories
        val scanRepository = ScanRepository(apiService)
        val authRepository = AuthRepository(apiService)
        val petRepository = PetRepository(apiService)


        // 4. ViewModels
        // Ideally these should use viewModel() factory to survive config changes
        val authViewModel = AuthViewModel(authRepository)
        val petViewModel = PetViewModel(petRepository)
        val scanViewModel = ScanViewModel(scanRepository, petRepository)
        val historyViewModel = HistoryViewModel(scanRepository)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Verify login state statically
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                val petProfile by scanViewModel.petProfile.collectAsState()

                // petProfile is only ever populated by an explicit refresh (see ScanViewModel) —
                // fetch it whenever the session becomes active (fresh login, or app relaunch with
                // an existing session).
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) scanViewModel.refreshPetProfile()
                }

                // Keep HistoryViewModel synced with current pet
                LaunchedEffect(petProfile?.id) {
                    petProfile?.id?.let { historyViewModel.setPetId(it) }
                }

                // Only the persistent tabs get the bottom bar — "scan"/"results" are a linear
                // task layered on top of the tabs, not peer destinations (see nav review).
                val showBottomNav = currentRoute in listOf(
                    "home",
                    "history",
                    "settings"
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (!isFinishing) {
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "routing",
                        modifier = Modifier.padding(innerPadding)

                    ) {
                        // 0. Routing gate — the single place that decides where a logged-in
                        // user lands (home vs pet setup vs resuming a scan left "processing"
                        // when the app was last killed). Used by cold launch AND by
                        // login/register success, so that logic only lives in one place.
                        composable("routing") {
                            LaunchedEffect(Unit) {
                                if (isFinishing) return@LaunchedEffect
                                if (!SessionManager.isLoggedIn()) {
                                    navController.navigate("login") {
                                        popUpTo("routing") { inclusive = true }
                                    }
                                    return@LaunchedEffect
                                }
                                petViewModel.checkHasPets(
                                    onHasPets = {
                                        scanViewModel.refreshPetProfile()
                                        val petId = petViewModel.selectedPet.value?.id
                                        if (petId == null) {
                                            navController.navigate("home") {
                                                popUpTo("routing") { inclusive = true }
                                            }
                                            return@checkHasPets
                                        }
                                        // A tapped "scan complete"/"scan failed" notification —
                                        // go straight to that result instead of Home. Checked
                                        // before the pending-scan resume below since a deep
                                        // link always names a terminal (already-resolved) scan.
                                        if (deepLinkScanId != null) {
                                            val loaded = scanViewModel.loadScanById(petId, deepLinkScanId)
                                            navController.navigate("home") {
                                                popUpTo("routing") { inclusive = true }
                                            }
                                            if (loaded) {
                                                navController.navigate("results")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Couldn't open that scan result.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            return@checkHasPets
                                        }
                                        scanViewModel.checkForPendingScan(
                                            petId = petId,
                                            onPending = { scanId ->
                                                scanViewModel.resumePendingScan(petId, scanId)
                                                // Land on "home" first (not just "analysing")
                                                // so it's there underneath, same as the normal
                                                // scan flow — onAnalysisFailed relies on it.
                                                navController.navigate("home") {
                                                    popUpTo("routing") { inclusive = true }
                                                }
                                                navController.navigate("analysing")
                                            },
                                            onNone = {
                                                navController.navigate("home") {
                                                    popUpTo("routing") { inclusive = true }
                                                }
                                            }
                                        )
                                    },
                                    onNoPets = {
                                        navController.navigate("pet_profile_setup") {
                                            popUpTo("routing") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        // 1. Login Screen
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = {
                                    if (!isFinishing) navController.navigate("register")
                                },
                                onLoginSuccess = {
                                    if (!isFinishing) {
                                        navController.navigate("routing") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // 2. Register Screen
                        composable("register") {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    if (!isFinishing) navController.navigate("login")
                                },
                                onRegisterSuccess = {
                                    if (!isFinishing) {
                                        // A brand-new account never has pets yet, but routing
                                        // through here (rather than hardcoding pet_profile_setup)
                                        // keeps there being exactly one place that decides this.
                                        navController.navigate("routing") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Pet Profile Setup — same screen for first-time setup (from routing/
                        // register) and editing an existing pet (from Home/Settings), distinguished
                        // by the "edit" arg since the two need different post-save navigation.
                        composable(
                            ROUTE_PET_SETUP,
                            arguments = listOf(navArgument("edit") {
                                type = NavType.BoolType
                                defaultValue = false
                            })
                        ) { backStackEntry ->
                            val isEditMode = backStackEntry.arguments?.getBoolean("edit") ?: false
                            PetProfileScreen(
                                viewModel = petViewModel,
                                onProfileSaved = {
                                    scanViewModel.refreshPetProfile()
                                    if (!isFinishing) {
                                        if (isEditMode) {
                                            // Editing from Home/Settings: those screens are
                                            // already on the back stack, just return to them.
                                            navController.popBackStack()
                                        } else {
                                            navController.navigate("home") {
                                                popUpTo(ROUTE_PET_SETUP) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // 4. Home Dashboard Screen
                        composable("home") {
                            val notificationPermission = rememberNotificationPermissionState()
                            LaunchedEffect(Unit) {
                                if (!notificationPermission.hasPermission) {
                                    notificationPermission.requestPermission()
                                }
                            }
                            HomeScreen(
                                authViewModel = authViewModel,
                                scanViewModel = scanViewModel,
                                historyViewModel = historyViewModel,
                                onNavigateToScan = {
                                    if (!isFinishing) navController.navigate("scan")
                                },
                                onNavigateToProfile = {
                                    if (!isFinishing) navController.navigate("pet_profile_setup?edit=true")
                                },
                                onNavigateToScanDetails = {
                                    if (!isFinishing) navController.navigate("results")
                                }
                            )
                        }

                        // 5. Smart Scanner Screen
                        composable("scan") {
                            ScannerScreen(
                                viewModel = scanViewModel,
                                onNavigateToAnalysing = {
                                    if (!isFinishing) {
                                        // Inclusive: once analysis has started, "scan" shouldn't
                                        // still be reachable via back — leaving it on the stack
                                        // (saveState only) is what previously made back-from-
                                        // Results land on a stale Scanner screen instead of Home.
                                        navController.navigate("analysing") {
                                            popUpTo("scan") { inclusive = true }
                                        }
                                    }
                                },
                                onClickBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 6. Analysing Screen (Transitional loading)
                        composable("analysing") {
                            AnalysingScreen(
                                viewModel = scanViewModel,
                                onAnalysisFinished = {
                                    if (!isFinishing) {
                                        navController.navigate("results") {
                                            popUpTo("analysing") { inclusive = true }
                                        }
                                    }
                                },
                                onAnalysisFailed = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    // "scan" is no longer guaranteed to be on the stack (see
                                    // above), so re-navigate explicitly rather than popping —
                                    // "home" is always the destination directly beneath.
                                    if (!isFinishing) {
                                        navController.navigate("scan") {
                                            popUpTo("home")
                                        }
                                    }
                                }
                            )
                        }

                        // 7. Results Screen
                        composable("results") {
                            ResultsScreen(
                                viewModel = scanViewModel,
                                onScanAgain = {
                                    if (!isFinishing) {
                                        navController.navigate("scan") {
                                            popUpTo("results") { inclusive = true }
                                        }
                                    }
                                },
                                onSaveToHistory = {
                                    if (!isFinishing) {
                                        navController.navigate("history") {
                                            popUpTo("results") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // 8. History Screen
                        composable("history") {
                            HistoryScreen(
                                historyViewModel = historyViewModel,
                                scanViewModel = scanViewModel,
                                onClickBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToDetails = {
                                    if (!isFinishing) {
                                        navController.navigate("results") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        // 9. Settings Screen
                        composable("settings") {
                            SettingsScreen(
                                authViewModel = authViewModel,
                                scanViewModel = scanViewModel,
                                onEditPetProfile = {
                                    if (!isFinishing) navController.navigate("pet_profile_setup?edit=true")
                                },
                                onClickBack = {
                                    navController.popBackStack()
                                },
                                onLogout = {
                                    authViewModel.logout()
                                    if (!isFinishing) {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
