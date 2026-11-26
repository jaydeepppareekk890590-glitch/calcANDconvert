package com.example.calculator

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class UnitConverterTest {
    private fun convert(name: String, value: String, from: Int, to: Int, bigha: String = "", biswa: String = "") = UnitConverter.convert(value, UnitConverter.categories.indexOfFirst { it.name == name }, from, to, bigha, biswa)
    private fun close(expected: Double, actual: Double) = assertEquals(expected, actual, maxOf(abs(expected) * 1e-10, 1e-10))
    private fun rejects(name: String, value: String, from: Int = 0, to: Int = 1) {
        try { convert(name, value, from, to); fail("Should reject $name $value") } catch (_: IllegalArgumentException) { }
    }

    @Test fun allCategoriesAndRoundTrips() {
        assertEquals(14, UnitConverter.categories.size)
        assertFalse(UnitConverter.categories.any { it.name == "Currency" })
        UnitConverter.categories.forEachIndexed { category, group ->
            for (from in group.units.indices) for (to in group.units.indices) {
                val original = if (group.name == "Temperature") 300.0 else 123.456
                val result = UnitConverter.convert(original.toString(), category, from, to, "2529.285264", "20")
                val back = UnitConverter.convert(result.toString(), category, to, from, "2529.285264", "20")
                close(original, back)
            }
        }
    }

    @Test fun knownLinearConversions() {
        close(Math.PI, convert("Angle", "180", 0, 1))
        close(43560.0, convert("Area", "1", 5, 3))
        close(100.0, convert("Area", "1", 7, 0))
        close(8.0, convert("Data", "1", 1, 0))
        close(1000.0, convert("Data", "1", 3, 2))
        close(1024.0, convert("Data", "1", 7, 6))
        close(3600000.0, convert("Energy", "1", 5, 0))
        close(4.4482216152605, convert("Force", "1", 2, 0))
        close(1609.344, convert("Length", "1", 8, 0))
        close(0.3048, convert("Length", "1", 6, 0))
        close(735.49875, convert("Power", "1", 4, 0))
        close(101325.0, convert("Pressure", "1", 5, 0))
        close(1.0, convert("Speed", "3.6", 1, 0))
        close(3600.0, convert("Time", "1", 4, 0))
        close(3.785411784, convert("Volume", "1", 4, 0))
        close(4.54609, convert("Volume", "1", 5, 0))
        close(0.45359237, convert("Weight", "1", 4, 0))
    }

    @Test fun temperatureOffsetsAndLimits() {
        close(32.0, convert("Temperature", "0", 0, 1))
        close(100.0, convert("Temperature", "212", 1, 0))
        close(-40.0, convert("Temperature", "-40", 0, 1))
        close(0.0, convert("Temperature", "-273.15", 0, 2))
        close(0.0, convert("Temperature", "-459.67", 1, 2))
        close(-273.15, convert("Temperature", "0", 2, 0))
        rejects("Temperature", "-273.150001", 0, 2)
        rejects("Temperature", "-459.670001", 1, 2)
        rejects("Temperature", "-0.001", 2, 0)
    }

    @Test fun fuelReciprocalAndGallonDefinitions() {
        close(5.0, convert("Fuel", "20", 1, 0))
        close(20.0, convert("Fuel", "5", 0, 1))
        close(23.52145833333333, convert("Fuel", "10", 0, 2))
        close(28.248093633182216, convert("Fuel", "10", 0, 3))
        rejects("Fuel", "0")
        rejects("Fuel", "-1")
    }

    @Test fun regionalLandDefinitions() {
        rejects("Area", "1", 8, 0)
        close(2500.0, convert("Area", "1", 8, 0, "2500", "20"))
        close(125.0, convert("Area", "1", 9, 0, "2500", "20"))
        close(20.0, convert("Area", "1", 8, 9, "2500", "20"))
        close(1.0, convert("Area", "1", 0, 0))
    }

    @Test fun rejectsBadNumbersAndNonphysicalNegatives() {
        listOf("", ".", "no", "NaN", "Infinity", "-Infinity", "1e301", "1e-400", "1,2", "1".repeat(81)).forEach { rejects("Length", it) }
        UnitConverter.categories.filterNot { it.name in listOf("Temperature", "Angle") }.forEach { rejects(it.name, "-1") }
        close(-Math.PI, convert("Angle", "-180", 0, 1))
        close(0.0, convert("Length", "0", 0, 1))
    }

    @Test fun preservesSmallAndLargeResults() {
        assertTrue(convert("Length", "1e-20", 0, 1) > 0.0)
        assertTrue(convert("Area", "1e100", 0, 3).isFinite())
        assertEquals("0.000001", UnitConverter.format(0.000001))
        assertEquals("12.05", UnitConverter.format(12.05))
        assertEquals("0", UnitConverter.format(-0.0))
    }
}
