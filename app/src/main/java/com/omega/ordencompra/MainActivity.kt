package com.omega.ordencompra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.omega.ordencompra.ui.navigation.NavGraph
import com.omega.ordencompra.ui.theme.OrdenCompraOmegaTheme
import com.omega.ordencompra.viewmodel.AdminViewModel
import com.omega.ordencompra.viewmodel.AuthViewModel
import com.omega.ordencompra.viewmodel.OrdenViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrdenCompraOmegaTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val ordenViewModel: OrdenViewModel = hiltViewModel()
                val adminViewModel: AdminViewModel = hiltViewModel()
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    ordenViewModel = ordenViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }
}
