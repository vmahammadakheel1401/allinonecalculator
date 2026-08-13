package com.example.utilities

import kotlin.math.*

class ExpressionEvaluator {
    fun evaluate(expression: String): Double {
        val expr = expression.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("Mod", "%")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
            
        val tokens = tokenize(expr)
        return parseExpression(tokens)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                var num = ""
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    num += expr[i]
                    i++
                }
                tokens.add(num)
            } else if (c.isLetter() || c == '√') {
                var func = ""
                while (i < expr.length && (expr[i].isLetter() || expr[i] == '√')) {
                    func += expr[i]
                    i++
                }
                tokens.add(func)
            } else {
                tokens.add(c.toString())
                i++
            }
        }
        return tokens
    }

    private var pos = 0
    private lateinit var tokens: List<String>

    private fun parseExpression(t: List<String>): Double {
        this.tokens = t
        this.pos = 0
        return parseAddSub()
    }

    private fun parseAddSub(): Double {
        var result = parseMulDiv()
        while (pos < tokens.size) {
            val op = tokens[pos]
            if (op != "+" && op != "-") break
            pos++
            val nextTerm = parseMulDiv()
            if (op == "+") result += nextTerm else result -= nextTerm
        }
        return result
    }

    private fun parseMulDiv(): Double {
        var result = parsePower()
        while (pos < tokens.size) {
            val op = tokens[pos]
            if (op != "*" && op != "/" && op != "%") break
            pos++
            val nextFactor = parsePower()
            if (op == "*") result *= nextFactor
            else if (op == "/") {
                if (nextFactor == 0.0) throw ArithmeticException("Division by zero")
                result /= nextFactor
            }
            else if (op == "%") result %= nextFactor
        }
        return result
    }

    private fun parsePower(): Double {
        var result = parseFactor()
        while (pos < tokens.size && tokens[pos] == "^") {
            pos++
            val exponent = parseFactor()
            result = result.pow(exponent)
        }
        return result
    }

    private fun parseFactor(): Double {
        if (pos >= tokens.size) throw IllegalArgumentException("Unexpected end of expression")
        val token = tokens[pos]
        
        if (token == "(") {
            pos++
            val result = parseAddSub()
            if (pos < tokens.size && tokens[pos] == ")") pos++
            return factorialFallback(result)
        } else if (token == "-") {
            pos++
            return -parseFactor()
        } else if (token == "+") {
            pos++
            return parseFactor()
        } else if (token == "sin" || token == "cos" || token == "tan" || token == "log" || token == "ln" || token == "√") {
            pos++
            // Require a parenthesis or just take the next factor
            val arg = parseFactor()
            val res = when (token) {
                "sin" -> sin(Math.toRadians(arg)) // assume degrees for standard use
                "cos" -> cos(Math.toRadians(arg))
                "tan" -> tan(Math.toRadians(arg))
                "log" -> log10(arg)
                "ln" -> ln(arg)
                "√" -> sqrt(arg)
                else -> arg
            }
            return factorialFallback(res)
        }
        
        pos++
        val num = token.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $token")
        return factorialFallback(num)
    }
    
    private fun factorialFallback(value: Double): Double {
        if (pos < tokens.size && tokens[pos] == "!") {
            pos++
            // very basic factorial for integers
            val n = value.toLong()
            if (n < 0) throw IllegalArgumentException("Factorial of negative")
            var f = 1L
            for (i in 2..n) f *= i
            return f.toDouble()
        }
        return value
    }
}
