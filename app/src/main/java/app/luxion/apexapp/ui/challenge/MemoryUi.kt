package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.memory.MemoryChallenge
import app.luxion.apexapp.domain.challenge.memory.MemoryInstance
import app.luxion.apexapp.domain.challenge.memory.MemoryLevels
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val GridColumns = 3
private const val StartDelayMs = 600L
private const val GapMs = 200L

class MemoryRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = MemoryChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_memory)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        val memory = instance as MemoryInstance
        // La sesión conserva la instancia tras un fallo, así que el reintento lo cuenta el propio renderer.
        var attempt by remember(memory) { mutableIntStateOf(0) }
        var taps by remember(memory, attempt) { mutableStateOf(emptyList<Int>()) }
        var lit by remember(memory, attempt) { mutableStateOf<Int?>(null) }
        var playing by remember(memory, attempt) { mutableStateOf(true) }

        LaunchedEffect(memory, attempt) {
            playing = true
            delay(StartDelayMs)
            memory.sequence.forEach { cell ->
                lit = cell
                delay(memory.stepMs)
                lit = null
                delay(GapMs)
            }
            playing = false
        }

        val onTap = { cell: Int ->
            if (!playing) {
                val next = taps + cell
                taps = next
                val mismatch = next.last() != memory.sequence[next.lastIndex]
                if (mismatch || next.size == memory.sequence.size) {
                    onAnswer(ChallengeAnswer.Pattern(memory.type, next))
                    if (mismatch) attempt++
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
            Text(
                stringResource(if (playing) R.string.challenge_memory_watch else R.string.challenge_memory_repeat),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (wrong && !playing) {
                Text(
                    stringResource(R.string.challenge_memory_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            repeat(MemoryLevels.GRID_SIZE / GridColumns) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                    repeat(GridColumns) { col ->
                        val cell = row * GridColumns + col
                        MemoryCell(
                            index = cell,
                            lit = lit == cell,
                            enabled = !playing,
                            onClick = { onTap(cell) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCell(index: Int, lit: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val description = stringResource(
        if (lit) R.string.challenge_memory_cell_lit else R.string.challenge_memory_cell,
        index + 1,
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {}
}

/** Editor sin parámetros: la longitud y el ritmo salen de la dificultad, aquí solo se muestran. */
class MemoryParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = MemoryChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_memory), style = MaterialTheme.typography.titleSmall)
            val length = MemoryLevels.lengthFor(difficulty)
            Text(
                pluralStringResource(R.plurals.challenge_target_info_memory, length, length),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
