package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

object CalculatorEngine {

  private val mathContext = MathContext(15, RoundingMode.HALF_UP)

  sealed interface EvalResult {
    data class Success(val formatted: String, val value: Double) : EvalResult
    data class Error(val message: String) : EvalResult
  }

  fun evaluate(rawExpression: String): EvalResult {
    val sanitized = rawExpression
      .replace("×", "*")
      .replace("÷", "/")
      .replace("−", "-")
      .replace(" ", "")
    if (sanitized.isBlank()) return EvalResult.Success("0", 0.0)

    return try {
      val tokens = tokenize(sanitized)
      if (tokens.isEmpty()) return EvalResult.Success("0", 0.0)
      val parser = Parser(tokens)
      val value = parser.parseExpression()
      if (parser.hasMore()) {
        return EvalResult.Error("Syntax error")
      }
      if (value.isNaN() || value.isInfinite()) {
        return EvalResult.Error("Cannot divide by zero")
      }
      EvalResult.Success(formatNumber(value), value)
    } catch (e: ArithmeticException) {
      EvalResult.Error(e.message ?: "Math error")
    } catch (_: Exception) {
      EvalResult.Error("Invalid expression")
    }
  }

  fun tryPreview(rawExpression: String): String? {
    if (rawExpression.isBlank()) return null
    val trimmed = rawExpression.trimEnd()
    if (trimmed.isEmpty()) return null
    val last = trimmed.last()
    if (last in charArrayOf('+', '-', '−', '*', '×', '/', '÷', '(')) return null
    return when (val res = evaluate(trimmed)) {
      is EvalResult.Success -> res.formatted
      is EvalResult.Error -> null
    }
  }

  fun toggleLastNumberSign(expression: String): String {
    if (expression.isBlank()) return "-"
    val idx = expression.indexOfLast { it in charArrayOf('+', '−', '-', '×', '*', '÷', '/', '(') }
    if (idx < 0) {
      return if (expression.startsWith("-")) expression.drop(1) else "-$expression"
    }
    val ch = expression[idx]
    val tail = expression.substring(idx + 1)
    if (tail.isEmpty()) {
      return if (ch == '-' || ch == '−') expression.dropLast(1) else "$expression(-"
    }
    return if (ch == '-' && (idx == 0 || expression[idx - 1] in charArrayOf('(', '+', '−', '×', '÷', '*', '/'))) {
      if (idx > 0 && expression[idx - 1] == '(') {
        expression.substring(0, idx - 1) + tail
      } else {
        expression.substring(0, idx) + tail
      }
    } else {
      expression.substring(0, idx + 1) + "(-" + tail
    }
  }

  fun formatNumber(value: Double): String {
    if (value == 0.0 || abs(value) < 1e-13) return "0"
    if (abs(value) >= 1e12 || (abs(value) > 0 && abs(value) < 1e-7)) {
      return String.format(java.util.Locale.US, "%.6e", value)
        .replace(Regex("\\.?0+e"), "e")
    }
    val bd = BigDecimal.valueOf(value).round(mathContext).stripTrailingZeros()
    return bd.toPlainString()
  }

  private sealed interface Token {
    data class Number(val value: Double) : Token
    data class Op(val symbol: Char) : Token
    data object LParen : Token
    data object RParen : Token
    data object Percent : Token
  }

  private fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < input.length) {
      val ch = input[i]
      when {
        ch.isDigit() || ch == '.' -> {
          val start = i
          var dotSeen = ch == '.'
          i++
          while (i < input.length) {
            val next = input[i]
            if (next.isDigit()) {
              i++
            } else if (next == '.' && !dotSeen) {
              dotSeen = true
              i++
            } else {
              break
            }
          }
          val numStr = input.substring(start, i)
          val parsed = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Bad number")
          if (tokens.lastOrNull() is Token.RParen || tokens.lastOrNull() is Token.Percent) {
            tokens.add(Token.Op('*'))
          }
          tokens.add(Token.Number(parsed))
        }
        ch == '(' -> {
          val prev = tokens.lastOrNull()
          if (prev is Token.Number || prev is Token.RParen || prev is Token.Percent) {
            tokens.add(Token.Op('*'))
          }
          tokens.add(Token.LParen)
          i++
        }
        ch == ')' -> {
          tokens.add(Token.RParen)
          i++
        }
        ch == '%' -> {
          tokens.add(Token.Percent)
          i++
        }
        ch in charArrayOf('+', '-', '*', '/') -> {
          tokens.add(Token.Op(ch))
          i++
        }
        else -> throw IllegalArgumentException("Unknown char: $ch")
      }
    }
    return tokens
  }

  private class Parser(private val tokens: List<Token>) {
    private var pos = 0

    fun hasMore(): Boolean = pos < tokens.size

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun consume(): Token = tokens[pos++]

    fun parseExpression(): Double {
      var left = parseTerm()
      while (true) {
        val t = peek()
        if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
          consume()
          val right = parseTerm()
          left = if (t.symbol == '+') left + right else left - right
        } else {
          break
        }
      }
      return left
    }

    private fun parseTerm(): Double {
      var left = parseFactor()
      while (true) {
        val t = peek()
        if (t is Token.Op && (t.symbol == '*' || t.symbol == '/')) {
          consume()
          val right = parseFactor()
          if (t.symbol == '*') {
            left *= right
          } else {
            if (right == 0.0) throw ArithmeticException("Cannot divide by zero")
            left /= right
          }
        } else {
          break
        }
      }
      return left
    }

    private fun parseFactor(): Double {
      var value = parsePrimary()
      while (peek() is Token.Percent) {
        consume()
        value /= 100.0
      }
      return value
    }

    private fun parsePrimary(): Double {
      return when (val t = peek()) {
        is Token.Op -> {
          when (t.symbol) {
            '+' -> {
              consume()
              parseFactor()
            }
            '-' -> {
              consume()
              -parseFactor()
            }
            else -> throw IllegalArgumentException("Unexpected operator")
          }
        }
        is Token.Number -> {
          consume()
          t.value
        }
        Token.LParen -> {
          consume()
          val inner = parseExpression()
          if (peek() == Token.RParen) {
            consume()
          }
          inner
        }
        else -> throw IllegalArgumentException("Unexpected token")
      }
    }
  }
}
