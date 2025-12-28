package com.equiptrack.android.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.equiptrack.android.ui.equipment.EquipmentScreen
import com.equiptrack.android.ui.history.HistoryScreen
import com.equiptrack.android.ui.profile.ProfileScreen
import com.equiptrack.android.ui.approval.ApprovalScreen
import com.equiptrack.android.ui.approval.BorrowApprovalScreen
import com.equiptrack.android.ui.user.UsersScreen
import com.equiptrack.android.ui.info.SystemInfoScreen
import com.equiptrack.android.ui.department.DepartmentScreen
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedPage
import com.equiptrack.android.ui.components.PageTransitionType
import com.equiptrack.android.ui.navigation.NavigationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToServerConfig: () -> Unit,
    onNavigateToThemeCustomize: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navVm: NavigationViewModel = hiltViewModel()
    val currentUser = navVm.authRepository.getCurrentUser()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            ) {
                Text(
                    text = "导航",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                
                val drawerItems = remember(currentUser) {
                    val items = mutableListOf(
                        MainNavItem.Equipment,
                        MainNavItem.History,
                        MainNavItem.Profile
                    )
                    if (PermissionChecker.hasPermission(currentUser, PermissionType.MANAGE_EQUIPMENT_ITEMS)) {
                        items.add(MainNavItem.BorrowApprovals)
                    }
                    if (PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_REGISTRATION_APPROVALS)) {
                        items.add(MainNavItem.Approvals)
                    }
                    if (PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_USER_MANAGEMENT)) {
                        items.add(MainNavItem.Users)
                    }
                    if (PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_DEPARTMENT_MANAGEMENT)) {
                        items.add(MainNavItem.Departments)
                    }
                    items
                }

                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("服务器配置") },
                    selected = false,
                    onClick = {
                        onNavigateToServerConfig()
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    label = { Text("主题与背景") },
                    selected = false,
                    onClick = {
                        onNavigateToThemeCustomize()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.8f)
                    ),
                    title = {
                        val routeLabel = when (currentDestination?.route) {
                            MainNavItem.Equipment.route -> "EquipTrack > 物资管理"
                            MainNavItem.History.route -> "EquipTrack > 借用记录"
                            MainNavItem.Profile.route -> "EquipTrack > 个人中心"
                            MainNavItem.BorrowApprovals.route -> "EquipTrack > 借用审批"
                            MainNavItem.Approvals.route -> "EquipTrack > 注册审批"
                            MainNavItem.Users.route -> "EquipTrack > 用户管理"
                            MainNavItem.Departments.route -> "EquipTrack > 部门管理"
                            MainNavItem.SystemInfo.route -> "EquipTrack > 系统说明"
                            else -> "EquipTrack"
                        }
                        Text(routeLabel)
                    },
                    navigationIcon = {
                        AnimatedIconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开侧边栏")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MainNavItem.Equipment.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
            ) {
                composable(MainNavItem.Equipment.route) {
                    EquipmentScreen(
                        navController = navController,
                        onNavigateToDetail = { }
                    )
                }
                composable(MainNavItem.History.route) {
                    HistoryScreen()
                }
                composable(MainNavItem.Profile.route) {
                    ProfileScreen(
                        onLogout = onLogout,
                        onNavigateToSettings = { onNavigateToServerConfig() },
                        onNavigateToSystemInfo = { navController.navigate(MainNavItem.SystemInfo.route) }
                    )
                }
                composable(MainNavItem.BorrowApprovals.route) {
                    BorrowApprovalScreen()
                }
                composable(MainNavItem.Approvals.route) {
                    ApprovalScreen()
                }
                composable(MainNavItem.Users.route) {
                    UsersScreen()
                }
                composable(MainNavItem.Departments.route) {
                    if (PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_DEPARTMENT_MANAGEMENT)) {
                        DepartmentScreen()
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "无权限", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("无权访问部门管理")
                            }
                        }
                    }
                }
                composable(MainNavItem.SystemInfo.route) {
                    SystemInfoScreen()
                }
            }
        }
    }
}

sealed class MainNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Equipment : MainNavItem("equipment", "物资管理", Icons.Default.Inventory)
    object History : MainNavItem("history", "借用记录", Icons.Default.History)
    object Profile : MainNavItem("profile", "个人中心", Icons.Default.Person)
    object BorrowApprovals : MainNavItem("borrow_approvals", "借用审批", Icons.Default.Assignment)
    object Approvals : MainNavItem("approvals", "注册审批", Icons.Default.CheckCircle)
    object Users : MainNavItem("users", "用户管理", Icons.Default.Group)
    object SystemInfo : MainNavItem("system_info", "系统说明", Icons.Default.Info)
    object Departments : MainNavItem("departments", "部门管理", Icons.Default.Business)
}

// 底部导航已移除，改为左侧可伸缩的侧边栏
