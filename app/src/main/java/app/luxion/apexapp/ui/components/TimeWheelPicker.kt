package app.luxion.apexapp.ui.components

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Etiquetas de prueba de [TimeWheelPicker]. */
object TimeWheelTags {
    const val Hours = "timeWheelHours"
    const val Minutes = "timeWheelMinutes"
    const val EntryField = "timeWheelEntryField"
}

// Cada rueda es una lista con tantas repeticiones que, en la práctica, no tiene principio ni fin.
private const val Repeats = 1_000
private const val VisibleRows = 3

/**
 * Selector de hora de dos ruedas verticales (horas y minutos) con snap, inercia y háptico en cada
 * paso. Estado elevado: [hour] (0..23) y [minute] (0..59); [onTimeChange] recibe siempre la hora
 * en formato de 24 h. En 12 h el periodo AM/PM se deduce de [hour] y tiene su propio selector.
 * Tocar la fila central abre una entrada numérica; tocar una vecina la centra.
 */
@Composable
fun TimeWheelPicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    style: TextStyle = MaterialTheme.typography.displayMedium,
) {
    var typing by rememberSaveable { mutableStateOf(false) }
    val rowHeight = with(LocalDensity.current) { maxOf(style.lineHeight.toDp() + ApexTheme.spacing.sm, 48.dp) }
    val openEntry = { typing = true }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // La banda de la fila central abarca solo las ruedas, no el selector AM/PM.
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().height(rowHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Wheel(
                    tag = TimeWheelTags.Hours,
                    size = if (is24Hour) 24 else 12,
                    first = if (is24Hour) 0 else 1,
                    value = if (is24Hour) hour else hour12Of(hour),
                    onValueChange = { v -> onTimeChange(if (is24Hour) v else withHour12(hour, v), minute) },
                    label = stringResource(R.string.time_wheel_hours),
                    upLabel = stringResource(R.string.time_wheel_hours_up),
                    downLabel = stringResource(R.string.time_wheel_hours_down),
                    typeLabel = stringResource(R.string.time_wheel_type),
                    onType = openEntry,
                    style = style,
                    rowHeight = rowHeight,
                )
                Box(Modifier.height(rowHeight * VisibleRows).padding(horizontal = ApexTheme.spacing.xs)) {
                    Text(
                        ":",
                        Modifier.align(Alignment.Center).clearAndSetSemantics {},
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Wheel(
                    tag = TimeWheelTags.Minutes,
                    size = 60,
                    first = 0,
                    value = minute,
                    onValueChange = { v -> onTimeChange(hour, v) },
                    label = stringResource(R.string.time_wheel_minutes),
                    upLabel = stringResource(R.string.time_wheel_minutes_up),
                    downLabel = stringResource(R.string.time_wheel_minutes_down),
                    typeLabel = stringResource(R.string.time_wheel_type),
                    onType = openEntry,
                    style = style,
                    rowHeight = rowHeight,
                )
            }
        }
        if (!is24Hour) {
            PeriodSelector(
                pm = isPm(hour),
                onChange = { pm -> onTimeChange(withPeriod(hour, pm), minute) },
                modifier = Modifier.padding(start = ApexTheme.spacing.md),
            )
        }
    }

    if (typing) {
        TimeEntryDialog(
            initialDigits = entryDigits(hour, minute, is24Hour),
            is24Hour = is24Hour,
            pm = isPm(hour),
            onDismiss = { typing = false },
            onConfirm = { h, m ->
                typing = false
                onTimeChange(h, m)
            },
        )
    }
}

@Composable
private fun Wheel(
    tag: String,
    size: Int,
    first: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    upLabel: String,
    downLabel: String,
    typeLabel: String,
    onType: () -> Unit,
    style: TextStyle,
    rowHeight: Dp,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val start = size * (Repeats / 2) + (value - first)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start - VisibleRows / 2)

    // Índice de la fila más cercana al centro del visor: la que cuenta como elegida.
    val centre by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: start
        }
    }

    // La rueda manda mientras se arrastra: cada fila nueva en el centro es un paso (háptico + valor).
    LaunchedEffect(state) {
        snapshotFlow { centre }.collect { index ->
            val v = first + index % size
            if (v != currentValue) {
                if (state.isScrollInProgress) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnChange(v)
            }
        }
    }
    // El estado manda cuando cambia desde fuera (entrada numérica): lleva la rueda al valor por el camino corto.
    LaunchedEffect(value) {
        if (state.isScrollInProgress) return@LaunchedEffect
        var delta = (value - first) - centre % size
        if (delta > size / 2) delta -= size else if (delta < -size / 2) delta += size
        if (delta != 0) state.scrollToItem(centre + delta - VisibleRows / 2)
    }

    fun step(delta: Int) {
        scope.launch { state.animateScrollToItem(centre + delta - VisibleRows / 2) }
    }
    fun format(v: Int) = "%02d".format(v)

    Box(
        Modifier
            .testTag(tag)
            .height(rowHeight * VisibleRows)
            .semantics {
                contentDescription = label
                stateDescription = format(value)
                progressBarRangeInfo = ProgressBarRangeInfo(
                    value.toFloat(),
                    first.toFloat()..(first + size - 1).toFloat(),
                    steps = size - 2,
                )
                setProgress { target ->
                    val delta = target.toInt() - value
                    if (delta != 0) step(delta)
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(upLabel) { step(1); true },
                    CustomAccessibilityAction(downLabel) { step(-1); true },
                    CustomAccessibilityAction(typeLabel) { onType(); true },
                )
            },
    ) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
            modifier = Modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(size * Repeats) { index ->
                val isCentre = index == centre
                val emphasis by animateFloatAsState(if (isCentre) 1f else 0f, label = "wheelRow")
                Box(
                    Modifier.widthIn(min = 72.dp).height(rowHeight).padding(horizontal = ApexTheme.spacing.xs).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (isCentre) onType() else scope.launch { state.animateScrollToItem(index - VisibleRows / 2) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        format(first + index % size),
                        Modifier.graphicsLayer {
                            alpha = 0.3f + 0.7f * emphasis
                            val s = 0.8f + 0.2f * emphasis
                            scaleX = s
                            scaleY = s
                        },
                        style = style,
                        color = if (isCentre) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** AM / PM como dos opciones excluyentes; la elegida lleva borde y texto de acento. */
@Composable
private fun PeriodSelector(pm: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.time_period)
    Column(
        modifier.selectableGroup().semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
    ) {
        listOf(false to R.string.time_am, true to R.string.time_pm).forEach { (isPm, text) ->
            val on = pm == isPm
            Surface(
                modifier = Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onChange(isPm) }),
                shape = MaterialTheme.shapes.small,
                color = if (on) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(text),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Entrada numérica: se teclean los dígitos (`705` → 07:05) y el diálogo avisa si no es una hora válida. */
@Composable
private fun TimeEntryDialog(
    initialDigits: String,
    is24Hour: Boolean,
    pm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialDigits, TextRange(0, initialDigits.length)))
    }
    val focus = remember { FocusRequester() }
    val parsed = parseTimeEntry(field.text, is24Hour, pm)
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.time_wheel_type)) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { new ->
                    val digits = new.text.filter { it in '0'..'9' }.take(4)
                    field = TextFieldValue(digits, new.selection.coerceIn(0, digits.length))
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(TimeWheelTags.EntryField),
                label = { Text(stringResource(R.string.time_entry_label)) },
                textStyle = MaterialTheme.typography.headlineLarge,
                singleLine = true,
                isError = parsed == null,
                supportingText = {
                    Text(
                        parsed?.let { (h, m) -> stringResource(R.string.time_entry_preview, formatClock(h, m, is24Hour)) }
                            ?: stringResource(R.string.time_entry_invalid),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { parsed?.let { onConfirm(it.first, it.second) } }),
                shape = MaterialTheme.shapes.small,
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onConfirm(it.first, it.second) } }, enabled = parsed != null) {
                Text(stringResource(R.string.time_entry_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.time_entry_cancel)) } },
    )
}

private fun formatClock(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%02d:%02d %s".format(hour12Of(hour), minute, if (isPm(hour)) "PM" else "AM")

@Composable
private fun WheelPreview(is24Hour: Boolean) {
    var time by remember { mutableStateOf(7 to 5) }
    Box(Modifier.padding(ApexTheme.spacing.lg)) {
        TimeWheelPicker(time.first, time.second, { h, m -> time = h to m }, is24Hour = is24Hour)
    }
}

@Preview(name = "Claro 24 h", showBackground = true)
@Composable
private fun WheelLight() = ApexAppTheme(darkTheme = false) { WheelPreview(true) }

@Preview(name = "Oscuro 12 h", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WheelDark() = ApexAppTheme(darkTheme = true) { WheelPreview(false) }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun WheelLargeFont() = ApexAppTheme(darkTheme = false) { WheelPreview(false) }
