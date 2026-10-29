package app.luxion.apexapp.domain

import java.time.DayOfWeek

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val label: String? = null,
    val soundId: Long? = null,
    val challenges: ChallengeConfig = ChallengeConfig(),
    val soundConfig: SoundConfig = SoundConfig(),
    val antiCheat: AntiCheatPolicy = AntiCheatPolicy(),
    /** Tras resolver la alarma, comprueba que el usuario sigue despierto (ver `AwakeVerifier`). */
    val awakeVerification: Boolean = false,
) {
    init {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }
}
