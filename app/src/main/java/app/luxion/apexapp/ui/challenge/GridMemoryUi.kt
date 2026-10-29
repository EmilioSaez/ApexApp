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
import app.luxion.apexapp.domain.challenge.gridmemory.GridMemoryChallenge
import app.luxion.apexapp.domain.challenge.gridmemory.GridMemoryInstance
import app.luxion.apexapp.domain.challenge.gridmemory.GridMemoryLevels
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val StartDelayMs = 600L

class GridMemoryRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = GridMemoryChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_grid_memory)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        val grid = instance as GridMemoryInstance
        // La sesión conserva la instancia tras un fallo, así que el reintento lo cuenta el propio renderer.
        var attempt by remember(grid) { mutableIntStateOf(0) }
        var taps by remember(grid, attempt) { mutableStateOf(emptyList<Int>()) }
        var showing by remember(grid, attempt) { mutableStateOf(true) }
        var revealed by remember(grid, attempt) { mutableStateOf(false) }

        LaunchedEffect(grid, attempt) {
            showing = true
            revealed = false
            delay(StartDelayMs)
            revealed = true
            delay(grid.showMs)
            revealed = false
            showing = false
        }

        val onTap = { cell: Int ->
            if (!showing && cell !in taps) {
                val next = taps + cell
                taps = next
                val mismatch = !grid.isTarget(cell)
                if (mismatch || next.size == grid.cells.size) {
                    onAnswer(ChallengeAnswer.Pattern(grid.type, next))
                    if (mismatch) attempt++
                }
            }
        }

        val gap = if (grid.side > 4) ApexTheme.spacing.xs else ApexTheme.spacing.sm
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
            Text(
                if (showing) {
                    stringResource(R.string.challenge_grid_memory_watch)
                } else {
                    stringResource(R.string.challenge_grid_memory_remaining, grid.cells.size - taps.size)
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (wrong && !showing) {
                Text(
                    stringResource(R.string.challenge_grid_memory_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(grid.side) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(grid.side) { col ->
                            val cell = row * grid.side + col
                            val on = (revealed && grid.isTarget(cell)) || cell in taps
                            GridMemoryCell(
                                index = cell,
                                on = on,
                                enabled = !showing,
                                onClick = { onTap(cell) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridMemoryCell(index: Int, on: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val description = stringResource(
        if (on) R.string.challenge_grid_memory_cell_on else R.string.challenge_grid_memory_cell,
        index + 1,
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {}
}

/** Editor sin parámetros: tablero y cantidad salen de la dificultad, aquí solo se muestran. */
class GridMemoryParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = GridMemoryChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_grid_memory), style = MaterialTheme.typography.titleSmall)
            val side = GridMemoryLevels.sideFor(difficulty)
            val count = GridMemoryLevels.countFor(difficulty)
            Text(
                pluralStringResource(R.plurals.challenge_target_info_grid_memory, count, side, side, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
