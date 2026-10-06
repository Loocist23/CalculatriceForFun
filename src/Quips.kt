import java.awt.Color
import javax.swing.Timer
import kotlin.random.Random

/** Toutes les blagues de la calculatrice, au même endroit. */
object Quips {

    val titles = listOf(
        "Calculatrice 3000 TURBO",
        "Calculatron Deluxe",
        "La Machine \u00E0 Nombres",
        "Calculette de l'Apocalypse",
        "Turbo-Calc Mark Fun",
        "Approuv\u00E9e par 9 math\u00E9maticiens sur 10",
        "Le Bolt des additions",
        "Mode Chaos : activ\u00E9"
    )

    private val generic = listOf(
        "R\u00E9sultat livr\u00E9, frais du matin.",
        "Voil\u00E0. De rien.",
        "Calcule sans faute, mais sans modeste.",
        "Les maths ont parl\u00E9.",
        "R\u00E9sultat certifi\u00E9 100% sans OGM.",
        "Tu peux me remercier en math sp\u00E9.",
        "Je sais, je suis trop rapide pour toi.",
        "Un autre calcul ? Je n'ai que \u00E7a \u00E0 faire.",
        "Compte l\u00E0-dessus, c'est cadeau."
    )

    private val zero = listOf(
        "Z\u00E9ro. Le n\u00E9ant absolu. Po\u00E9tique.",
        "Z\u00E9ro. Comme ma motivation le lundi matin."
    )

    private val one = listOf("Un. La solitude math\u00E9matique.", "Un. Toute seule au monde.")

    private val negative = listOf(
        "N\u00E9gatif... quelqu'un est dans le rouge.",
        "Moins que z\u00E9ro. Ta banque a des frissons.",
        "N\u00E9gatif : m\u00EAme les maths sont tristes."
    )

    private val huge = listOf(
        "GIGANTESQUE. La calculatrice a transpir\u00E9.",
        "Norme interm\u00E9diaire atteinte : la NASA appelle.",
        "Ce nombre a besoin de son propre code postal."
    )

    private val clears = listOf(
        "Table rase. On oublie tout, comme un lundi matin.",
        "Effac\u00E9. Quel beau vide.",
        "Nouveau d\u00E9part, nouvelle vie.",
        "Plus rien. La zen attitude."
    )

    private val backs = listOf("Hop, retir\u00E9.", "On fait marche arri\u00E8re.", "Ce chiffre n'a jamais exist\u00E9.")

    private val ignored = listOf(
        "Non. Pas comme \u00E7a.",
        "Interdit par la loi des maths.",
        "Tu essaies de me casser, avoue.",
        "L\u00E0, m\u00EAme moi je suis perdu.",
        "Non non non. R\u00E9fl\u00E9chis."
    )

    fun randomClear(): String = clears.random()
    fun randomBack(): String = backs.random()
    fun randomIgnore(): String = ignored.random()

    /** Message selon la valeur du r\u00E9sultat. */
    fun forResult(v: Double): String = when {
        v == 42.0 -> "42 : LA r\u00E9ponse \u00E0 la grande question sur la vie, l'univers et le reste."
        v == 69.0 -> "Soixante-neuf... je n'ai rien dit."
        v == 1337.0 -> "1337 : LEET. Tu es officiellement un hacker."
        v == 80085.0 -> "On a vu la m\u00EAme chose, ne nions pas."
        v == 666.0 -> "A\u00EFe. R\u00E9sultat satanique. Recule lentement."
        v == 404.0 -> "404 : r\u00E9sultat introuvable. Ah non, si. Le voil\u00E0."
        kotlin.math.abs(v - 3.14159265358979) < 1e-6 -> "Presque \u03C0 ! Les math\u00E9maticiens applaudissent."
        v == 0.0 -> zero.random()
        v == 1.0 -> one.random()
        v == 2.0 -> "Deux. Le seul nombre premier pair, une star."
        v > 9000 && v < 1e6 -> "C'EST PLUS DE NEUF MILLE !!!"
        v < -1e6 -> huge.random()
        v > 1e6 -> huge.random()
        v < 0 -> negative.random()
        v == v.toLong().toDouble() -> generic.random()
        else -> generic.random()
    }

    fun randomTitle(): String = titles.random()

    /** Couleurs de la f\u00EAte, pour les particules. */
    val partyPalette = arrayOf(
        Color(0xFF, 0x5F, 0x57),
        Color(0xFF, 0xD1, 0x3D),
        Color(0x3D, 0xC1, 0xF1),
        Color(0x7E, 0xD3, 0x50),
        Color(0xB0, 0x66, 0xFF),
        Color(0xFF, 0x7A, 0xE5)
    )

    fun randomTitleTimer(intervalMs: Int = 5000, onChange: (String) -> Unit): Timer =
        Timer(intervalMs) { onChange(randomTitle()) }
}

/** Petit utilitaire partag\u00E9. */
fun randomSign(): Double = if (Random.nextBoolean()) -1.0 else 1.0
