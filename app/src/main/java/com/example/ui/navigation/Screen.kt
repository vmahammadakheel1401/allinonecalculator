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
    object SGPA : Screen("sgpa")
    object Fuel : Screen("fuel")
    object GST : Screen("gst")
    object SIP : Screen("sip")
    object Salary : Screen("salary")
    object Inflation : Screen("inflation")
    object UnitPrice : Screen("unit_price")
    object Freelance : Screen("freelance")
    object History : Screen("history")
    object Settings : Screen("settings")
    object BMI : Screen("bmi")
    object Ovulation : Screen("ovulation")
}
