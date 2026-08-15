package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import com.example.ui.screens.*

@Composable
fun BannerAdPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Banner Ad Placeholder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppNavigation(
    database: AppDatabase,
    settingsManager: SettingsManager,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            Column {
                BannerAdPlaceholder()
                if (currentRoute in listOf(Screen.Home.route, Screen.History.route, Screen.Settings.route)) {
                    BottomNavigationBar(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Calculator.route) { CalculatorScreen(navController, database) }
            composable(Screen.Currency.route) { CurrencyScreen(navController, database, settingsManager) }
            composable(Screen.UnitConverter.route) { UnitConverterScreen(navController, database) }
            composable(Screen.Discount.route) { DiscountScreen(navController, database, settingsManager) }
            composable(Screen.Loan.route) { LoanScreen(navController, database, settingsManager) }
            composable(Screen.DateCalc.route) { DateScreen(navController, database) }
            composable(Screen.Age.route) { AgeScreen(navController, database) }
            composable(Screen.SGPA.route) { SGPAScreen(navController, database) }
            composable(Screen.Fuel.route) { FuelCostScreen(navController, database) }
            composable(Screen.GST.route) { GSTScreen(navController, database) }
            composable(Screen.SIP.route) { SIPScreen(navController, database) }
            composable(Screen.Salary.route) { SalaryScreen(navController, database, settingsManager) }
            composable(Screen.Inflation.route) { InflationScreen(navController, database) }
            composable(Screen.UnitPrice.route) { UnitPriceScreen(navController, database, settingsManager) }
            composable(Screen.Freelance.route) { FreelanceRateScreen(navController, database, settingsManager) }
            composable(Screen.History.route) { HistoryScreen(navController, database) }
            composable(Screen.Settings.route) { SettingsScreen(navController, settingsManager, database) }
        }
    }
}
