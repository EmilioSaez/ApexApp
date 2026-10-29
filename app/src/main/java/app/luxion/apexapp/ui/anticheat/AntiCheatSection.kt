package app.luxion.apexapp.ui.anticheat

import androidx.annotation.StringRes
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.anticheat.Capabilities
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

/** Controles del editor de alarma: nivel anti-trampas y política de snooze. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiCheatSection(
    policy: AntiCheatPolicy,
    onChange: (AntiCheatPolicy) -> Unit,
    onOpenStatus: () -> Unit,
    modifier: Modifier = Modifier,
    capabilities: Capabilities = Capabilities(overlay = true),
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        SectionHeader(stringResource(R.string.anticheat_section), horizontalPadding = 0.dp)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AntiCheatLevel.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    selected = policy.level == level,
                    onClick = { onChange(policy.copy(level = level)) },
                    shape = SegmentedButtonDefaults.itemShape(index, AntiCheatLevel.entries.size, MaterialTheme.shapes.small),
                ) { Text(stringResource(level.labelRes())) }
            }
        }
        Text(
            stringResource(policy.level.hintRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (policy.level != AntiCheatLevel.OFF && !capabilities.overlay) {
            MissingNote(R.string.anticheat_missing_overlay)
        }
        Row(
            Modifier.fillMaxWidth().toggleable(
                value = policy.snooze.allowed,
                role = Role.Switch,
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(allowed = it))) },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.anticheat_snooze_allowed), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = policy.snooze.allowed, onCheckedChange = null)
        }
        if (policy.snooze.allowed) {
            SnoozeSettings(policy.snooze) { onChange(policy.copy(snooze = it)) }
        }
        TextButton(onClick = onOpenStatus) { Text(stringResource(R.string.anticheat_open_status)) }
    }
}

/** Duración y límite de la posposición, sangrados bajo el interruptor con un filete a la izquierda. */
@Composable
private fun SnoozeSettings(snooze: SnoozePolicy, onChange: (SnoozePolicy) -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
            .padding(start = ApexTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        SnoozeOptionRow(stringResource(R.string.anticheat_snooze_duration)) {
            val options = (SNOOZE_MINUTES + snooze.minutes).distinct().sorted()
            SnoozeSegments(
                labels = options.map { stringResource(R.string.anticheat_snooze_minutes_option, it) },
                selected = options.indexOf(snooze.minutes),
                onSelect = { onChange(snooze.copy(minutes = options[it])) },
            )
        }
        SnoozeOptionRow(stringResource(R.string.anticheat_snooze_limit)) {
            val counts = if (snooze.maxCount == SnoozePolicy.UNLIMITED) SNOOZE_COUNTS else (SNOOZE_COUNTS + snooze.maxCount).distinct().sorted()
            val options = counts + SnoozePolicy.UNLIMITED
            SnoozeSegments(
                labels = options.map { if (it == SnoozePolicy.UNLIMITED) stringResource(R.string.anticheat_snooze_unlimited) else it.toString() },
                selected = options.indexOf(snooze.maxCount),
                onSelect = { onChange(snooze.copy(maxCount = options[it])) },
                lastWeight = 1.8f,
            )
            if (snooze.maxCount != SnoozePolicy.UNLIMITED) {
                Text(
                    pluralStringResource(R.plurals.anticheat_snooze_limit_hint, snooze.maxCount, snooze.maxCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SnoozeOptionRow(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozeSegments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, lastWeight: Float = 1f) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size, MaterialTheme.shapes.small),
                modifier = Modifier.weight(if (index == labels.lastIndex) lastWeight else 1f),
            ) { Text(label, maxLines = 1) }
        }
    }
}

private val SNOOZE_MINUTES = listOf(5, 10, 15)
private val SNOOZE_COUNTS = listOf(1, 2, 3, 5)

@StringRes
private fun AntiCheatLevel.labelRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_standard
}

@StringRes
private fun AntiCheatLevel.hintRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_hint_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_hint_standard
}

@Composable
private fun MissingNote(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview(name = "Claro", showBackground = true, heightDp = 700)
@Composable
private fun AntiCheatLight() = ApexAppTheme(darkTheme = false) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }

@Preview(name = "Oscuro", showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AntiCheatDark() = ApexAppTheme(darkTheme = true) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 700, fontScale = 1.3f)
@Composable
private fun AntiCheatLargeFont() = ApexAppTheme(darkTheme = false) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }
