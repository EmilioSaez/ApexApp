package app.luxion.apexapp.ui.editor.sound

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.SoundSource
import app.luxion.apexapp.domain.VibrationPatterns
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Sección "Sonido" del editor: fuente, rampa, vibración y voz de la alarma en edición, con
 * previsualización. El editor la incrusta pasando el borrador y recibe el borrador modificado.
 */
@Composable
fun SoundSettingsSection(
    alarm: Alarm,
    onChange: (Alarm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SoundSettingsViewModel = hiltViewModel(),
) {
    val previewing by viewModel.previewing.collectAsStateWithLifecycle()
    val sound by produceState<String?>(initialValue = null, alarm.soundId) {
        value = viewModel.sound(alarm.soundId)?.name
    }
    // La previsualización no sobrevive a la pantalla.
    DisposableEffect(Unit) { onDispose { viewModel.stopPreview() } }

    SoundSettingsContent(
        alarm = alarm,
        soundName = sound,
        previewing = previewing,
        onChange = onChange,
        onPreview = { viewModel.togglePreview(alarm) },
        viewModel = viewModel,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundSettingsContent(
    alarm: Alarm,
    soundName: String?,
    previewing: Boolean,
    onChange: (Alarm) -> Unit,
    onPreview: () -> Unit,
    viewModel: SoundSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val spacing = ApexTheme.spacing
    val config = alarm.soundConfig
    val scope = rememberCoroutineScope()
    var rejected by rememberSaveable { mutableStateOf(false) }
    val update = { new: SoundConfig -> onChange(alarm.copy(soundConfig = new)) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val id = viewModel.pickFile(uri.toString())
            rejected = id == null
            if (id != null) onChange(alarm.copy(soundId = id))
        }
    }
    val tonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        } else {
            null
        }
        if (uri != null) scope.launch {
            val id = viewModel.pickSystemTone(uri.toString())
            rejected = id == null
            if (id != null) onChange(alarm.copy(soundId = id))
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        SectionHeader(stringResource(R.string.sound_section))

        Text(
            soundName ?: stringResource(R.string.sound_default),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedButton(onClick = { rejected = false; onChange(alarm.copy(soundId = null)) }) {
                Text(stringResource(R.string.sound_use_default))
            }
            OutlinedButton(onClick = {
                rejected = false
                tonePicker.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false),
                )
            }) { Text(stringResource(R.string.sound_pick_system)) }
            OutlinedButton(onClick = { rejected = false; filePicker.launch(arrayOf("audio/*")) }) {
                Text(stringResource(R.string.sound_pick_file))
            }
        }
        if (rejected) {
            Text(
                stringResource(R.string.sound_not_audio),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LabeledSlider(
            label = if (config.rampSeconds == 0) {
                stringResource(R.string.sound_ramp_off)
            } else {
                stringResource(R.string.sound_ramp_seconds, config.rampSeconds)
            },
            value = config.rampSeconds.toFloat(),
            range = 0f..SoundConfig.MAX_RAMP_SECONDS.toFloat(),
            steps = SoundConfig.MAX_RAMP_SECONDS / 5 - 1,
            onValue = { update(config.copy(rampSeconds = it.roundToInt())) },
        )

        SwitchRow(stringResource(R.string.vibration_enabled), config.vibrate) { update(config.copy(vibrate = it)) }
        if (config.vibrate) VibrationPicker(config.vibrationPattern) { update(config.copy(vibrationPattern = it)) }

        SwitchRow(stringResource(R.string.voice_speak_time), config.speakTime) { update(config.copy(speakTime = it)) }
        SwitchRow(stringResource(R.string.voice_reminder), config.ttsEnabled) { update(config.copy(ttsEnabled = it)) }
        if (config.ttsEnabled) {
            OutlinedTextField(
                value = config.ttsText.orEmpty(),
                onValueChange = { update(config.copy(ttsText = it.take(SoundConfig.MAX_TTS_TEXT_LENGTH).ifEmpty { null })) },
                label = { Text(stringResource(R.string.voice_reminder_text)) },
                supportingText = { Text("${config.ttsText.orEmpty().length}/${SoundConfig.MAX_TTS_TEXT_LENGTH}") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (config.ttsEnabled || config.speakTime) {
            val repeating = config.speakRepeatSeconds > 0
            SwitchRow(stringResource(R.string.voice_repeat), repeating) {
                update(config.copy(speakRepeatSeconds = if (it) 60 else 0))
            }
            if (repeating) {
                LabeledSlider(
                    label = stringResource(R.string.voice_repeat_every, config.speakRepeatSeconds),
                    value = config.speakRepeatSeconds.toFloat(),
                    range = SoundConfig.MIN_SPEAK_REPEAT_SECONDS.toFloat()..300f,
                    steps = 300 / 15 - 2,
                    onValue = { update(config.copy(speakRepeatSeconds = (it / 15).roundToInt() * 15)) },
                )
            }
        }

        OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (previewing) R.string.sound_preview_stop else R.string.sound_preview))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibrationPicker(pattern: String, onPattern: (String) -> Unit) {
    val spacing = ApexTheme.spacing
    val isCustom = pattern !in VibrationPatterns.presets
    var draft by rememberSaveable(isCustom) {
        mutableStateOf(if (isCustom) pattern.removePrefix(VibrationPatterns.CUSTOM_PREFIX) else "200,100,400")
    }
    val valid = VibrationPatterns.isValid(VibrationPatterns.CUSTOM_PREFIX + draft)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            VibrationPatterns.presets.keys.forEach { id ->
                FilterChip(
                    selected = pattern == id,
                    onClick = { onPattern(id) },
                    label = { Text(stringResource(presetLabel(id))) },
                )
            }
            FilterChip(
                selected = isCustom,
                onClick = { if (valid) onPattern(VibrationPatterns.CUSTOM_PREFIX + draft) },
                label = { Text(stringResource(R.string.vibration_custom)) },
            )
        }
        if (isCustom) {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    if (VibrationPatterns.isValid(VibrationPatterns.CUSTOM_PREFIX + it)) {
                        onPattern(VibrationPatterns.CUSTOM_PREFIX + it)
                    }
                },
                isError = !valid,
                label = { Text(stringResource(R.string.vibration_custom_hint)) },
                supportingText = if (valid) null else ({ Text(stringResource(R.string.vibration_custom_invalid)) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun presetLabel(id: String): Int = when (id) {
    "pulse" -> R.string.vibration_pulse
    "sos" -> R.string.vibration_sos
    else -> R.string.vibration_steady
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValue: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value = value, onValueChange = onValue, valueRange = range, steps = steps)
    }
}
