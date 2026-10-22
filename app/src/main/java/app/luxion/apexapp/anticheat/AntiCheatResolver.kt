package app.luxion.apexapp.anticheat

import app.luxion.apexapp.domain.AntiCheatLevel

/** Medidas que se aplican durante un sonado. Un nivel `OFF` no activa ninguna. */
data class Protection(
    val requested: AntiCheatLevel,
    val effective: AntiCheatLevel,
    val immersive: Boolean,
    val blockBack: Boolean,
    val volumeLock: Boolean,
    val foregroundRecovery: Boolean,
    val lockTask: Boolean,
) {
    val active: Boolean get() = effective != AntiCheatLevel.OFF

    /** El nivel pedido no se puede cumplir del todo con las capacidades actuales. */
    val degraded: Boolean get() = requested != effective || (active && !foregroundRecovery)

    companion object {
        val NONE = Protection(AntiCheatLevel.OFF, AntiCheatLevel.OFF, false, false, false, false, false)
    }
}

object AntiCheatResolver {
    fun effective(requested: AntiCheatLevel, capabilities: Capabilities): Protection {
        if (requested == AntiCheatLevel.OFF) return Protection.NONE
        val strict = requested == AntiCheatLevel.STRICT && capabilities.deviceOwner
        return Protection(
            requested = requested,
            effective = if (strict) AntiCheatLevel.STRICT else AntiCheatLevel.STANDARD,
            immersive = true,
            blockBack = true,
            volumeLock = true,
            foregroundRecovery = capabilities.overlay,
            lockTask = strict,
        )
    }
}
