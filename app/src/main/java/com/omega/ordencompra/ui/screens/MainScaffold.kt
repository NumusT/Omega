package com.omega.ordencompra.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.ui.components.ProOrderTopBar

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(
    currentTab: Int,
    onTabChange: (Int) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val items = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard),
        BottomNavItem("Pedidos", Icons.Default.ShoppingCart),
        BottomNavItem("Inventario", Icons.Default.Inventory2),
        BottomNavItem("Clientes", Icons.Default.People),
        BottomNavItem("Pagos", Icons.Default.Payments)
    )

    Scaffold(
        topBar = {
            Column {
                ProOrderTopBar(
                    title = "Omega"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentTab == index,
                        onClick = { onTabChange(index) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        content(Modifier.padding(paddingValues))
    }
}

