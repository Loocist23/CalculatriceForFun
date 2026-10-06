import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Champ de particules affich\u00E9 en glass pane : explosions au clic, confettis de f\u00EAte.
 * Anim\u00E9 par un javax.swing.Timer \u00E0 ~60 FPS, s'arr\u00EAte tout seul quand c'est vide.
 */
class ParticleField : JPanel() {

    private class Particle(
        var x: Double, var y: Double,
        var vx: Double, var vy: Double,
        var life: Int, var size: Int,
        val color: Color, val square: Boolean, val spin: Double, var angle: Double
    )

    private val particles = ArrayList<Particle>()
    private val timer = Timer(16) { step() }
    private val maxParticles = 1200

    init {
        isOpaque = false
        timer.isRepeats = true
    }

    /** Explosion radiale au point (coordonn\u00E9es du composant). */
    fun burst(x: Int, y: Int, count: Int, palette: Array<Color> = Quips.partyPalette, speed: Double = 6.0, size: Int = 7) {
        if (particles.size >= maxParticles) return
        repeat(count) {
            val a = Random.nextDouble(Math.PI * 2)
            val s = speed * (0.35 + Random.nextDouble(0.9))
            particles.add(
                Particle(
                    x.toDouble(), y.toDouble(),
                    s * kotlin.math.cos(a), s * kotlin.math.sin(a),
                    life = 35 + Random.nextInt(25),
                    size = size / 2 + Random.nextInt(size),
                    color = palette.random(),
                    square = Random.nextBoolean(),
                    spin = Random.nextDouble(-0.4, 0.4),
                    angle = Random.nextDouble(Math.PI * 2)
                )
            )
        }
        startIfNeeded()
    }

    /** Pluie de confettis depuis le haut de la fen\u00EAtre. */
    fun confetti(width: Int) {
        if (particles.size >= maxParticles) return
        repeat(220) {
            particles.add(
                Particle(
                    Random.nextDouble(width.toDouble()), -20.0,
                    Random.nextDouble(-2.0, 2.0), Random.nextDouble(2.0, 7.0),
                    life = 90 + Random.nextInt(60),
                    size = 5 + Random.nextInt(8),
                    color = Quips.partyPalette.random(),
                    square = Random.nextBoolean(),
                    spin = Random.nextDouble(-0.35, 0.35),
                    angle = Random.nextDouble(Math.PI * 2)
                )
            )
        }
        startIfNeeded()
    }

    private fun startIfNeeded() {
        if (!timer.isRunning) timer.start()
    }

    private fun step() {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life--
            if (p.life <= 0) { it.remove(); continue }
            p.vy += 0.18                       // gravité
            p.vx *= 0.985                      // frottement
            p.vy *= 0.985
            p.x += p.vx
            p.y += p.vy
            p.angle += p.spin
        }
        if (particles.isEmpty()) timer.stop()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val fading = CompositeCache.alpha
        for (p in particles) {
            val alpha = (p.life.coerceAtMost(20) / 20.0).toFloat()
            g2.composite = fading.derive(alpha)
            g2.color = p.color
            val s = p.size
            if (p.square) {
                val cx = p.x; val cy = p.y
                val d = s / 2.0
                g2.rotate(p.angle, cx, cy)
                g2.fillRect((cx - d).toInt(), (cy - d).toInt(), s, s)
                g2.rotate(-p.angle, cx, cy)
            } else {
                val r = s / 2.0
                g2.fillOval((p.x - r).toInt(), (p.y - r).toInt(), s, s)
            }
        }
        g2.composite = fading.derive(1f)
    }
}

/** Cache simple pour ne pas recr\u00E9er un AlphaComposite \u00E0 chaque frame. */
private object CompositeCache {
    val alpha: java.awt.AlphaComposite = java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER)
}

/** Utilis\u00E9 par ParticleField.confetti pour mesurer, et utile ailleurs. */
fun particleSpeedFor(count: Int): Double = 4.0 + sqrt(count.toDouble()) / 2.0
