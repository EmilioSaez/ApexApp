package app.luxion.apexapp.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.adaptive.AdaptiveSettings
import app.luxion.apexapp.domain.adaptive.ChangeReason
import app.luxion.apexapp.ui.theme.ApexTheme

/** Sección del editor de alarmas con su propio estado. */
@Composable
fun AdaptiveDifficultySection(
    modifier: Modifier = Modifier,
    viewModel: AdaptiveDifficultyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AdaptiveDifficultyContent(
        state = state,
        onEnabled = viewModel::setEnabled,
        onRange = viewModel::setRange,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

/** Contenido sin estado: interruptor, explicación, nivel actual con su motivo, rango y reinicio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveDifficultyContent(
    state: AdaptiveUiState,
    onEnabled: (Boolean) -> Unit,
    onRange: (min: Int, max: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.adaptive_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Switch(checked = state.enabled, onCheckedChange = onEnabled, enabled = state.available)
        }
        Text(
            stringResource(R.string.adaptive_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!state.available) {
            Text(
                stringResource(R.string.adaptive_save_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (!state.enabled) return@Column
        Text(
            stringResource(R.string.adaptive_current_level, state.current),
            style = MaterialTheme.typography.titleMedium,
        )
        state.reason?.let {
            Text(
                adaptiveReasonText(it, state.current, state.hits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.adaptive_range, state.min, state.max),
            style = MaterialTheme.typography.bodyMedium,
        )
        RangeSlider(
            value = state.min.toFloat()..state.max.toFloat(),
            onValueChange = { onRange(it.start.toInt(), it.endInclusive.toInt()) },
            valueRange = AdaptiveSettings.MIN_LEVEL.toFloat()..AdaptiveSettings.MAX_LEVEL.toFloat(),
            steps = AdaptiveSettings.MAX_LEVEL - AdaptiveSettings.MIN_LEVEL - 1,
        )
        TextButton(onClick = onReset) { Text(stringResource(R.string.adaptive_reset)) }
    }
}

/** Frase que explica un cambio de nivel, p. ej. "Subida a 4: pospusiste 2 de las últimas 3 veces". */
@Composable
fun adaptiveReasonText(reason: ChangeReason, level: Int, hits: Int): String = when (reason) {
    ChangeReason.SNOOZES -> stringResource(R.string.adaptive_reason_snoozes, level, hits)
    ChangeReason.EASY -> stringResource(R.string.adaptive_reason_easy, level)
    ChangeReason.FAILURES -> stringResource(R.string.adaptive_reason_failures, level, hits)
    ChangeReason.SLOW -> stringResource(R.string.adaptive_reason_slow, level, hits)
}

/** Línea breve que avisa en la pantalla del reto de que el nivel ha cambiado. */
@Composable
fun AdaptiveLevelNotice(notice: AdaptiveNotice, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            adaptiveReasonText(notice.reason, notice.level, notice.hits),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(ApexTheme.spacing.md),
        )
    }
}
