package dev.komu.kraken.utils.exp

import dev.komu.kraken.utils.exp.TokenType.*
import java.util.*
import java.util.regex.Pattern

open class ExpressionParser(val expression: String) {

    private val lexer = ExpressionLexer(expression)

    fun parse() =
        parseExpression()

    /**
     * expr = factor
     *      | factor + expr
     *      | factor - expr
     */
    private fun parseExpression(): Expression {
        var exp = parseFactor()
        while (true) {
            val next = nextToken()
            exp = when (next) {
                PLUS  -> Expression.Binary(BinOp.ADD, exp, parseFactor())
                MINUS -> Expression.Binary(BinOp.SUB, exp, parseFactor())
                else -> {
                    lexer.pushBack()
                    return exp
                }
            }
        }
    }

    /**
     * factor = term
     *        | term * factor
     *        | term / factor
     *        | term % factor
     */
    private fun parseFactor(): Expression {
        var factor = parseTerm()
        while (true) {
            val next = nextToken()
            factor = when (next) {
                MUL -> Expression.Binary(BinOp.MUL, factor, parseTerm())
                DIV -> Expression.Binary(BinOp.DIV, factor, parseTerm())
                MOD -> Expression.Binary(BinOp.MOD, factor, parseTerm())
                else -> {
                    lexer.pushBack()
                    return factor
                }
            }
        }
    }

    /**
     * term = IDENT argument_list
     *      | IDENT
     *      | die_exp
     *      | ( expr )
     */
    private fun parseTerm(): Expression {
        val token = nextToken()
        when (token) {
            TokenType.IDENTIFIER -> {
                val name = lexer.currentValue as String
                return if (nextToken() == TokenType.LPAR) {
                    lexer.pushBack()
                    val args = parseArgumentList()
                    Expression.Apply(name, args)
                } else {
                    lexer.pushBack()
                    parseVariableOrDie(name)
                }
            }
            TokenType.LPAR -> {
                val term = parseExpression()
                assertToken(TokenType.RPAR)
                return term
            }
            else -> {
                lexer.pushBack()
                return Expression.Constant(getNumber())
            }
        }
    }

    /**
     * argumentList = ()
     *              | (nonEmptyExplist)
     */
    private fun parseArgumentList(): List<Expression> {
        assertToken(TokenType.LPAR)
        return if (nextToken() == TokenType.RPAR) {
            Collections.emptyList()
        } else {
            lexer.pushBack()
            val exps = parseNonEmptyExpList()
            assertToken(TokenType.RPAR)
            exps
        }
    }

    /**
     * nonEmptyExpList = exp
     *                 | exp, explist
     */
    private fun parseNonEmptyExpList(): List<Expression> {
        val result = mutableListOf<Expression>()
        while (true) {
            result.add(parseExpression())
            if (nextToken() != TokenType.COMMA) {
                lexer.pushBack()
                return result
            }
        }
    }

    private fun assertToken(token: TokenType) {
        if (nextToken() != token)
            throw ParseException("invalid expression <$expression>, expected: $token")
    }

    private fun getNumber(): Int {
        var sign = 1
        var tokenType = nextToken()
        if (tokenType == TokenType.PLUS || tokenType == TokenType.MINUS) {
            sign = if (tokenType == TokenType.PLUS) 1 else -1
            tokenType = nextToken()
        }

        if (tokenType == TokenType.NUMBER) {
            return sign * (lexer.currentValue as Int)
        } else {
            throw ParseException("expected number, got $tokenType")
        }
    }

    private fun nextToken(): TokenType =
        lexer.next()

    companion object {
        private val DIE_PATTERN = Pattern.compile("(\\d*)d(\\d+)")

        private fun parseVariableOrDie(token: String): Expression {
            val m = DIE_PATTERN.matcher(token)
            return if (m.matches()) {
                val dieCount = m.group(1)
                val multiplier = if (dieCount != null && dieCount != "") dieCount.toInt() else 1
                val sides = m.group(2)!!.toInt()
                Expression.Die(multiplier, sides)
            } else {
                Expression.Variable(token)
            }
        }
    }
}

class ParseException(message: String): RuntimeException(message)
