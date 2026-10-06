import kotlin.math.abs

/**
 * Moteur de calcul : parseur récursif descendant, évaluation sûre, formatage propre.
 *
 * Grammaire (descente récursive) :
 *   expression := terme  (('+' | '-') terme)*
 *   terme      := facteur (('*' | '/' | '%') facteur)*
 *   facteur    := unaire   ('^' unaire)*          // associatif à droite
 *   unaire     := ('-' | '+')? puisissance
 *   puisissance:= primaire ('!' )*                // factorielle
 *   primaire   := NOMBRE | '(' expression ')'
 */
class CalculatorEngine {

    class CalcException(val funny: Boolean, message: String) : RuntimeException(message)

    private var pos = 0
    private var src = ""

    companion object {
        const val MAX_EXPR_LENGTH = 80
        const val MAX_FACTORIAL = 170          // 170! est le max représentable en Double
        const val MAX_NUMBER_LITERAL = 16      // nb de chiffres max saisis pour un nombre
    }

    /** Évalue l'expression. Lève [CalcException] sur entrée invalide. */
    fun evaluate(expression: String): Double {
        val cleaned = expression.replace(" ", "")
            .replace("\u00D7", "*").replace("\u00F7", "/").replace("\u2212", "-")
        if (cleaned.isEmpty()) throw CalcException(true, "Rien \u00E0 calculer. Tu veux que je devine ?")
        if (cleaned.length > MAX_EXPR_LENGTH)
            throw CalcException(true, "Expression trop longue, m\u00EAme ma m\u00E9moire a des limites !")
        if (cleaned.count { it == '(' } != cleaned.count { it == ')' })
            throw CalcException(true, "Les parenth\u00E8ses vont par deux, comme les chaussettes.")
        if (cleaned.any { it !in "0123456789.+-*/%^()!" })
            throw CalcException(false, "Caract\u00E8re invalide dans l'expression.")

        src = cleaned
        pos = 0
        val result = parseExpression()
        skipSpaces()
        if (pos < src.length) throw CalcException(true, "Je suis perdu au caract\u00E8re ${pos + 1}.")
        if (result.isNaN()) throw CalcException(true, "R\u00E9sultat ind\u00E9fini. M\u00EAme les maths baissent les bras.")
        return result
    }

    private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'
    private fun eat(c: Char): Boolean {
        if (peek() == c) { pos++; return true }
        return false
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            when {
                eat('+') -> {
                    val rhs = parseTerm()
                    value = checkedAdd(value, rhs)
                }
                eat('-') -> {
                    val rhs = parseTerm()
                    value = checkedAdd(value, -rhs)
                }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            when {
                eat('*') -> {
                    val rhs = parseFactor()
                    value = checkedMul(value, rhs)
                }
                eat('/') -> {
                    val rhs = parseFactor()
                    if (rhs == 0.0) throw CalcException(true, "Division par z\u00E9ro ! Tu viens de casser l'univers.")
                    value = checkedMul(value, 1.0 / rhs)
                }
                eat('%') -> {
                    val rhs = parseFactor()
                    if (rhs == 0.0) throw CalcException(true, "Modulo par z\u00E9ro : le reste de rien, c'est... rien.")
                    value = value % rhs
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        var base = parseUnary()
        if (eat('^')) {
            val exponent = parseFactor()      // associatif à droite : 2^3^2 = 2^(3^2)
            if (base == 0.0 && exponent < 0)
                throw CalcException(true, "0 puissance n\u00E9gatif ? M\u00EAme l'infini dit non.")
            val powered = checkedPow(base, exponent)
            return powered
        }
        return base
    }

    private fun parseUnary(): Double {
        if (eat('-')) return checkedNeg(parseUnary())
        if (eat('+')) return parseUnary()
        return parsePostfix()
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (eat('!')) {
            if (value < 0 || value != value.toLong().toDouble())
                throw CalcException(true, "Factorielle d'un nombre n\u00E9gatif ou d\u00E9cimal : interdit !")
            if (value > MAX_FACTORIAL)
                throw CalcException(true, "$value! ? La calculatrice surchauffe, elle refuse !")
            var acc = 1.0
            for (i in 2..value.toLong()) acc = checkedMul(acc, i.toDouble())
            value = acc
        }
        return value
    }

    private fun parsePrimary(): Double {
        if (eat('(')) {
            val value = parseExpression()
            if (!eat(')')) throw CalcException(true, "Il manque une parenth\u00E8se fermante quelque part...")
            return value
        }
        if (peek().isDigit() || peek() == '.') return parseNumber()
        throw CalcException(
            true,
            if (pos >= src.length) "Expression incompl\u00E8te, il manque la fin !"
            else "Je ne comprends pas ce que '${
                peek()
            }' fait l\u00E0."
        )
    }

    private fun parseNumber(): Double {
        val start = pos
        var dotSeen = false
        var digits = 0
        while (pos < src.length) {
            val c = src[pos]
            if (c.isDigit()) { digits++; pos++ }
            else if (c == '.') {
                if (dotSeen) throw CalcException(true, "Un seul point par nombre, on n'est pas \u00E0 la plage.")
                dotSeen = true; pos++
            } else break
        }
        if (digits > MAX_NUMBER_LITERAL)
            throw CalcException(true, "Ce nombre est trop grand pour moi, il me faut des lunettes.")
        val token = src.substring(start, pos)
        if (token == ".") throw CalcException(true, "Un point tout seul, c'est juste une boule.")
        val value = token.toDoubleOrNull() ?: throw CalcException(false, "Nombre invalide.")
        return value
    }

    private fun skipSpaces() { /* les espaces ont déjà été retirés, gardé pour symétrie */ }

    // ---------- Gardrails arithmétiques (overflow -> message fun plutôt que Infinity) ----------

    private fun checkedAdd(a: Double, b: Double): Double {
        val r = a + b
        ensureFinite(r) { "$a + $b ? Trop grand, le r\u00E9sultat explose !" }
        return r
    }

    private fun checkedNeg(a: Double): Double {
        val r = -a
        if (r.isInfinite()) throw CalcException(true, "N\u00E9gation g\u00E9ante : l'univers a un plafond.")
        return r
    }

    private fun checkedMul(a: Double, b: Double): Double {
        val r = a * b
        ensureFinite(r) { "$a \u00D7 $b ? Kaboom, d\u00E9bordement !" }
        return r
    }

    private fun checkedPow(a: Double, b: Double): Double {
        if (a == 0.0 && b == 0.0) throw CalcException(true, "0^0 : le d\u00E9bat continue, la calculatrice refuse.")
        if (b > 10000.0 || b < -10000.0)
            throw CalcException(true, "Exposant trop grand, la calculatrice prend feu !")
        val r = Math.pow(a, b)
        ensureFinite(r) { "$a^$b ? Trop grand, \u00E7a d\u00E9borde du cadre !" }
        return r
    }

    private inline fun ensureFinite(r: Double, lazyMsg: () -> String) {
        if (r.isInfinite()) throw CalcException(true, lazyMsg())
    }

    // ---------- Formatage ----------

    /** Format lisible : entiers sans décimales, sinon 12 chiffres significatifs max. */
    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Erreur"
        val absV = abs(value)
        if (value == Math.floor(value) && absV < 1e15) {
            val l = value.toLong()
            return if (Math.abs(l) > 9999) String.format("%,d", l).replace(',', ' ')
            else l.toString()
        }
        // trop grand ou trop petit pour \u00EAtre joli en notation d\u00E9cimale : notation scientifique
        if (absV >= 1e15 || (absV > 0 && absV < 1e-7)) {
            var s = String.format("%.6e", value)
            val eIdx = s.indexOf('e')
            var mantissa = s.substring(0, eIdx)
            if (mantissa.contains('.')) mantissa = mantissa.trimEnd('0').trimEnd('.')
            return "$mantissa${s[eIdx].uppercaseChar()}${s.substring(eIdx + 1)}"
        }
        val rounded = java.math.BigDecimal(value)
            .round(java.math.MathContext(12))
            .stripTrailingZeros()
        var s = rounded.toPlainString()
        if (s == "-0") s = "0"
        return s
    }
}
