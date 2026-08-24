package com.example.calculators

import kotlin.math.abs

enum class UnitCategory {
    Length, Weight, Area, Volume, Temperature, Data, Speed
}

data class UnitItem(
    val id: String,
    val name: String,
    val symbol: String,
    val category: UnitCategory,
    val factorToTarget: Double = 1.0
)

object UnitConverter {
    val units = listOf(
        // Length (Target: Meter)
        UnitItem("m", "Meter", "m", UnitCategory.Length, 1.0),
        UnitItem("km", "Kilometer", "km", UnitCategory.Length, 1000.0),
        UnitItem("cm", "Centimeter", "cm", UnitCategory.Length, 0.01),
        UnitItem("mm", "Millimeter", "mm", UnitCategory.Length, 0.001),
        UnitItem("mi", "Mile", "mi", UnitCategory.Length, 1609.344),
        UnitItem("yd", "Yard", "yd", UnitCategory.Length, 0.9144),
        UnitItem("ft", "Foot", "ft", UnitCategory.Length, 0.3048),
        UnitItem("in", "Inch", "in", UnitCategory.Length, 0.0254),

        // Weight (Target: Kilogram)
        UnitItem("kg", "Kilogram", "kg", UnitCategory.Weight, 1.0),
        UnitItem("g", "Gram", "g", UnitCategory.Weight, 0.001),
        UnitItem("mg", "Milligram", "mg", UnitCategory.Weight, 0.000001),
        UnitItem("lb", "Pound", "lb", UnitCategory.Weight, 0.45359237),
        UnitItem("oz", "Ounce", "oz", UnitCategory.Weight, 0.02834952),
        
        // Area (Target: Square Meter)
        UnitItem("sq_m", "Square Meter", "m²", UnitCategory.Area, 1.0),
        UnitItem("sq_km", "Square Kilometer", "km²", UnitCategory.Area, 1000000.0),
        UnitItem("ha", "Hectare", "ha", UnitCategory.Area, 10000.0),
        UnitItem("acre", "Acre", "ac", UnitCategory.Area, 4046.8564224),
        UnitItem("sq_ft", "Square Foot", "ft²", UnitCategory.Area, 0.09290304),
        
        // Volume (Target: Liter)
        UnitItem("l", "Liter", "L", UnitCategory.Volume, 1.0),
        UnitItem("ml", "Milliliter", "mL", UnitCategory.Volume, 0.001),
        UnitItem("gal", "US Gallon", "gal", UnitCategory.Volume, 3.785411784),
        UnitItem("qt", "US Quart", "qt", UnitCategory.Volume, 0.946352946),
        UnitItem("pt", "US Pint", "pt", UnitCategory.Volume, 0.473176473),
        UnitItem("cup", "US Cup", "cup", UnitCategory.Volume, 0.24),
        
        // Temperature
        UnitItem("c", "Celsius", "°C", UnitCategory.Temperature),
        UnitItem("f", "Fahrenheit", "°F", UnitCategory.Temperature),
        UnitItem("k", "Kelvin", "K", UnitCategory.Temperature),
        UnitItem("r", "Rankine", "°R", UnitCategory.Temperature),
        
        // Data (Target: Byte)
        UnitItem("bit", "Bit", "bit", UnitCategory.Data, 0.125),
        UnitItem("b", "Byte", "B", UnitCategory.Data, 1.0),
        UnitItem("kb", "Kilobyte", "KB", UnitCategory.Data, 1000.0),
        UnitItem("mb", "Megabyte", "MB", UnitCategory.Data, 1000.0 * 1000.0),
        UnitItem("gb", "Gigabyte", "GB", UnitCategory.Data, 1000.0 * 1000.0 * 1000.0),
        UnitItem("tb", "Terabyte", "TB", UnitCategory.Data, 1000.0 * 1000.0 * 1000.0 * 1000.0),
        
        // Speed (Target: Meter per Second)
        UnitItem("m_s", "Meter per second", "m/s", UnitCategory.Speed, 1.0),
        UnitItem("km_h", "Kilometer per hour", "km/h", UnitCategory.Speed, 1.0 / 3.6),
        UnitItem("mi_h", "Mile per hour", "mph", UnitCategory.Speed, 0.44704)
    )

    fun getUnitsForCategory(category: UnitCategory): List<UnitItem> {
        return units.filter { it.category == category }
    }

    fun convert(value: Double, fromUnit: UnitItem, toUnit: UnitItem): Double {
        if (fromUnit.category != toUnit.category) {
            throw IllegalArgumentException("Cannot convert between different categories")
        }
        
        if (fromUnit.id == toUnit.id) return value

        if (fromUnit.category == UnitCategory.Temperature) {
            // Convert any input temperature unit to Celsius
            val celsius = when (fromUnit.id) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                "r" -> (value - 491.67) * 5.0 / 9.0
                else -> value
            }

            // Convert Celsius to the destination temperature unit
            val result = when (toUnit.id) {
                "c" -> celsius
                "f" -> (celsius * 9.0 / 5.0) + 32.0
                "k" -> celsius + 273.15
                "r" -> (celsius + 273.15) * 1.8
                else -> celsius
            }

            return if (abs(result) < 1e-11) 0.0 else result
        }
        
        val targetValue = value * fromUnit.factorToTarget
        val result = targetValue / toUnit.factorToTarget
        return if (abs(result) < 1e-11) 0.0 else result
    }

    fun getTemperatureFormula(fromUnit: UnitItem, toUnit: UnitItem): String {
        if (fromUnit.id == toUnit.id) return "Same Unit"
        return when (fromUnit.id to toUnit.id) {
            "c" to "f" -> "°F = (°C × 9/5) + 32"
            "f" to "c" -> "°C = (°F - 32) × 5/9"
            "c" to "k" -> "K = °C + 273.15"
            "k" to "c" -> "°C = K - 273.15"
            "f" to "k" -> "K = (°F - 32) × 5/9 + 273.15"
            "k" to "f" -> "°F = (K - 273.15) × 9/5 + 32"
            "c" to "r" -> "°R = (°C + 273.15) × 9/5"
            "r" to "c" -> "°C = (°R - 491.67) × 5/9"
            "f" to "r" -> "°R = °F + 459.67"
            "r" to "f" -> "°F = °R - 459.67"
            else -> "Conversion Formula"
        }
    }
}
