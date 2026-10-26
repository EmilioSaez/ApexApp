package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.ui.theme.ApexTheme

/** Controles de configuración de retos compartidos por la pantalla de prueba y el editor de alarmas. */

@Composable
fun DifficultySelector(level: Int, onChange: (Int) -> Unit, inset: Dp = ApexTheme.spacing.screen) {
    ControlHeader(stringResource(R.string.challenge_test_difficulty), inset)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = inset)) {
        (SessionConfig.MIN_DIFFICULTY..SessionConfig.MAX_DIFFICULTY).forEachIndexed { index, value ->
            SegmentedButton(
                selected = value == level,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, SessionConfig.MAX_DIFFICULTY, MaterialTheme.shapes.small),
            ) { Text(value.toString()) }
        }
    }
}

@Composable
fun CountStepper(count: Int, onChange: (Int) -> Unit, inset: Dp = ApexTheme.spacing.screen) {
    ControlHeader(stringResource(R.string.challenge_test_count), inset)
    Row(
        Modifier.padding(horizontal = inset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        OutlinedIconButton(onClick = { onChange(count - 1) }, enabled = count > SessionConfig.MIN_COUNT) { Text("-") }
        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        OutlinedIconButton(onClick = { onChange(count + 1) }, enabled = count < SessionConfig.MAX_COUNT) { Text("+") }
    }
}

@Composable
fun ModeSelector(mode: ChallengeMode, onChange: (ChallengeMode) -> Unit, inset: Dp = ApexTheme.spacing.screen) {
    ControlHeader(stringResource(R.string.challenge_test_mode), inset)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = inset)) {
        listOf(
            ChallengeMode.CHAINED to stringResource(R.string.challenge_mode_chained),
            ChallengeMode.RANDOM to stringResource(R.string.challenge_mode_random),
        ).forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == mode,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small),
            ) { Text(label) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeChips(
    available: List<ChallengeType>,
    selected: Set<ChallengeType>,
    label: @Composable (ChallengeType) -> String,
    onToggle: (ChallengeType) -> Unit,
    inset: Dp = ApexTheme.spacing.screen,
) {
    ControlHeader(stringResource(R.string.challenge_test_types), inset)
    FlowRow(
        Modifier.padding(horizontal = inset),
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
    ) {
        available.forEach { type ->
            FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                shape = MaterialTheme.shapes.small,
                label = { Text(label(type)) },
            )
        }
    }
}

/** Cabecera de control con margen propio: en el editor el margen de pantalla ya lo pone el contenedor. */
@Composable
private fun ControlHeader(text: String, inset: Dp) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset, vertical = ApexTheme.spacing.md),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
