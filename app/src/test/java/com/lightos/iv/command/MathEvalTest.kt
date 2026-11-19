package com.lightos.iv.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEvalTest {
    @Test fun precedence() = assertEquals("14", MathEval.evaluate("2+3*4"))
    @Test fun parens() = assertEquals("20", MathEval.evaluate("(2+3)*4"))
    @Test fun decimals() = assertEquals("148", MathEval.evaluate("185 * 0.8"))
    @Test fun division() = assertEquals("0.333333333333", MathEval.evaluate("1/3"))
    @Test fun unicodeOps() = assertEquals("12", MathEval.evaluate("3×4"))
    @Test fun xAsTimes() = assertEquals("12", MathEval.evaluate("3x4"))
    @Test fun power() = assertEquals("1024", MathEval.evaluate("2^10"))
    @Test fun unaryMinus() = assertEquals("-6", MathEval.evaluate("-2*3"))
    @Test fun divideByZero() = assertNull(MathEval.evaluate("1/0"))
    @Test fun garbage() = assertNull(MathEval.evaluate("2+*"))
    @Test fun plainNumberIsNotMath() = assertFalse(MathEval.looksLikeMath("2024"))
    @Test fun wordsAreNotMath() = assertFalse(MathEval.looksLikeMath("maps"))
    @Test fun expressionIsMath() = assertTrue(MathEval.looksLikeMath("45 * 12"))
}
