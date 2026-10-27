package app.luxion.apexapp.domain.challenge

/** Tipos de reto disponibles. Otros changes añaden los suyos al conjunto que construye Hilt. */
class ChallengeRegistry(challenges: Collection<Challenge>) {
    private val byType: Map<ChallengeType, Challenge> = challenges.associateBy { it.type }

    val types: Set<ChallengeType> get() = byType.keys

    operator fun get(type: ChallengeType): Challenge? = byType[type]

    operator fun contains(type: ChallengeType): Boolean = type in byType
}
