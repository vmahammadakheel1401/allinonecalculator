package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calculator : Screen("calculator")
    object Currency : Screen("currency")
    object UnitConverter : Screen("unit_converter")
    object Discount : Screen("discount")
    object Loan : Screen("loan")
    object DateCalc : Screen("date_calc")
    object Age : Screen("age")
    object BMI : Screen("bmi")
    object WorldClock : Screen("world_clock")
    object History : Screen("history")
    object Settings : Screen("settings")
}
