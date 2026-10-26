package app.luxion.apexapp.ui.sharing

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.sharing.ShareIntents
import app.luxion.apexapp.sharing.StreakCardColors
import app.luxion.apexapp.sharing.StreakCardRenderer
import app.luxion.apexapp.sharing.StreakCardText
import app.luxion.apexapp.ui.components.EmptyState
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun StreakCardScreen(onBack: () -> Unit, viewModel: StreakCardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooser = stringResource(R.string.streak_chooser)
    StreakCardContent(
        state = state,
        onBack = onBack,
        onShare = { card ->
            context.startActivity(ShareIntents.file(viewModel.imageUri(card), "image/png", chooser))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakCardContent(state: StreakCardUiState, onBack: () -> Unit, onShare: (Bitmap) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.streak_share_open), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        val streak = state.streak
        when {
            streak == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !state.hasAchievements -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = stringResource(R.string.streak_empty))
            }

            else -> CardPreview(streak, onShare, Modifier.padding(padding))
        }
    }
}

@Composable
private fun CardPreview(streak: Streak, onShare: (Bitmap) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = StreakCardColors(
        background = scheme.background.toArgb(),
        primaryText = scheme.onBackground.toArgb(),
        secondaryText = scheme.onSurfaceVariant.toArgb(),
        accent = scheme.primary.toArgb(),
    )
    val text = StreakCardText(
        title = stringResource(R.string.streak_card_title),
        currentLabel = stringResource(R.string.streak_card_current),
        currentUnit = pluralStringResource(R.plurals.streak_unit, streak.current),
        bestLabel = stringResource(R.string.streak_card_best),
        bestValue = pluralStringResource(R.plurals.streak_days, streak.best, streak.best),
        footer = stringResource(R.string.streak_card_footer),
    )
    val card = remember(streak, colors, text) {
        val display = ResourcesCompat.getFont(context, R.font.sora) ?: Typeface.DEFAULT
        val body = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
        StreakCardRenderer.render(streak, text, colors, display, body)
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
    ) {
        Image(
            bitmap = card.asImageBitmap(),
            contentDescription = stringResource(R.string.streak_card_description, streak.current, streak.best),
            modifier = Modifier.widthIn(max = ApexTheme.spacing.xxxl * 8).fillMaxWidth(),
        )
        PrimaryAction(
            text = stringResource(R.string.streak_share_action),
            onClick = { onShare(card) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
