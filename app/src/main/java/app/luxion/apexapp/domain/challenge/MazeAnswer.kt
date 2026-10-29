package app.luxion.apexapp.domain.challenge

/** Celdas (índice `fila * columnas + columna`) que recorrió el trazo del laberinto, en orden. */
data class MazeTrace(val type: ChallengeType, val cells: List<Int>) : ChallengeAnswer
