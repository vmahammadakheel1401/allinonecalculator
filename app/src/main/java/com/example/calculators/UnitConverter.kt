package com.example.calculators

enum class UnitCategory {
    Length, Weight, Area, Volume, Temperature, Data, Speed
}

data class UnitItem(val id: String, val name: String, val category: UnitCategory, val factorToTarget: Double = 1.0)

object UnitConverter {
    val units = listOf(
        // Length (Target: Meter)
        UnitItem("m", "Meter", UnitCategory.Length, 1.0),
        UnitItem("km", "Kilometer", UnitCategory.Length, 1000.0),
        UnitItem("cm", "Centimeter", UnitCategory.Length, 0.01),
        UnitItem("mm", "Millimeter", UnitCategory.Length, 0.001),
        UnitItem("mi", "Mile", UnitCategory.Length, 1609.344),
        UnitItem("yd", "Yard", UnitCategory.Length, 0.9144),
        UnitItem("ft", "Foot", UnitCategory.Length, 0.3048),
        UnitItem("in", "Inch", UnitCategory.Length, 0.0254),

        // Weight (Target: Kilogram)
        UnitItem("kg", "Kilogram", UnitCategory.Weight, 1.0),
        UnitItem("g", "Gram", UnitCategory.Weight, 0.001),
        UnitItem("mg", "Milligram", UnitCategory.Weight, 0.000001),
        UnitItem("lb", "Pound", UnitCategory.Weight, 0.45359237),
        UnitItem("oz", "Ounce", UnitCategory.Weight, 0.02834952),
        
        // Area (Target: Square Meter)
        UnitItem("sq_m", "Square Meter", UnitCategory.Area, 1.0),
        UnitItem("sq_km", "Square Kilometer", UnitCategory.Area, 1000000.0),
        UnitItem("ha", "Hectare", UnitCategory.Area, 10000.0),
        UnitItem("acre", "Acre", UnitCategory.Area, 4046.8564224),
        UnitItem("sq_ft", "Square Foot", UnitCategory.Area, 0.09290304),
        
        // Volume (Target: Liter)
        UnitItem("l", "Liter", UnitCategory.Volume, 1.0),
        UnitItem("ml", "Milliliter", UnitCategory.Volume, 0.001),
        UnitItem("gal", "US Gallon", UnitCategory.Volume, 3.78541178),
        UnitItem("qt", "US Quart", UnitCategory.Volume, 0.946352946),
        UnitItem("pt", "US Pint", UnitCategory.Volume, 0.473176473),
        UnitItem("cup", "US Cup", UnitCategory.Volume, 0.24),
        
        // Temperature (handled specially)
        UnitItem("c", "Celsius", UnitCategory.Temperature),
        UnitItem("f", "Fahrenheit", UnitCategory.Temperature),
        UnitItem("k", "Kelvin", UnitCategory.Temperature),
        
        // Data (Target: Byte)
        UnitItem("bit", "Bit", UnitCategory.Data, 0.125),
        UnitItem("b", "Byte", UnitCategory.Data, 1.0),
        UnitItem("kb", "Kilobyte", UnitCategory.Data, 1000.0),
        UnitItem("mb", "Megabyte", UnitCategory.Data, 1000.0 * 1000.0),
        UnitItem("gb", "Gigabyte", UnitCategory.Data, 1000.0 * 1000.0 * 1000.0),
        UnitItem("tb", "Terabyte", UnitCategory.Data, 1000.0 * 1000.0 * 1000.0 * 1000.0),
        
        // Speed (Target: Meter per Second)
        UnitItem("m_s", "Meter per second", UnitCategory.Speed, 1.0),
        UnitItem("km_h", "Kilometer per hour", UnitCategory.Speed, 1.0/3.6),
        UnitItem("mi_h", "Mile per hour", UnitCategory.Speed, 0.44704)
    )

    fun getUnitsForCategory(category: UnitCategory): List<UnitItem> {
        return units.filter { it.category == category }
    }

    fun convert(value: Double, fromUnit: UnitItem, toUnit: UnitItem): Double {
        if (fromUnit.category != toUnit.category) throw IllegalArgumentException("Cannot convert between different categories")
        
        if (fromUnit.category == UnitCategory.Temperature) {
            val celsius = when (fromUnit.id) {
                "c" -> value
                "f" -> (value - 32) * 5 / 9
                "k" -> value - 273.15
                else -> value
            }
            return when (toUnit.id) {
                "c" -> celsius
                "f" -> celsius * 9 / 5 + 32
                "k" -> celsius + 273.15
                else -> celsius
            }
        }
        
        val targetValue = value * fromUnit.factorToTarget
        return targetValue / toUnit.factorToTarget
    }
}
