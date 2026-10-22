package app.luxion.apexapp.anticheat

/** Acceso al volumen del canal de alarma; la implementación real usa `AudioManager`. */
interface AlarmVolumeGateway {
    fun get(): Int
    fun max(): Int
    fun set(volume: Int)

    /** Avisa de cada cambio externo del volumen de alarma. Devuelve la función que cancela. */
    fun observe(onChanged: () -> Unit): () -> Unit
}

/** Fuerza el volumen de alarma al máximo mientras dure el lockdown y lo restaura después. */
class VolumeLock(
    private val gateway: AlarmVolumeGateway,
    private val onTamper: (String) -> Unit = {},
) {
    private var sessionId: String? = null
    private var previous = 0
    private var stopObserving: (() -> Unit)? = null

    @Synchronized
    fun engage(sessionId: String) {
        if (this.sessionId == null) previous = gateway.get()
        this.sessionId = sessionId
        stopObserving?.invoke()
        gateway.set(gateway.max())
        stopObserving = gateway.observe { reapply() }
    }

    @Synchronized
    fun release() {
        if (sessionId == null) return
        stopObserving?.invoke()
        stopObserving = null
        sessionId = null
        gateway.set(previous)
    }

    @Synchronized
    private fun reapply() {
        val id = sessionId ?: return
        if (gateway.get() >= gateway.max()) return
        gateway.set(gateway.max())
        onTamper(id)
    }

    /** Las teclas de volumen se consumen en la pantalla de sonar; cuenta como intento. */
    @Synchronized
    fun keyPressed() {
        val id = sessionId ?: return
        gateway.set(gateway.max())
        onTamper(id)
    }
}
