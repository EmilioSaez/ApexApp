package app.luxion.apexapp.anticheat

/** Privilegios disponibles para resistir manipulaciones; los demuestra [CapabilityChecker]. */
data class Capabilities(
    val overlay: Boolean = false,
    val deviceAdmin: Boolean = false,
    val deviceOwner: Boolean = false,
)

interface CapabilityChecker {
    fun current(): Capabilities
}
