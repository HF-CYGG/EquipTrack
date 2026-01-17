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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
            exitTransition = {
                if (targetState.destination.route == Screen.Main.route) {
                    // 丝滑过渡：放大并淡出，制造"穿越"效果
                    fadeOut(animationSpec = tween(800, easing = FastOutSlowInEasing)) + 
                    scaleOut(targetScale = 1.2f, animationSpec = tween(800, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
                } else {
                    fadeOut(animationSpec = tween(500))
                }
            }
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
            exitTransition = {
                if (targetState.destination.route == Screen.Main.route) {
                    // 深度优化：视差退出 + 缩放退后效果
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 }, // 仅移动 1/3 距离，产生视差
                        animationSpec = tween(600, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                    ) + scaleOut(
                        targetScale = 0.92f, // 轻微缩小，产生纵深感
                        animationSpec = tween(600, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                    ) + fadeOut(
                        animationSpec = tween(600)
                    )
                } else {
                    fadeOut(animationSpec = tween(500))
                }
            },
            popEnterTransition = { fadeIn(animationSpec = tween(500)) }
        ) { backStackEntry ->
            // Reuse existing ViewModel instance if possible, or let hilt provide one scoped to this nav graph entry
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
                            navController.navigate(Screen.ServerConfig.route + "?fromLogin=true") {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onNavigateToServerConfig = { navController.navigate(Screen.ServerConfig.route + "?fromLogin=true") }
                )
            }
        }
        
        composable(
            route = Screen.Signup.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
            }
        ) {
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
                val fromRoute = initialState.destination.route
                if (fromRoute?.startsWith("login") == true) {
                    // 深度优化：从右侧覆盖进入，使用极度平滑的曲线
                    slideInHorizontally(
                        initialOffsetX = { it }, 
                        animationSpec = tween(600, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                    ) + fadeIn(
                        animationSpec = tween(400) // 快速淡入，避免黑边
                    )
                } else if (fromRoute == Screen.Splash.route) {
                    // 丝滑过渡：配合 Splash 的放大退出，主页从小放大进入
                    fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) + 
                    scaleIn(initialScale = 0.92f, animationSpec = tween(800, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
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
                        navController.navigate(Screen.ServerConfig.route + "?fromLogin=false")
                    },
                    onNavigateToThemeCustomize = {
                        navController.navigate(Screen.ThemeCustomize.route)
                    }
                )
            }
        }

        composable(
            route = Screen.ServerConfig.route + "?fromLogin={fromLogin}",
            arguments = listOf(
                androidx.navigation.navArgument("fromLogin") {
                    defaultValue = true
                    type = androidx.navigation.NavType.BoolType
                }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(600, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                ) + fadeIn(animationSpec = tween(600))
            },
            exitTransition = {
                // 延长退出动画时间，防止返回登录页时出现白屏闪烁
                fadeOut(animationSpec = tween(800))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(600))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(600, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                ) + fadeOut(animationSpec = tween(600))
            }
        ) { backStackEntry ->
            // Obtain ViewModel in a @Composable context, use inside callbacks
            // val navVm: NavigationViewModel = hiltViewModel() // Shadowed variable removed
            val fromLogin = backStackEntry.arguments?.getBoolean("fromLogin") ?: true
            ServerConfigScreen(
                showFluidBackground = fromLogin,
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

        composable(
            route = Screen.ThemeCustomize.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
            }
        ) {
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
