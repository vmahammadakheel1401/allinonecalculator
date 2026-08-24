package com.example.utilities

import kotlin.math.*

class ExpressionEvaluator {
    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0
        val expr = expression.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("Mod", "#") // temporary marker for modulo
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
            
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return 0.0
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
        val result = parseAddSub()
        return result
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
            if (op != "*" && op != "/" && op != "#") break
            pos++
            val nextFactor = parsePower()
            if (op == "*") result *= nextFactor
            else if (op == "/") {
                if (nextFactor == 0.0) throw ArithmeticException("Division by zero")
                result /= nextFactor
            }
            else if (op == "#") result %= nextFactor
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
        if (pos >= tokens.size) return 0.0
        val token = tokens[pos]
        
        if (token == "(") {
            pos++
            val result = parseAddSub()
            if (pos < tokens.size && tokens[pos] == ")") pos++
            return factorSuffix(result)
        } else if (token == "-") {
            pos++
            return -parseFactor()
        } else if (token == "+") {
            pos++
            return parseFactor()
        } else if (token == "sin" || token == "cos" || token == "tan" || token == "log" || token == "ln" || token == "√") {
            pos++
            val arg = parseFactor()
            val res = when (token) {
                "sin" -> sin(Math.toRadians(arg))
                "cos" -> cos(Math.toRadians(arg))
                "tan" -> tan(Math.toRadians(arg))
                "log" -> if (arg > 0) log10(arg) else throw IllegalArgumentException("Log of non-positive")
                "ln" -> if (arg > 0) ln(arg) else throw IllegalArgumentException("Ln of non-positive")
                "√" -> if (arg >= 0) sqrt(arg) else throw IllegalArgumentException("Square root of negative")
                else -> arg
            }
            return factorSuffix(res)
        }
        
        pos++
        val num = token.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $token")
        return factorSuffix(num)
    }
    
    private fun factorSuffix(value: Double): Double {
        var res = value
        while (pos < tokens.size && (tokens[pos] == "!" || tokens[pos] == "%")) {
            val op = tokens[pos]
            pos++
            if (op == "!") {
                val n = res.toLong()
                if (n < 0) throw IllegalArgumentException("Factorial of negative")
                if (n > 20) throw IllegalArgumentException("Factorial overflow")
                var f = 1.0
                for (i in 2..n) f *= i
                res = f
            } else if (op == "%") {
                res /= 100.0
            }
        }
        return res
    }
}
