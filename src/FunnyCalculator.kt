import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import kotlin.math.abs
import kotlin.random.Random

/** Fond sombre arrondi pour l'affichage. */
private class Card(private val bgColor: Color) : JPanel() {
    init { isOpaque = false }
    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = bgColor
        g2.fillRoundRect(0, 0, width - 1, height - 1, 22, 22)
        super.paintComponent(g)
    }
}

/** Bouton arrondi avec d\u00E9grad\u00E9 et pulse au clic. */
private class FunButton(text: String, private val base: Color) : JButton(text) {
    private var glow = 0f
    private val animTimer = Timer(16, null)

    init {
        setContentAreaFilled(false)
        isBorderPainted = false
        isFocusPainted = false
        isFocusable = false
        isOpaque = false
        foreground = Color.WHITE
        font = Font(Font.SANS_SERIF, Font.BOLD, 22)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        animTimer.addActionListener {
            glow -= 0.09f
            if (glow <= 0f) { glow = 0f; animTimer.stop() }
            repaint()
        }
    }

    fun pulse() {
        glow = 1f
        animTimer.start()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val m = (3 + glow * 6f).toInt()
        val x = m
        val y = m
        val w = width - 2 * m
        val h = height - 2 * m
        val top = blend(base.brighter(), Color.WHITE, glow * 0.45f)
        val gp = GradientPaint(0f, y.toFloat(), top, 0f, (y + h).toFloat(), base.darker())
        g2.paint = gp
        g2.fillRoundRect(x, y, w, h, 20, 20)
        if (glow > 0f) {
            g2.color = Color(255, 255, 255, (90 * glow).toInt())
            g2.drawRoundRect(x, y, w, h, 20, 20)
        }
        super.paintComponent(g)
    }

    private fun blend(a: Color, b: Color, t: Float): Color {
        val f = t.coerceIn(0f, 1f)
        return Color(
            (a.red + (b.red - a.red) * f).toInt(),
            (a.green + (b.green - a.green) * f).toInt(),
            (a.blue + (b.blue - a.blue) * f).toInt()
        )
    }
}

/**
 * La calculatrice fun : moteur s\u00E9rieux, habillage d\u00E9lirant.
 * Explosions de particules au clic, secousses de fen\u00EAtre, confettis sur les r\u00E9sultats l\u00E9gendaires,
 * commentaires moqueurs, et un clavier qui marche.
 */
class FunnyCalculator {

    private val engine = CalculatorEngine()
    private val frame = JFrame(Quips.randomTitle())
    private val particles = ParticleField()

    private val exprLabel = JLabel(" ")
    private val resultLabel = JLabel("0")
    private val statusLabel = JLabel("Pr\u00EAte \u00E0 tout casser.")
    private val historyLabel = JLabel("")

    private val expr = StringBuilder()
    private var justEvaluated = false
    private val operators = "*/-+^%"

    private val shakeTimer = Timer(30, null)
    private var shakeTicks = 0
    private var shakeAmp = 0.0
    private var baseX = 0
    private var baseY = 0

    companion object {
        private val COL_BG = Color(0x1B, 0x1E, 0x27)
        private val COL_CARD = Color(0x24, 0x28, 0x36)
        private val COL_NUM = Color(0x2E, 0x33, 0x40)
        private val COL_OP = Color(0xE8, 0x7A, 0x2E)
        private val COL_EQ = Color(0x2E, 0xCC, 0x71)
        private val COL_CLEAR = Color(0xE7, 0x4C, 0x3C)
        private val COL_EXTRA = Color(0x44, 0x52, 0x66)
        private val COL_TXT_DIM = Color(0x8E, 0x99, 0xAD)
        private val COL_TXT = Color(0xF2, 0xF4, 0xF8)
        private val COL_ERR = Color(0xFF, 0x6B, 0x6B)
    }

    fun show() {
        SwingUtilities.invokeLater {
            buildUi()
            frame.isVisible = true
        }
    }

    // ---------------------------------------------------------------- UI

    private fun buildUi() {
        frame.isResizable = false
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.contentPane.background = COL_BG

        val root = JPanel()
        root.background = COL_BG
        root.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        root.layout = BorderLayout(0, 10)

        // --- Affichage ---
        val display = Card(COL_CARD)
        display.layout = BorderLayout(0, 2)
        display.border = BorderFactory.createEmptyBorder(10, 16, 10, 16)

        historyLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        historyLabel.foreground = COL_TXT_DIM
        historyLabel.horizontalAlignment = SwingConstants.RIGHT
        exprLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 16)
        exprLabel.foreground = COL_TXT_DIM
        exprLabel.horizontalAlignment = SwingConstants.RIGHT
        resultLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 44)
        resultLabel.foreground = COL_TXT
        resultLabel.horizontalAlignment = SwingConstants.RIGHT

        val topLine = JPanel()
        topLine.isOpaque = false
        topLine.layout = BoxLayout(topLine, BoxLayout.Y_AXIS)
        topLine.add(historyLabel)
        topLine.add(exprLabel)
        display.add(topLine, BorderLayout.NORTH)
        display.add(resultLabel, BorderLayout.CENTER)
        display.preferredSize = Dimension(340, 130)

        // --- Grille de boutons ---
        val grid = JPanel(GridBagLayout())
        grid.isOpaque = false
        val gc = GridBagConstraints()
        gc.fill = GridBagConstraints.BOTH
        gc.weightx = 1.0
        gc.weighty = 1.0
        gc.insets = Insets(4, 4, 4, 4)

        val defs = listOf(
            listOf("C" to COL_CLEAR, "\u232B" to COL_EXTRA, "(" to COL_EXTRA, ")" to COL_EXTRA),
            listOf("7" to COL_NUM, "8" to COL_NUM, "9" to COL_NUM, "\u00F7" to COL_OP),
            listOf("4" to COL_NUM, "5" to COL_NUM, "6" to COL_NUM, "\u00D7" to COL_OP),
            listOf("1" to COL_NUM, "2" to COL_NUM, "3" to COL_NUM, "\u2212" to COL_OP),
            listOf("0" to COL_NUM, "." to COL_NUM, "!" to COL_OP, "+" to COL_OP)
        )

        var row = 0
        for (line in defs) {
            var col = 0
            for ((label, color) in line) {
                gc.gridx = col; gc.gridy = row
                grid.add(mkButton(label, color), gc)
                col++
            }
            row++
        }
        gc.gridx = 0; gc.gridy = 5
        grid.add(mkButton("^", COL_EXTRA), gc)
        gc.gridx = 1; gc.gridy = 5
        gc.gridwidth = 3
        val eq = mkButton("=", COL_EQ)
        eq.font = Font(Font.SANS_SERIF, Font.BOLD, 30)
        grid.add(eq, gc)

        // --- Barre de statut ---
        statusLabel.font = Font(Font.SANS_SERIF, Font.ITALIC + Font.BOLD, 13)
        statusLabel.foreground = COL_TXT_DIM

        root.add(display, BorderLayout.NORTH)
        root.add(grid, BorderLayout.CENTER)
        root.add(statusLabel, BorderLayout.SOUTH)
        frame.contentPane.add(root)

        frame.glassPane = particles
        particles.isVisible = true

        frame.pack()
        frame.setLocationRelativeTo(null)

        // Titre qui change tout seul
        val titleTimer = Quips.randomTitleTimer(5000) { t -> frame.title = t }
        titleTimer.start()

        // Secousse de fen\u00EAtre
        shakeTimer.addActionListener { shakeStep() }

        bindKeys()
    }

    private fun mkButton(label: String, color: Color): JButton {
        val b = FunButton(label, color)
        b.addActionListener { e ->
            b.pulse()
            val src = e.source as JComponent
            val pt = Point(src.width / 2, src.height / 2)
            SwingUtilities.convertPointToScreen(pt, src)
            SwingUtilities.convertPointFromScreen(pt, particles)
            particles.burst(pt.x, pt.y, count = 14, speed = 5.0, size = 8)
            onButton(label)
        }
        return b
    }

    private fun bindKeys() {
        val im = frame.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = frame.rootPane.actionMap
        fun bind(stroke: KeyStroke, name: String, action: (ActionEvent) -> Unit) {
            im.put(stroke, name)
            am.put(name, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) = action(e)
            })
        }
        for (c in "0123456789.()+-*/^%!") bind(KeyStroke.getKeyStroke(c), "k$c") { onChar(c) }
        bind(KeyStroke.getKeyStroke('='), "kEq") { onChar('=') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "kEnter") { onChar('=') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "kBack") { onChar('\u232B') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "kEsc") { onChar('C') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0), "kMul") { onChar('\u00D7') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), "kDiv") { onChar('\u00F7') }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "kMinus") { onChar('\u2212') }
    }

    // ---------------------------------------------------------------- logique de saisie

    private fun onButton(label: String) = onChar(label.first())

    private fun onChar(c: Char) {
        when {
            c == 'C' -> doClear()
            c == '\u232B' -> doBackspace()
            c == '=' -> doEquals()
            c in "0123456789" -> inputDigit(c)
            c == '.' -> inputDot()
            c in "\u00F7\u00D7\u2212+^%" -> inputOperator(c)
            c == '(' -> inputOpenParen()
            c == ')' -> inputCloseParen()
            c == '!' -> inputFactorial()
        }
        refreshDisplay()
    }

    private fun startFreshIfNeeded(isDigitOrParen: Boolean) {
        if (!justEvaluated) return
        justEvaluated = false
        if (isDigitOrParen) {
            expr.clear()
            historyLabel.text = ""
        } else {
            expr.clear()
            expr.append(resultLabel.text.replace(" ", ""))
            historyLabel.text = ""
        }
    }

    private fun inputDigit(d: Char) {
        startFreshIfNeeded(isDigitOrParen = true)
        if (expr.length >= CalculatorEngine.MAX_EXPR_LENGTH) { refuse("Stop ! L'expression est pleine !"); return }
        if (expr.isNotEmpty() && (expr.last() == ')' || expr.last() == '!')) {
            expr.append('\u00D7') // multiplication implicite : 3(4) -> 3x(4)
        }
        expr.append(d)
    }

    /** Extrait le nombre en cours de saisie (segment depuis le dernier op\u00E9rateur/parenth\u00E8se). */
    private fun currentNumber(): String {
        val last = expr.indexOfLast { it in operators || it == '(' || it == ')' }
        return if (last == -1) expr.toString() else expr.substring(last + 1)
    }

    private fun inputDot() {
        startFreshIfNeeded(isDigitOrParen = false)
        val current = currentNumber()
        if (current.contains('.')) { refuse("Un seul point par nombre, on n'est pas \u00E0 la plage."); return }
        if (current.isEmpty()) { expr.append('0') } // ".5" devient "0.5"
        expr.append('.')
    }

    private fun inputOperator(op: Char) {
        startFreshIfNeeded(isDigitOrParen = false)
        val engineChar = when (op) {
            '\u00F7' -> '/'
            '\u00D7' -> '*'
            '\u2212' -> '-'
            else -> op
        }
        if (expr.isEmpty()) {
            if (engineChar == '-') expr.append('-') // "-5" acceptable
            else refuse("On ne commence pas une phrase par $op, ni un calcul.")
            return
        }
        if (expr.last() in operators) {
            if (expr.length >= 2 && expr[expr.length - 2] in operators && engineChar == '-')
                expr.append('-') // cas "5*-3"
            else {
                expr.setCharAt(expr.length - 1, engineChar) // remplace l'opérateur
            }
            return
        }
        if (expr.last() == '(' && engineChar != '-') { refuse("Un op\u00E9rateur juste apr\u00E8s '(' ? Non."); return }
        if (expr.length >= CalculatorEngine.MAX_EXPR_LENGTH) { refuse("L'expression d\u00E9borde !"); return }
        expr.append(engineChar)
    }

    private fun inputOpenParen() {
        startFreshIfNeeded(isDigitOrParen = true)
        if (expr.count { it == '(' } - expr.count { it == ')' } >= 10) {
            refuse("Trop de parenth\u00E8ses, on n'\u00E9crit pas une th\u00E8se.")
            return
        }
        if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == '.' || expr.last() == ')' || expr.last() == '!'))
            expr.append('\u00D7') // "3(" -> "3x("
        expr.append('(')
    }

    private fun inputCloseParen() {
        startFreshIfNeeded(isDigitOrParen = false)
        if (expr.count { it == '(' } <= expr.count { it == ')' }) { refuse("Il n'y a rien \u00E0 fermer l\u00E0."); return }
        if (expr.last() == '(') { refuse("Des parenth\u00E8ses vides ? S\u00E9rieusement ?"); return }
        if (expr.last() in operators) { refuse("On ne ferme pas sur un op\u00E9rateur."); return }
        expr.append(')')
    }

    private fun inputFactorial() {
        startFreshIfNeeded(isDigitOrParen = false)
        if (expr.isEmpty()) { refuse("Factorielle de quoi, du vide ?"); return }
        if (expr.last() != ')' && !expr.last().isDigit()) { refuse("Factorielle d'un op\u00E9rateur ? Non."); return }
        if (expr.length >= CalculatorEngine.MAX_EXPR_LENGTH) { refuse("L'expression d\u00E9borde !"); return }
        expr.append('!')
    }

    private fun doClear() {
        expr.clear()
        justEvaluated = false
        historyLabel.text = ""
        resultLabel.text = "0"
        if (Random.nextInt(3) == 0) say(Quips.randomClear())
    }

    private fun doBackspace() {
        if (justEvaluated) { doClear(); return }
        if (expr.isNotEmpty()) {
            expr.deleteCharAt(expr.length - 1)
            if (Random.nextInt(4) == 0) say(Quips.randomBack())
        }
    }

    private fun doEquals() {
        if (expr.isEmpty()) { refuse("Rien \u00E0 calculer. Tu veux que je devine ?"); return }
        try {
            val value = engine.evaluate(expr.toString())
            val formatted = engine.format(value)
            historyLabel.text = "${expr} ="
            expr.clear()
            expr.append(formatted.replace(" ", ""))
            resultLabel.text = formatted
            justEvaluated = true
            celebrate(value)
        } catch (e: CalculatorEngine.CalcException) {
            fail(e.message ?: "Erreur inconnue.")
        }
    }

    private fun refuse(msg: String) {
        say(msg, error = false)
        shake(5)
        beep()
    }

    private fun fail(msg: String) {
        say(msg, error = true)
        shake(14)
        beep()
        particles.burst(frame.width / 2, resultLabel.y + 60, count = 60, speed = 9.0, size = 10)
    }

    private fun celebrate(value: Double) {
        say(Quips.forResult(value))
        shake(6)
        val special = value == 42.0 || value == 69.0 || value == 1337.0 ||
                value == 80085.0 || value == 666.0 || value == 404.0 ||
                (value > 9000 && abs(value) < 1e6 && value == value.toLong().toDouble())
        if (special) {
            particles.confetti(frame.width)
            shake(9)
        } else {
            particles.burst(frame.width / 2, resultLabel.y + 60, count = 40, speed = 8.0, size = 9)
        }
    }

    private fun say(msg: String, error: Boolean = false) {
        statusLabel.text = msg
        statusLabel.foreground = if (error) COL_ERR else COL_TXT_DIM
    }

    // ---------------------------------------------------------------- affichage

    private fun refreshDisplay() {
        val text = expr.toString()
        exprLabel.text = text.ifEmpty { " " }
        resultLabel.text = text.ifEmpty { "0" }
        resultLabel.foreground = COL_TXT

        // taille de police adapt\u00E9e \u00E0 la longueur
        val size = when {
            text.length <= 9 -> 44f
            text.length <= 13 -> 34f
            text.length <= 18 -> 26f
            else -> 20f
        }
        resultLabel.font = Font(Font.SANS_SERIF, Font.BOLD, size.toInt())

        // aper\u00E7u live du r\u00E9sultat pendant la saisie
        if (!justEvaluated && text.isNotEmpty() && (text.last().isDigit() || text.last() == ')')) {
            try {
                val v = engine.evaluate(text)
                val f = engine.format(v)
                historyLabel.text = "\u2248 $f"
            } catch (_: Exception) {
                historyLabel.text = ""
            }
        } else if (!justEvaluated) {
            historyLabel.text = ""
        }
    }

    // ---------------------------------------------------------------- secousse de fen\u00EAtre

    private fun shake(amp: Int) {
        if (!shakeTimer.isRunning) {
            baseX = frame.x
            baseY = frame.y
            shakeTicks = 0
        }
        shakeAmp = amp.toDouble()
        shakeTimer.start()
    }

    private fun shakeStep() {
        shakeTicks++
        val t = shakeTicks / 12.0
        if (t >= 1.0) {
            frame.setLocation(baseX, baseY)
            shakeTimer.stop()
            return
        }
        val amp = shakeAmp * (1 - t)
        frame.setLocation(
            baseX + Random.nextInt(-amp.toInt() - 1, amp.toInt() + 1),
            baseY + Random.nextInt(-amp.toInt() - 1, amp.toInt() + 1)
        )
    }

    private fun beep() {
        Toolkit.getDefaultToolkit().beep()
    }
}
