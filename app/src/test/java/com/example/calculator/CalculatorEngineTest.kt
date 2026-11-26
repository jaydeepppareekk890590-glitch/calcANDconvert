package com.example.calculator

import org.junit.Assert.*
import org.junit.Test

class CalculatorEngineTest {
    private fun result(value: String) = CalculatorEngine.format(CalculatorEngine.calculate(value))
    private fun rejects(value: String) {
        try { result(value); fail("Should reject $value") } catch (_: IllegalArgumentException) { }
    }

    @Test fun precedenceAndParentheses() {
        assertEquals("14", result("2+3*4"))
        assertEquals("20", result("(2+3)*4"))
        assertEquals("1", result("10-3*3"))
        assertEquals("4", result("12/3"))
        assertEquals("2", result("8/2/2"))
        assertEquals("512", result("2^3^2"))
    }

    @Test fun signedNumbers() {
        assertEquals("-7", result("-3-4"))
        assertEquals("-6", result("2*-3"))
        assertEquals("-4", result("-2^2"))
        assertEquals("4", result("(-2)^2"))
        assertEquals("-3", result("-(1+2)"))
        assertEquals("5", result("2--3"))
    }

    @Test fun decimalPrecisionAndFormatting() {
        assertEquals("0.3", result("0.1+0.2"))
        assertEquals("12.05", result("12.05+0"))
        assertEquals("0.01", result("0.01*1"))
        assertEquals("-0.05", result("-0.1/2"))
        assertEquals("1", result("1.000"))
        assertEquals("0", result("-0.0"))
        assertEquals("0.3333333333333333", result("1/3"))
        assertEquals("1001", result("1E3+1"))
        assertEquals("0.75", result(".5+.25"))
    }

    @Test fun percentAndPowers() {
        assertEquals("20", result("200*10%"))
        assertEquals("200.1", result("200+10%"))
        assertEquals("220", result("200*(1+10%)"))
        assertEquals("0.125", result("2^-3"))
        assertEquals("1", result("5^0"))
        assertEquals("2", result("8×25%"))
        assertEquals("3", result("9÷3"))
    }

    @Test fun rejectsMalformedExpressions() {
        listOf("", ".", "1.2.3", "3+", "()", "(2+3", "2+3)", "2(3)", "2**3", "hello", "1E", "--", "1,5", "2^0.5", "2^101", "0^0").forEach(::rejects)
    }

    @Test fun rejectsZeroDivisionAndBounds() {
        listOf("1/0", "0/0", "0^-1", "2/(3-3)", "1E301", "1E-351", "(".repeat(40)+"1"+")".repeat(40), "1".repeat(257)).forEach(::rejects)
    }

    @Test fun arithmeticSamples() {
        for (a in -12..12) for (b in -12..12) {
            assertEquals((a+b).toString(), result("($a)+($b)"))
            assertEquals((a-b).toString(), result("($a)-($b)"))
            assertEquals((a*b).toString(), result("($a)*($b)"))
        }
    }
}
