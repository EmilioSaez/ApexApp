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
    /** Silencia sonido, vibración y voz mientras el usuario interactúa con los retos. */
    val muteDuringChallenges: Boolean = false,
    /** Segundos sin interacción tras los que la alarma vuelve a sonar estando silenciada. */
    val muteInactivitySeconds: Int = DEFAULT_MUTE_INACTIVITY_SECONDS,
    /** Tras resolver la alarma, comprueba que el usuario sigue despierto (ver `AwakeVerifier`). */
    val awakeVerification: Boolean = false,
) {
    init {
        require(muteInactivitySeconds in MIN_MUTE_INACTIVITY_SECONDS..MAX_MUTE_INACTIVITY_SECONDS) {
            "muteInactivitySeconds must be in $MIN_MUTE_INACTIVITY_SECONDS..$MAX_MUTE_INACTIVITY_SECONDS"
        }
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }
}

const val DEFAULT_MUTE_INACTIVITY_SECONDS = 30
const val MIN_MUTE_INACTIVITY_SECONDS = 10
const val MAX_MUTE_INACTIVITY_SECONDS = 300
