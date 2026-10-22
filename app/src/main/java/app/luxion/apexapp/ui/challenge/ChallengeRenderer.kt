package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/**
 * UI de un tipo de reto. Cada tipo registra la suya con `@Binds @IntoSet ChallengeRenderer`,
 * igual que registra su `Challenge`.
 */
interface ChallengeRenderer {
    val type: ChallengeType

    /** Nombre visible del tipo; por defecto, su id. */
    @Composable
    fun label(): String = type.id

    /**
     * [wrong] es verdadero tras una respuesta incorrecta; [instance] cambia al regenerarse el reto.
     * [onUnavailable] avisa de que el reto no puede completarse (sensor sin datos, permiso revocado):
     * la sesión lo sustituye por la reserva.
     */
    @Composable
    fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    )
}

class MathRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = ChallengeType.MATH

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_math)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        // Una instancia nueva (tras un fallo o al avanzar) vacía el campo.
        var text by remember(instance) { mutableStateOf("") }
        val submit = { onAnswer(ChallengeAnswer.Text(text)) }
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
        ) {
            Text(instance.prompt, style = MaterialTheme.typography.displayMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.challenge_answer_label)) },
                singleLine = true,
                isError = wrong,
                supportingText = if (wrong) {
                    { Text(stringResource(R.string.challenge_wrong)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(stringResource(R.string.challenge_confirm)) }
        }
    }
}
