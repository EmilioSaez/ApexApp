package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/**
 * UI de un tipo de reto. Cada tipo registra la suya con `@Binds @IntoSet ChallengeRenderer`,
 * igual que registra su `Challenge`.
 */
interface ChallengeRenderer {
    val type: ChallengeType

    /** Nombre visible del tipo; por defecto, su id. */
    @Composable
    fun label(): String = type.id

    /**
     * [wrong] es verdadero tras una respuesta incorrecta; [instance] cambia al regenerarse el reto.
     * [onUnavailable] avisa de que el reto no puede completarse (sensor sin datos, permiso revocado):
     * la sesión lo sustituye por la reserva.
     */
    @Composable
    fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    )
}

class MathRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = ChallengeType.MATH

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_math)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        // Una instancia nueva (tras un fallo o al avanzar) vacia la respuesta.
        var text by remember(instance) { mutableStateOf("") }
        val submit = { onAnswer(ChallengeAnswer.Text(text)) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.padding(ApexTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
                ) {
                    Text(
                        stringResource(R.string.challenge_solve_to_silence).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            instance.prompt,
                            modifier = Modifier.weight(1f, fill = false),
                            style = if (instance.prompt.length <= ShortPrompt) {
                                MaterialTheme.typography.displayMedium
                            } else {
                                MaterialTheme.typography.headlineLarge
                            },
                            textAlign = TextAlign.Center,
                        )
                        Text("=", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnswerBox(text, wrong)
                }
            }
            Keypad(
                onDigit = { if (text.length < MaxAnswerLength) text += it },
                onBackspace = { text = text.dropLast(1) },
            )
            PrimaryAction(
                text = stringResource(R.string.challenge_confirm),
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotEmpty(),
                icon = painterResource(R.drawable.ic_check),
            )
        }
    }
}

private const val ShortPrompt = 9
private const val MaxAnswerLength = 6

/** Respuesta en curso: caja de borde fino; en error, el borde y el aviso pasan al color de fallo. */
@Composable
private fun AnswerBox(text: String, wrong: Boolean) {
    val color = if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
        Surface(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, color),
        ) {
            Box(Modifier.padding(ApexTheme.spacing.md), contentAlignment = Alignment.Center) {
                Text(
                    text.ifEmpty { "\u2014" },
                    modifier = Modifier.semantics { contentDescription = text },
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (wrong) {
            Text(stringResource(R.string.challenge_wrong), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Teclado de 3 columnas: 1-9, borrar y 0. Evita abrir el teclado del sistema con la alarma sonando. */
@Composable
private fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("<", "0", ""))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.weight(1f))
                        "<" -> KeypadKey(Modifier.weight(1f), onBackspace) {
                            Icon(painterResource(R.drawable.ic_backspace), contentDescription = stringResource(R.string.challenge_backspace))
                        }
                        else -> KeypadKey(Modifier.weight(1f), { onDigit(key) }) {
                            Text(key, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

