package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MapRouteScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val tag: String) {
  object Dashboard : Screen("dashboard", "Painel", Icons.Default.Dashboard, "nav_dashboard_tab")
  object Clients : Screen("clients", "Clientes", Icons.Default.Group, "nav_clients_tab")
  object Visits : Screen("visits", "Visitas", Icons.Default.Assignment, "nav_visits_tab")
  object Orders : Screen("orders", "Pedidos", Icons.Default.Receipt, "nav_orders_tab")
  object MapRoute : Screen("map_route", "Rota", Icons.Default.Map, "nav_map_tab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
  val navController = rememberNavController()
  val viewModel: MainViewModel = viewModel()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

  val items = listOf(
    Screen.Dashboard,
    Screen.Clients,
    Screen.Visits,
    Screen.Orders,
    Screen.MapRoute
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "VisitaPro",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              letterSpacing = (-0.5).sp
            )
            Text(
              text = "DASHBOARD DE VENDAS",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.testTag("app_top_bar")
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("app_bottom_nav_bar")
      ) {
        items.forEach { screen ->
          NavigationBarItem(
            icon = { Icon(screen.icon, contentDescription = screen.title) },
            label = { Text(screen.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            selected = currentRoute == screen.route,
            onClick = {
              if (currentRoute != screen.route) {
                navController.navigate(screen.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
            modifier = Modifier.testTag(screen.tag)
          )
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Screen.Dashboard.route,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      composable(Screen.Dashboard.route) {
        DashboardScreen(
          viewModel = viewModel,
          onNavigateToMap = {
            navController.navigate(Screen.MapRoute.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
              restoreState = true
            }
          }
        )
      }
      composable(Screen.Clients.route) {
        ClientsScreen(viewModel = viewModel)
      }
      composable(Screen.Visits.route) {
        VisitsScreen(
          viewModel = viewModel,
          onNavigateToMap = {
            navController.navigate(Screen.MapRoute.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
              restoreState = true
            }
          }
        )
      }
      composable(Screen.Orders.route) {
        OrdersScreen(viewModel = viewModel)
      }
      composable(Screen.MapRoute.route) {
        MapRouteScreen(viewModel = viewModel)
      }
    }
  }
}

