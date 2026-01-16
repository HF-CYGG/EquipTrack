package com.equiptrack.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.ui.auth.LoginScreen
import com.equiptrack.android.ui.auth.SignupScreen
import com.equiptrack.android.ui.main.MainScreen
import com.equiptrack.android.ui.onboarding.OnboardingScreen
import com.equiptrack.android.ui.settings.ServerConfigScreen
import com.equiptrack.android.ui.settings.ThemeCustomizeScreen
import com.equiptrack.android.ui.components.AnimatedPage
import com.equiptrack.android.ui.components.PageTransitionType
import javax.inject.Inject
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.equiptrack.android.ui.splash.SplashScreen

@Composable
fun EquipTrackNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navVm: NavigationViewModel = hiltViewModel()
    
    // Listen for session expiry events
    LaunchedEffect(Unit) {
        navVm.sessionExpiredEvent.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(
            route = Screen.Splash.route,
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login?fromSplash=true") {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(500)) }
        ) {
            AnimatedPage(
                transitionType = PageTransitionType.FADE
            ) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = "login?fromSplash={fromSplash}",
            arguments = listOf(
                androidx.navigation.navArgument("fromSplash") {
                    defaultValue = false
                    type = androidx.navigation.NavType.BoolType
                }
            ),
            enterTransition = {
                if (initialState.destination.route == Screen.Splash.route) {
                    // Custom transition from Splash: Just Fade In (Logo animation handled internally)
                    fadeIn(animationSpec = tween(800))
                } else {
                    fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.95f, animationSpec = tween(800))
                }
            },
            exitTransition = { fadeOut(animationSpec = tween(500)) },
            popEnterTransition = { fadeIn(animationSpec = tween(500)) }
        ) { backStackEntry ->
            // Reuse existing ViewModel instance if possible, or let hilt provide one scoped to this nav graph entry
            val navVm: NavigationViewModel = hiltViewModel()
            val fromSplash = backStackEntry.arguments?.getBoolean("fromSplash") ?: false
            
            AnimatedPage(
                transitionType = PageTransitionType.FADE
            ) {
                LoginScreen(
                    fromSplash = fromSplash,
                    onLoginSuccess = {
                        navVm.checkAndUploadFcmToken()
                        val needsSetup = !navVm.settingsRepository.isSetupCompleted()
                        if (needsSetup) {
                            navController.navigate(Screen.ServerConfig.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onNavigateToServerConfig = { navController.navigate(Screen.ServerConfig.route) }
                )
            }
        }
        
        composable(Screen.Signup.route) {
            AnimatedPage(
                transitionType = PageTransitionType.SLIDE_UP
            ) {
                SignupScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSignupSuccess = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(
            route = Screen.Main.route,
            enterTransition = {
                if (initialState.destination.route == Screen.Splash.route) {
                    fadeIn(animationSpec = tween(800))
                } else {
                    fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500))
                }
            }
        ) {
            AnimatedPage(
                transitionType = PageTransitionType.SCALE
            ) {
                MainScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToServerConfig = {
                        navController.navigate(Screen.ServerConfig.route)
                    },
                    onNavigateToThemeCustomize = {
                        navController.navigate(Screen.ThemeCustomize.route)
                    }
                )
            }
        }

        composable(Screen.ServerConfig.route) {
            // Obtain ViewModel in a @Composable context, use inside callbacks
            // val navVm: NavigationViewModel = hiltViewModel() // Shadowed variable removed
            AnimatedPage(
                transitionType = PageTransitionType.SLIDE_DOWN
            ) {
                ServerConfigScreen(
                    onNavigateBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            val needsSetup = !navVm.settingsRepository.isSetupCompleted()
                            if (needsSetup || !navVm.authRepository.isLoggedIn()) {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    onConfigSaved = {
                        // After saving config, if user is logged in go to Main, else back to Login
                        if (navVm.authRepository.isLoggedIn()) {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable(Screen.ThemeCustomize.route) {
            AnimatedPage(transitionType = PageTransitionType.SLIDE_HORIZONTAL) {
                ThemeCustomizeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Main : Screen("main")
    object ServerConfig : Screen("server_config")
    object ThemeCustomize : Screen("theme_customize")
}
