package com.lightos.iv.command

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * A small, safe arithmetic evaluator (recursive descent, no `eval`).
 * Supports + − × ÷ * / % ^, parentheses, unary minus, decimals, and "x" as multiply.
 * `looksLikeMath` keeps plain numbers and app names out of the calculator.
 */
object MathEval {

    fun looksLikeMath(input: String): Boolean {
        val s = input.trim()
        if (s.isEmpty() || s.length > 120) return false
        if (!s.all { it.isDigit() || it in ALLOWED }) return false
        if (s.none { it.isDigit() }) return false
        // Needs at least one binary operator between operands.
        return Regex("[0-9.)]\\s*[-+*/x×÷%^]\\s*[-(0-9.]").containsMatchIn(s)
    }

    /** Returns the formatted result, or null if the expression is invalid or divides by zero. */
    fun evaluate(input: String): String? = try {
        val p = Parser(input.replace('×', '*').replace('÷', '/').replace('x', '*').replace(" ", ""))
        val v = p.expression()
        if (!p.atEnd()) null else format(v)
    } catch (_: ArithmeticException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun format(v: BigDecimal): String {
        val rounded = v.round(MathContext(12, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val plain = rounded.toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private const val ALLOWED = "+-*/x×÷%^(). "

    private class Parser(private val s: String) {
        private var i = 0
        fun atEnd() = i >= s.length

        fun expression(): BigDecimal {
            var v = term()
            while (!atEnd()) {
                v = when (s[i]) {
                    '+' -> { i++; v + term() }
                    '-' -> { i++; v - term() }
                    else -> return v
                }
            }
            return v
        }

        private fun term(): BigDecimal {
            var v = power()
            while (!atEnd()) {
                v = when (s[i]) {
                    '*' -> { i++; v * power() }
                    '/' -> {
                        i++
                        val d = power()
                        if (d.signum() == 0) throw ArithmeticException("divide by zero")
                        v.divide(d, MathContext.DECIMAL64)
                    }
                    '%' -> {
                        i++
                        val d = power()
                        if (d.signum() == 0) throw ArithmeticException("mod by zero")
                        v.remainder(d)
                    }
                    else -> return v
                }
            }
            return v
        }

        private fun power(): BigDecimal {
            val base = unary()
            if (!atEnd() && s[i] == '^') {
                i++
                val exp = power()
                val n = exp.toInt()
                require(exp.compareTo(BigDecimal(n)) == 0 && n in -64..64) { "unsupported exponent" }
                return if (n >= 0) base.pow(n) else BigDecimal.ONE.divide(base.pow(-n), MathContext.DECIMAL64)
            }
            return base
        }

        private fun unary(): BigDecimal {
            if (!atEnd() && s[i] == '-') { i++; return unary().negate() }
            if (!atEnd() && s[i] == '+') { i++; return unary() }
            return primary()
        }

        private fun primary(): BigDecimal {
            require(!atEnd()) { "unexpected end" }
            if (s[i] == '(') {
                i++
                val v = expression()
                require(!atEnd() && s[i] == ')') { "missing )" }
                i++
                return v
            }
            val start = i
            while (!atEnd() && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start) { "expected number" }
            return BigDecimal(s.substring(start, i))
        }
    }
}
