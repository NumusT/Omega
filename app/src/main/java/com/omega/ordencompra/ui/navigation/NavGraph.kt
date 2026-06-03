package com.omega.ordencompra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omega.ordencompra.ui.screens.CustomersScreen
import com.omega.ordencompra.ui.screens.DashboardScreen
import com.omega.ordencompra.ui.screens.DetalleOrdenScreen
import com.omega.ordencompra.ui.screens.InventoryScreen
import com.omega.ordencompra.ui.screens.ListaOrdenesScreen
import com.omega.ordencompra.ui.screens.LoginScreen
import com.omega.ordencompra.ui.screens.MainScaffold
import com.omega.ordencompra.ui.screens.NewOrderScreen
import com.omega.ordencompra.ui.screens.ReportesScreen
import com.omega.ordencompra.ui.screens.SuccessScreen
import com.omega.ordencompra.ui.screens.admin.GestionClientesScreen
import com.omega.ordencompra.ui.screens.admin.GestionProductosScreen
import com.omega.ordencompra.ui.screens.admin.GestionUsuariosScreen
import com.omega.ordencompra.viewmodel.AdminViewModel
import com.omega.ordencompra.viewmodel.AuthViewModel
import com.omega.ordencompra.viewmodel.OrdenViewModel

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val LISTA_ORDENES = "lista_ordenes"
    const val INVENTARIO = "inventario"
    const val CLIENTES = "clientes"
    const val CREAR_ORDEN = "crear_orden"
    const val DETALLE_ORDEN = "detalle_orden/{ordenId}"
    const val SUCCESS = "success/{ordenId}"
    const val REPORTES = "reportes"
    const val ADMIN_PRODUCTOS = "admin_productos"
    const val ADMIN_CLIENTES = "admin_clientes"
    const val ADMIN_USUARIOS = "admin_usuarios"

    fun detalleOrden(ordenId: String) = "detalle_orden/$ordenId"
    fun success(ordenId: String) = "success/$ordenId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    ordenViewModel: OrdenViewModel,
    adminViewModel: AdminViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val startDest = if (currentUser != null) Routes.DASHBOARD else Routes.LOGIN
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = { email, password, rememberMe ->
                    authViewModel.login(email, password, rememberMe)
                },
                isLoading = false,
                error = if (authViewModel.loginError.collectAsState().value) stringResource(com.omega.ordencompra.R.string.error_login) else null,
                initialEmail = authViewModel.savedUsername,
                initialPassword = authViewModel.savedPassword,
                initialRememberMe = authViewModel.savedRememberMe
            )
        }
        composable(Routes.DASHBOARD) {
            currentTab = 0
            val isAdmin = currentUser?.rol == "admin"
            LaunchedEffect(currentUser) {
                ordenViewModel.currentUserName = currentUser?.nombreCompleto ?: currentUser?.username ?: ""
            }
            MainScaffold(
                currentTab = currentTab,
                onTabChange = { tab ->
                    currentTab = tab
                    when (tab) {
                        1 -> navController.navigate(Routes.LISTA_ORDENES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                        2 -> navController.navigate(Routes.INVENTARIO) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                        3 -> navController.navigate(Routes.CLIENTES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                    }
                }
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier = modifier) {
                    DashboardScreen(
                        ordenViewModel = ordenViewModel,
                        isAdmin = isAdmin,
                        currentUserId = currentUser?.id ?: "",
                        onCreateOrder = { navController.navigate(Routes.CREAR_ORDEN) },
                        onGestionUsuarios = { navController.navigate(Routes.ADMIN_USUARIOS) },
                        onNavigateToClientes = {
                            if (isAdmin) {
                                navController.navigate(Routes.ADMIN_CLIENTES)
                            } else {
                                navController.navigate(Routes.CLIENTES)
                            }
                        },
                        onNavigateToProductos = { navController.navigate(Routes.ADMIN_PRODUCTOS) },
                        onNavigateToReportes = { navController.navigate(Routes.REPORTES) },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    )
                }
            }
        }
        composable(Routes.LISTA_ORDENES) {
            currentTab = 1
            val isAdmin = currentUser?.rol == "admin"
            MainScaffold(
                currentTab = currentTab,
                onTabChange = { tab ->
                    currentTab = tab
                    when (tab) {
                        0 -> navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true }; launchSingleTop = true }
                        2 -> navController.navigate(Routes.INVENTARIO) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                        3 -> navController.navigate(Routes.CLIENTES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                    }
                }
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier = modifier) {
                    ListaOrdenesScreen(
                        viewModel = ordenViewModel,
                        isAdmin = isAdmin,
                        currentUserId = currentUser?.id ?: "",
                        onCrear = { navController.navigate(Routes.CREAR_ORDEN) },
                        onOrdenClick = { ordenId -> navController.navigate(Routes.detalleOrden(ordenId)) }
                    )
                }
            }
        }
        composable(Routes.INVENTARIO) {
            currentTab = 2
            val isAdmin = currentUser?.rol == "admin"
            MainScaffold(
                currentTab = currentTab,
                onTabChange = { tab ->
                    currentTab = tab
                    when (tab) {
                        0 -> navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true }; launchSingleTop = true }
                        1 -> navController.navigate(Routes.LISTA_ORDENES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                        3 -> navController.navigate(Routes.CLIENTES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                    }
                }
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier = modifier) {
                    InventoryScreen(
                        adminViewModel = adminViewModel,
                        isAdmin = isAdmin,
                        onAddProduct = { navController.navigate(Routes.ADMIN_PRODUCTOS) }
                    )
                }
            }
        }
        composable(Routes.CLIENTES) {
            currentTab = 3
            val isAdmin = currentUser?.rol == "admin"
            MainScaffold(
                currentTab = currentTab,
                onTabChange = { tab ->
                    currentTab = tab
                    when (tab) {
                        0 -> navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true }; launchSingleTop = true }
                        1 -> navController.navigate(Routes.LISTA_ORDENES) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                        2 -> navController.navigate(Routes.INVENTARIO) { popUpTo(Routes.DASHBOARD) { inclusive = false }; launchSingleTop = true }
                    }
                }
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier = modifier) {
                    CustomersScreen(
                        adminViewModel = adminViewModel,
                        ordenViewModel = ordenViewModel,
                        isAdmin = isAdmin,
                        onAddClient = { navController.navigate(Routes.ADMIN_CLIENTES) }
                    )
                }
            }
        }
        composable(Routes.CREAR_ORDEN) {
            NewOrderScreen(
                ordenViewModel = ordenViewModel,
                usuarioId = currentUser?.id ?: "",
                onBack = { navController.popBackStack() },
                onSuccess = { ordenId ->
                    navController.navigate(Routes.success(ordenId)) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.SUCCESS,
            arguments = listOf(navArgument("ordenId") { type = NavType.StringType })
        ) {
            val ordenId = it.arguments?.getString("ordenId") ?: ""
            SuccessScreen(
                viewModel = ordenViewModel,
                ordenId = ordenId,
                onGoHome = {
                    navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true } }
                }
            )
        }
        composable(
            route = Routes.DETALLE_ORDEN,
            arguments = listOf(navArgument("ordenId") { type = NavType.StringType })
        ) {
            val ordenId = it.arguments?.getString("ordenId") ?: ""
            val isAdmin = currentUser?.rol == "admin"
            DetalleOrdenScreen(
                ordenId = ordenId,
                viewModel = ordenViewModel,
                isAdmin = isAdmin,
                currentUserId = currentUser?.id ?: "",
                onNavigateBack = { navController.popBackStack() },
                onDuplicateSuccess = { newId ->
                    navController.navigate(Routes.success(newId)) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ADMIN_USUARIOS) {
            GestionUsuariosScreen(authViewModel = authViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_PRODUCTOS) {
            val isAdmin = currentUser?.rol == "admin"
            GestionProductosScreen(
                adminViewModel = adminViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADMIN_CLIENTES) {
            val isAdmin = currentUser?.rol == "admin"
            GestionClientesScreen(
                adminViewModel = adminViewModel,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.REPORTES) {
            val isAdmin = currentUser?.rol == "admin"
            ReportesScreen(
                ordenViewModel = ordenViewModel,
                isAdmin = isAdmin,
                currentUserId = currentUser?.id ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

