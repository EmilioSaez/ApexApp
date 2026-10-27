package app.luxion.apexapp.domain.challenge.flappy

import kotlin.random.Random

enum class FlappyPhase { READY, PLAYING, CRASHED, WON }

/** Tubería: [x] es su borde izquierdo y [gapCenter] el centro vertical del hueco. */
class FlappyPipe(var x: Float, val gapCenter: Float, var passed: Boolean = false)

/**
 * Lógica del juego sin dependencias de Android. Mundo de [WIDTH] x [HEIGHT] unidades con el eje Y hacia abajo;
 * la interfaz solo lo escala. Se avanza con [step] y el toque llama a [flap]. Al chocar se pierde la puntuación:
 * hay que sumar [target] puntos seguidos.
 */
class FlappyGame(private val target: Int, private val random: Random = Random.Default) {
    var phase: FlappyPhase = FlappyPhase.READY
        private set
    var birdY: Float = HEIGHT / 2
        private set
    var velocity: Float = 0f
        private set
    var score: Int = 0
        private set
    val pipes = mutableListOf<FlappyPipe>()

    private var idleTime = 0f
    private var crashedFor = 0f

    /** Primer toque: arranca; en vuelo, salta; tras un choque, reinicia cuando acaba el breve bloqueo. */
    fun flap() {
        when (phase) {
            FlappyPhase.READY -> {
                phase = FlappyPhase.PLAYING
                velocity = FLAP_VELOCITY
            }
            FlappyPhase.PLAYING -> velocity = FLAP_VELOCITY
            FlappyPhase.CRASHED -> if (crashedFor >= RESTART_LOCKOUT) reset()
            FlappyPhase.WON -> Unit
        }
    }

    fun step(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, MAX_STEP)
        when (phase) {
            FlappyPhase.READY -> {
                idleTime += dt
                birdY = HEIGHT / 2 + HOVER_AMPLITUDE * kotlin.math.sin(idleTime * HOVER_SPEED)
            }
            FlappyPhase.PLAYING -> advance(dt)
            FlappyPhase.CRASHED -> crashedFor += dt
            FlappyPhase.WON -> Unit
        }
    }

    private fun advance(dt: Float) {
        velocity += GRAVITY * dt
        birdY += velocity * dt
        pipes.forEach { it.x -= SPEED * dt }
        pipes.removeAll { it.x + PIPE_WIDTH < 0f }
        val last = pipes.lastOrNull()
        if (last == null || last.x <= WIDTH - PIPE_SPACING) spawn(last)

        if (hitsBounds() || pipes.any { hits(it) }) {
            phase = FlappyPhase.CRASHED
            crashedFor = 0f
            return
        }
        for (pipe in pipes) {
            if (!pipe.passed && pipe.x + PIPE_WIDTH < BIRD_X - BIRD_RADIUS) {
                pipe.passed = true
                score++
            }
        }
        if (score >= target) phase = FlappyPhase.WON
    }

    private fun spawn(last: FlappyPipe?) {
        val min = GAP / 2 + MARGIN
        val max = HEIGHT - GAP / 2 - MARGIN
        // Se limita el desnivel entre huecos consecutivos para que siempre se pueda llegar al siguiente.
        val lo = if (last == null) min else maxOf(min, last.gapCenter - MAX_GAP_SHIFT)
        val hi = if (last == null) max else minOf(max, last.gapCenter + MAX_GAP_SHIFT)
        pipes += FlappyPipe(x = if (last == null) WIDTH + FIRST_PIPE_OFFSET else WIDTH, gapCenter = lo + random.nextFloat() * (hi - lo))
    }

    private fun hitsBounds() = birdY - HIT_RADIUS < 0f || birdY + HIT_RADIUS > HEIGHT

    private fun hits(pipe: FlappyPipe): Boolean {
        val overlapsX = BIRD_X + HIT_RADIUS > pipe.x && BIRD_X - HIT_RADIUS < pipe.x + PIPE_WIDTH
        val inGap = birdY - HIT_RADIUS >= pipe.gapCenter - GAP / 2 && birdY + HIT_RADIUS <= pipe.gapCenter + GAP / 2
        return overlapsX && !inGap
    }

    private fun reset() {
        phase = FlappyPhase.READY
        birdY = HEIGHT / 2
        velocity = 0f
        score = 0
        pipes.clear()
        idleTime = 0f
    }

    /** Distancia vertical del pájaro al centro del hueco de la próxima tubería; 0 si aún no hay ninguna. Para tests. */
    internal fun offsetToNextGap(): Float {
        val next = pipes.firstOrNull { it.x + PIPE_WIDTH >= BIRD_X - HIT_RADIUS } ?: return 0f
        return birdY - next.gapCenter
    }

    companion object {
        const val WIDTH = 360f
        const val HEIGHT = 440f
        const val BIRD_X = 90f
        const val BIRD_RADIUS = 14f
        const val PIPE_WIDTH = 54f
        const val GAP = 150f

        private const val HIT_RADIUS = BIRD_RADIUS * 0.85f
        private const val GRAVITY = 1000f
        private const val FLAP_VELOCITY = -330f
        private const val SPEED = 120f
        private const val PIPE_SPACING = 200f
        private const val FIRST_PIPE_OFFSET = 60f
        private const val MARGIN = 40f
        private const val MAX_GAP_SHIFT = 110f
        private const val MAX_STEP = 1f / 30f
        private const val RESTART_LOCKOUT = 0.6f
        private const val HOVER_AMPLITUDE = 8f
        private const val HOVER_SPEED = 4f
    }
}
