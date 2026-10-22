package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/**
 * Editor de los parámetros opacos (`ChallengeSpec.params`) de un tipo de reto. Se registra con
 * `@Binds @IntoSet ChallengeParamsEditor` y se muestra en la sección «Retos» mientras el tipo está habilitado.
 */
interface ChallengeParamsEditor {
    val type: ChallengeType

    @Composable
    fun Content(params: String, onChange: (String) -> Unit)

    /** Si es falso, la alarma no se puede guardar. */
    fun isValid(params: String): Boolean = true
}

/** Sección «Retos» del editor: tipos habilitados, nº de retos, dificultad, modo y parámetros por tipo. */
class ChallengesSection @Inject constructor(
    private val registry: ChallengeRegistry,
    renderers: Set<@JvmSuppressWildcards ChallengeRenderer>,
    paramsEditors: Set<@JvmSuppressWildcards ChallengeParamsEditor>,
) : EditorSection {
    override val order: Int = ORDER

    private val renderersByType = renderers.associateBy { it.type }
    private val editorsByType = paramsEditors.associateBy { it.type }

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        val config = draft.challenges
        val update = { transform: (ChallengeConfig) -> ChallengeConfig -> onChange(draft.copy(challenges = transform(config))) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(
                stringResource(R.string.challenge_editor_section),
                style = MaterialTheme.typography.titleMedium,
            )
            TypeChips(
                available = registry.types.sortedBy { it.id },
                selected = config.challenges.map { ChallengeType(it.type) }.toSet(),
                label = { renderersByType[it]?.label() ?: it.id },
                onToggle = { type -> update { it.toggled(type) } },
            )
            if (config.challenges.isEmpty()) {
                Text(
                    stringResource(R.string.challenge_editor_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CountStepper(config.count) { value -> update { it.withCount(value) } }
            DifficultySelector(config.displayDifficulty) { value -> update { it.withDifficulty(value) } }
            ModeSelector(config.mode) { value -> update { it.withMode(value) } }
            config.challenges.forEach { spec ->
                val type = ChallengeType(spec.type)
                editorsByType[type]?.Content(spec.params) { params -> update { it.withParams(type, params) } }
            }
        }
    }

    override fun isValid(draft: Alarm): Boolean = draft.challenges.challenges.all { spec ->
        editorsByType[ChallengeType(spec.type)]?.isValid(spec.params) ?: true
    }

    companion object {
        /** Los retos van antes que otras secciones habituales (sonido, anti-trampa). */
        const val ORDER = 100
    }
}
