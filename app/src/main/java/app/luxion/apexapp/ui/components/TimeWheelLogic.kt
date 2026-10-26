package app.luxion.apexapp.ui.components

/** Lógica pura del selector de hora: conversión 12/24 h y entrada numérica. */

/** Hora en reloj de 12 h (1..12) de una hora de 0..23. */
internal fun hour12Of(hour24: Int): Int = (hour24 + 11) % 12 + 1

internal fun isPm(hour24: Int): Boolean = hour24 >= 12

/** Hora de 0..23 a partir de una hora de 1..12 y el periodo. */
internal fun hour24Of(hour12: Int, pm: Boolean): Int = hour12 % 12 + if (pm) 12 else 0

/** Mismo periodo, otra hora de 1..12 (el AM/PM no cambia al girar la rueda de horas). */
internal fun withHour12(hour24: Int, hour12: Int): Int = hour24Of(hour12, isPm(hour24))

internal fun withPeriod(hour24: Int, pm: Boolean): Int = hour24Of(hour12Of(hour24), pm)

/** Dígitos que muestra el cuadro de entrada para una hora: `HHMM` en la escala visible. */
internal fun entryDigits(hour24: Int, minute: Int, is24Hour: Boolean): String =
    "%02d%02d".format(if (is24Hour) hour24 else hour12Of(hour24), minute)

/**
 * Interpreta lo tecleado: 1–2 dígitos son la hora (`7` → 07:00), 3 son `H:MM` (`705` → 07:05) y
 * 4 son `HH:MM`. Devuelve hora (0..23) y minuto, o `null` si está vacío o fuera de rango. En 12 h
 * la hora va de 1 a 12 y el periodo lo da [pm].
 */
internal fun parseTimeEntry(digits: String, is24Hour: Boolean, pm: Boolean): Pair<Int, Int>? {
    if (digits.isEmpty() || digits.length > 4 || !digits.all { it in '0'..'9' }) return null
    val (h, m) = when (digits.length) {
        1, 2 -> digits.toInt() to 0
        else -> digits.dropLast(2).toInt() to digits.takeLast(2).toInt()
    }
    if (m !in 0..59) return null
    return if (is24Hour) {
        if (h in 0..23) h to m else null
    } else {
        if (h in 1..12) hour24Of(h, pm) to m else null
    }
}
