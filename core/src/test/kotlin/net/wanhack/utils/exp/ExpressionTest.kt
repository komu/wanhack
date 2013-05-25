/*
 * Copyright 2013 The Wanhack Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wanhack.utils.exp

import org.junit.Test as test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ExpressionTest {

    test fun simple() {
        assertExpression(0, "0")
        assertExpression(1, "1")
        assertExpression(5, "5")
        assertExpression(-5, "-5")
        assertExpression(5, "+5")
    }

    test fun additiveExpression()  {
        assertExpression(4, "2 + 2")
        assertExpression(3, "5 - 2")
        assertExpression(6, "1 + 2 + 3")
        assertExpression(2, "5 - 2 - 1")
    }

    test fun multiplicativeExpression()  {
        assertExpression(6, "2 * 3")
        assertExpression(11, "1 + 2 * 3 + 4")
    }

    test fun parenthesizedExpression() {
        assertExpression(21, "(1 + 2) * (3 + 4)")
    }

    test fun functionCalls() {
        assertExpression(2, "max(1, 2)")
    }

    test fun variables() {
        assertExpression(3, "one + two", mapOf("one" to 1, "two" to 2))
    }

    test fun diceSyntax() {
        assertExpression(2, "d1 + d1")
        assertExpression(4, "3d1 + 1")

        val result = Expression.evaluate("d4")
        assertTrue(result >= 1 && result <= 4)
    }

    class object {

        fun assertExpression(expected: Int, exp: String) {
            assertEquals(expected, Expression.evaluate(exp))
        }

        fun assertExpression(expected: Int, exp: String, env: Map<String, Int>) {
            assertEquals(expected, Expression.evaluate(exp, env))
        }
    }
}