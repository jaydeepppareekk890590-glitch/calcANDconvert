package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object CalculatorEngine {
    private val precision = MathContext(16, RoundingMode.HALF_EVEN)

    fun calculate(value: String): BigDecimal {
        require(value.length in 1..256) { "Enter a shorter calculation" }
        return Parser(value.replace('×', '*').replace('÷', '/').replace('−', '-').filterNot { it.isWhitespace() }).parse()
    }

    fun format(value: BigDecimal): String {
        val number = value.round(precision).stripTrailingZeros()
        return if (number.signum() == 0) "0" else if (number.scale() in -12..16 && number.precision() <= 24) number.toPlainString() else number.toString()
    }

    private class Parser(private val text: String) {
        private var position = 0
        private var depth = 0

        fun parse(): BigDecimal {
            val result = expression()
            require(position == text.length) { "Check the calculation" }
            return checked(result)
        }

        private fun take(char: Char): Boolean {
            if (position < text.length && text[position] == char) {
                position++
                return true
            }
            return false
        }

        private fun expression(): BigDecimal {
            var result = term()
            while (true) {
                result = when {
                    take('+') -> checked(result.add(term(), precision))
                    take('-') -> checked(result.subtract(term(), precision))
                    else -> return result
                }
            }
        }

        private fun term(): BigDecimal {
            var result = unary()
            while (true) {
                result = when {
                    take('*') -> checked(result.multiply(unary(), precision))
                    take('/') -> {
                        val divisor = unary()
                        require(divisor.signum() != 0) { "Cannot divide by zero" }
                        checked(result.divide(divisor, precision))
                    }
                    else -> return result
                }
            }
        }

        private fun unary(): BigDecimal {
            require(++depth <= 32) { "Calculation is too deeply nested" }
            val result = when {
                take('+') -> unary()
                take('-') -> unary().negate()
                else -> power()
            }
            depth--
            return checked(result)
        }

        private fun power(): BigDecimal {
            var result = primary()
            while (take('%')) result = result.divide(BigDecimal(100), precision)
            if (take('^')) {
                val exponent = unary()
                val count = try { exponent.intValueExact() } catch (_: ArithmeticException) { throw IllegalArgumentException("Use a whole-number power") }
                require(count in -100..100) { "Power must be between -100 and 100" }
                if (count < 0) {
                    require(result.signum() != 0) { "Cannot divide by zero" }
                    result = BigDecimal.ONE.divide(result.pow(-count, precision), precision)
                } else {
                    require(result.signum() != 0 || count != 0) { "Zero to power zero is undefined" }
                    result = result.pow(count, precision)
                }
            }
            return checked(result)
        }

        private fun primary(): BigDecimal {
            if (take('(')) {
                val result = expression()
                require(take(')')) { "Close the parenthesis" }
                return result
            }
            val start = position
            var digits = 0
            while (position < text.length && text[position].isDigit()) { position++; digits++ }
            if (take('.')) while (position < text.length && text[position].isDigit()) { position++; digits++ }
            require(digits > 0) { "Enter a number" }
            if (take('E') || take('e')) {
                if (!take('+')) take('-')
                val exponentStart = position
                while (position < text.length && text[position].isDigit()) position++
                require(position > exponentStart) { "Complete the exponent" }
            }
            val value = try { BigDecimal(text.substring(start, position), precision) } catch (_: NumberFormatException) { throw IllegalArgumentException("Check the number") }
            return checked(value)
        }
    }

    private fun checked(value: BigDecimal): BigDecimal {
        require(value.scale() in -300..350 && value.precision() <= 350) { "Number is outside the supported range" }
        return value
    }
}
