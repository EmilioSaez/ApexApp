package app.luxion.apexapp.ui.anticheat

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexTheme

/** Controles del editor de alarma: nivel anti-trampas y política de snooze. */
@Composable
fun AntiCheatSection(
    policy: AntiCheatPolicy,
    onChange: (AntiCheatPolicy) -> Unit,
    onOpenStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        SectionHeader(stringResource(R.string.anticheat_section))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            AntiCheatLevel.entries.forEach { level ->
                FilterChip(
                    selected = policy.level == level,
                    onClick = { onChange(policy.copy(level = level)) },
                    label = { Text(stringResource(level.labelRes())) },
                )
            }
        }
        Text(
            stringResource(policy.level.hintRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.anticheat_snooze_allowed), Modifier.weight(1f))
            Switch(
                checked = policy.snooze.allowed,
                onCheckedChange = { onChange(policy.copy(snooze = policy.snooze.copy(allowed = it))) },
            )
        }
        if (policy.snooze.allowed) {
            Text(stringResource(R.string.anticheat_snooze_minutes, policy.snooze.minutes))
            Slider(
                value = policy.snooze.minutes.toFloat(),
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(minutes = it.toInt()))) },
                valueRange = 1f..30f,
            )
            Text(stringResource(R.string.anticheat_snooze_max, policy.snooze.maxCount))
            Slider(
                value = policy.snooze.maxCount.toFloat(),
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(maxCount = it.toInt()))) },
                valueRange = 1f..10f,
            )
        }
        TextButton(onClick = onOpenStatus) { Text(stringResource(R.string.anticheat_open_status)) }
    }
}

@StringRes
private fun AntiCheatLevel.labelRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_standard
    AntiCheatLevel.STRICT -> R.string.anticheat_level_strict
}

@StringRes
private fun AntiCheatLevel.hintRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_hint_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_hint_standard
    AntiCheatLevel.STRICT -> R.string.anticheat_level_hint_strict
}
