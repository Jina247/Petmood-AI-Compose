package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        enableEdgeToEdge()

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

                val showBottomNav = currentRoute in listOf(
                    "home",
                    "scan",
                    "results",
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
                        startDestination = if (isLoggedIn) "home" else "login",
                        modifier = Modifier.padding(innerPadding)

                    ) {
                        // 1. Login Screen
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = {
                                    if (!isFinishing) navController.navigate("register")
                                },
                                onLoginSuccess = {
                                    if (!isFinishing) {
                                        petViewModel.checkHasPets(
                                            onHasPets = {
                                                navController.navigate("home") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            },
                                            onNoPets = {
                                                navController.navigate("pet_profile_setup") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                        )
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
                                        navController.navigate("pet_profile_setup") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Pet Profile Setup
                        composable("pet_profile_setup") {
                            PetProfileScreen(
                                viewModel = petViewModel,
                                onProfileSaved = {
                                    scanViewModel.refreshPetProfile()
                                    if (!isFinishing) {
                                        navController.navigate("home") {
                                            popUpTo("pet_profile_setup") { inclusive = true }
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
                                    if (!isFinishing) navController.navigate("pet_profile_setup")
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
                                        navController.navigate("analysing") {
                                            popUpTo("scan") { saveState = true }
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
                                    if (!isFinishing) navController.popBackStack()
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
                                    if (!isFinishing) navController.navigate("pet_profile_setup")
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
