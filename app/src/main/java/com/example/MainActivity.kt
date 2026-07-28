package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.api.ApiService
import com.example.data.api.AuthInterceptor
import com.example.data.repository.AuthRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ScanRepository
import com.example.session.SessionManager
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.ScanViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        enableEdgeToEdge()

        // 2. Network client setup with interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(applicationContext))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val apiService = Retrofit.Builder()
            .baseUrl("https://petmoodai-dsa4f0bybtb8fzeh.eastasia-01.azurewebsites.net")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)

        // 3. Repositories
        val scanRepository = ScanRepository(apiService)
        val authRepository = AuthRepository()
        val petRepository = PetRepository(apiService)


        // 4. ViewModels
        // Ideally these should use viewModel() factory to survive config changes
        val authViewModel = AuthViewModel(authRepository)
        val petViewModel = PetViewModel(petRepository)
        val scanViewModel = ScanViewModel(scanRepository, petRepository)
        val historyViewModel = HistoryViewModel(scanRepository)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Verify login state statically
                val isLoggedIn = authViewModel.isLoggedIn
                val petProfile by scanViewModel.petProfile.collectAsState()

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
