package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/**
 * Editor de los parámetros opacos (`ChallengeSpec.params`) de un tipo de reto. Se registra con
 * `@Binds @IntoSet ChallengeParamsEditor` y se muestra en la sección «Retos» mientras el tipo está habilitado.
 */
interface ChallengeParamsEditor {
    val type: ChallengeType

    @Composable
    fun Content(params: String, difficulty: Int, onChange: (String) -> Unit)

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
            ChallengeKindSelector(withChallenges = !config.isNormal, onChange = { on -> update { if (on) it.withChallenges() else it.asNormal() } })
            if (config.isNormal) {
                Text(
                    stringResource(R.string.challenge_editor_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            TypeChips(
                available = registry.types.sortedBy { it.id },
                selected = config.challenges.map { ChallengeType(it.type) }.toSet(),
                label = { renderersByType[it]?.label() ?: it.id },
                onToggle = { type -> update { it.toggled(type) } },
                inset = 0.dp,
            )
            CountStepper(
                config.displayCount,
                onChange = { value -> update { it.withCount(value) } },
                inset = 0.dp,
                minCount = config.minCount,
            )
            DifficultySelector(config.displayDifficulty, onChange = { value -> update { it.withDifficulty(value) } }, inset = 0.dp)
            ModeSelector(config.mode, onChange = { value -> update { it.withMode(value) } }, inset = 0.dp)
            config.challenges.forEach { spec ->
                val type = ChallengeType(spec.type)
                editorsByType[type]?.Content(spec.params, config.displayDifficulty) { params -> update { it.withParams(type, params) } }
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

@Composable
private fun SectionPreview(withChallenges: Boolean, dark: Boolean) = ApexAppTheme(darkTheme = dark) {
    val section = ChallengesSection(ChallengeRegistry(listOf(MathChallenge())), setOf(MathRenderer()), emptySet())
    val draft = Alarm(hour = 7, minute = 0).let { if (withChallenges) it.copy(challenges = it.challenges.withChallenges()) else it }
    androidx.compose.material3.Surface { Column(Modifier.padding(ApexTheme.spacing.screen)) { section.Content(draft) {} } }
}

@Preview(name = "Normal claro", showBackground = true)
@Composable
private fun NormalLight() = SectionPreview(withChallenges = false, dark = false)

@Preview(name = "Normal oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NormalDark() = SectionPreview(withChallenges = false, dark = true)

@Preview(name = "Con retos claro", showBackground = true, heightDp = 600)
@Composable
private fun ChallengesLight() = SectionPreview(withChallenges = true, dark = false)

@Preview(name = "Con retos oscuro", showBackground = true, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChallengesDark() = SectionPreview(withChallenges = true, dark = true)

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 600, fontScale = 1.3f)
@Composable
private fun ChallengesLargeFont() = SectionPreview(withChallenges = true, dark = false)
