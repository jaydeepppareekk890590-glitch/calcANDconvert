package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext

data class UnitOption(val name: String, val factor: Double)
data class ConversionCategory(val name: String, val units: List<UnitOption>, val note: String = "")

object UnitConverter {
    private fun units(vararg values: Pair<String, Double>) = values.map { UnitOption(it.first, it.second) }

    val categories = listOf(
        ConversionCategory("Angle", units("Degrees (°)" to 1.0, "Radians (rad)" to 180.0 / Math.PI, "Gradians (gon)" to 0.9, "Turns" to 360.0)),
        ConversionCategory("Area", units("Square metres (m²)" to 1.0, "Square kilometres (km²)" to 1e6, "Square centimetres (cm²)" to 1e-4, "Square feet (ft²)" to 0.09290304, "Square inches (in²)" to 0.00064516, "Acres (international)" to 4046.8564224, "Hectares (ha)" to 10000.0, "Ares (a)" to 100.0, "Bigha (custom region)" to 1.0, "Biswa (custom region)" to 1.0), "Bigha and Biswa vary by region. Enter your regional definition when using them."),
        ConversionCategory("Data", units("Bits (b)" to 0.125, "Bytes (B)" to 1.0, "Kilobytes (kB)" to 1e3, "Megabytes (MB)" to 1e6, "Gigabytes (GB)" to 1e9, "Terabytes (TB)" to 1e12, "Kibibytes (KiB)" to 1024.0, "Mebibytes (MiB)" to 1048576.0, "Gibibytes (GiB)" to 1073741824.0, "Tebibytes (TiB)" to 1099511627776.0), "Decimal units use 1000; binary units use 1024. Eight bits equal one byte."),
        ConversionCategory("Energy", units("Joules (J)" to 1.0, "Kilojoules (kJ)" to 1000.0, "Calories (thermochemical)" to 4.184, "Kilocalories (kcal)" to 4184.0, "Watt-hours (Wh)" to 3600.0, "Kilowatt-hours (kWh)" to 3600000.0, "BTU (IT)" to 1055.05585262)),
        ConversionCategory("Force", units("Newtons (N)" to 1.0, "Kilonewtons (kN)" to 1000.0, "Pound-force (lbf)" to 4.4482216152605, "Kilogram-force (kgf)" to 9.80665, "Dynes (dyn)" to 1e-5)),
        ConversionCategory("Fuel", units("Litres / 100 km" to 1.0, "Kilometres / litre" to 1.0, "Miles / US gallon" to 235.21458333333333, "Miles / UK gallon" to 282.48093633182216), "Fuel economy uses reciprocal conversion. Values must be greater than zero. US and UK gallons differ."),
        ConversionCategory("Length", units("Metres (m)" to 1.0, "Kilometres (km)" to 1000.0, "Centimetres (cm)" to 0.01, "Millimetres (mm)" to 0.001, "Micrometres (µm)" to 1e-6, "Inches (in)" to 0.0254, "Feet (ft)" to 0.3048, "Yards (yd)" to 0.9144, "Miles (mi)" to 1609.344, "Nautical miles (nmi)" to 1852.0)),
        ConversionCategory("Power", units("Watts (W)" to 1.0, "Kilowatts (kW)" to 1000.0, "Megawatts (MW)" to 1e6, "Horsepower (mechanical)" to 745.6998715822702, "Horsepower (metric)" to 735.49875, "BTU (IT) / hour" to 0.2930710701722222)),
        ConversionCategory("Pressure", units("Pascals (Pa)" to 1.0, "Kilopascals (kPa)" to 1000.0, "Megapascals (MPa)" to 1e6, "Bar" to 100000.0, "Millibar (mbar)" to 100.0, "Atmospheres (atm)" to 101325.0, "PSI (lbf/in²)" to 6894.757293168, "Torr" to 101325.0 / 760.0)),
        ConversionCategory("Speed", units("Metres / second" to 1.0, "Kilometres / hour" to 1.0 / 3.6, "Miles / hour" to 0.44704, "Feet / second" to 0.3048, "Knots" to 1852.0 / 3600.0)),
        ConversionCategory("Temperature", units("Celsius (°C)" to 1.0, "Fahrenheit (°F)" to 1.0, "Kelvin (K)" to 1.0), "Temperature uses offsets, not just multiplication. Values below absolute zero are rejected."),
        ConversionCategory("Time", units("Seconds (s)" to 1.0, "Milliseconds (ms)" to 0.001, "Microseconds (µs)" to 1e-6, "Minutes" to 60.0, "Hours" to 3600.0, "Days" to 86400.0, "Weeks" to 604800.0, "Years (365.25 days)" to 31557600.0)),
        ConversionCategory("Volume", units("Litres (L)" to 1.0, "Millilitres (mL)" to 0.001, "Cubic metres (m³)" to 1000.0, "Cubic centimetres (cm³)" to 0.001, "US gallons" to 3.785411784, "UK gallons" to 4.54609, "US quarts" to 0.946352946, "US fluid ounces" to 0.0295735295625, "UK fluid ounces" to 0.0284130625, "US cups" to 0.2365882365, "US tablespoons" to 0.01478676478125, "US teaspoons" to 0.00492892159375)),
        ConversionCategory("Weight", units("Kilograms (kg)" to 1.0, "Grams (g)" to 0.001, "Milligrams (mg)" to 1e-6, "Tonnes (t)" to 1000.0, "Pounds (lb)" to 0.45359237, "Ounces (oz)" to 0.028349523125, "Stone (st)" to 6.35029318, "US tons (short)" to 907.18474, "UK tons (long)" to 1016.0469088), "Weight here converts mass units; force units are in Force.")
    )

    fun convert(input: String, category: Int, from: Int, to: Int, bigha: String = "", biswa: String = ""): Double {
        require(input.length <= 80) { "Enter a shorter number" }
        val decimal = input.trim().toBigDecimalOrNull()
        val value = input.trim().toDoubleOrNull()
        require(decimal != null && (decimal.signum() == 0 || value != 0.0)) { "Number is outside the supported range" }
        require(value != null && value.isFinite() && (value == 0.0 || kotlin.math.abs(value) >= 1e-300) && kotlin.math.abs(value) <= 1e300) { "Enter a valid number between -1e300 and 1e300" }
        require(category in categories.indices) { "Choose a category" }
        val group = categories[category]
        require(from in group.units.indices && to in group.units.indices) { "Choose valid units" }
        require(group.name in listOf("Angle", "Temperature") || value >= 0.0) { "This quantity cannot be negative" }
        val result = when (group.name) {
            "Temperature" -> {
                require(value >= when (from) { 0 -> -273.15; 1 -> -459.67; else -> 0.0 }) { "Temperature cannot be below absolute zero" }
                val kelvin = when (from) { 0 -> value + 273.15; 1 -> (value - 32.0) * 5.0 / 9.0 + 273.15; else -> value }
                require(kelvin >= -1e-10) { "Temperature cannot be below absolute zero" }
                val safe = kelvin.coerceAtLeast(0.0)
                when (to) { 0 -> safe - 273.15; 1 -> (safe - 273.15) * 9.0 / 5.0 + 32.0; else -> safe }
            }
            "Fuel" -> {
                require(value > 0.0) { "Fuel economy must be greater than zero" }
                val litres = when (from) { 0 -> value; 1 -> 100.0 / value; else -> group.units[from].factor / value }
                when (to) { 0 -> litres; 1 -> 100.0 / litres; else -> group.units[to].factor / litres }
            }
            else -> {
                fun factor(index: Int): Double {
                    if (group.name != "Area" || index < 8) return group.units[index].factor
                    val area = bigha.toDoubleOrNull()
                    require(area != null && area.isFinite() && area in 1e-9..1e12) { "Enter square metres per Bigha for your region" }
                    if (index == 8) return area
                    val count = biswa.toDoubleOrNull()
                    require(count != null && count.isFinite() && count in 1.0..10000.0) { "Enter Biswa per Bigha for your region" }
                    return area / count
                }
                BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(factor(from))).divide(BigDecimal.valueOf(factor(to)), MathContext(16)).toDouble()
            }
        }
        require(result.isFinite() && (value == 0.0 || result != 0.0 || group.name == "Temperature")) { "Result is outside the supported range" }
        return if (result == -0.0) 0.0 else result
    }

    fun format(value: Double): String = CalculatorEngine.format(BigDecimal.valueOf(value).round(MathContext(12)))
}
