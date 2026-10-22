package app.luxion.apexapp.ui.sharing

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.sharing.ShareIntents
import app.luxion.apexapp.sharing.SharedAlarmCodec
import app.luxion.apexapp.ui.components.EmptyState
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.components.TimeDisplay
import app.luxion.apexapp.ui.theme.ApexTheme
import kotlinx.coroutines.launch

@Composable
fun ShareAlarmScreen(onBack: () -> Unit, viewModel: ShareAlarmViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkText = state.link?.let { stringResource(R.string.share_link_text, it) }
    val linkChooser = stringResource(R.string.share_chooser_link)
    val fileChooser = stringResource(R.string.share_chooser_file)

    ShareAlarmContent(
        state = state,
        onBack = onBack,
        onShareLink = { linkText?.let { context.startActivity(ShareIntents.text(it, linkChooser)) } },
        onShareFile = {
            scope.launch {
                viewModel.fileUri()?.let {
                    context.startActivity(ShareIntents.file(it, SharedAlarmCodec.MIME_TYPE, fileChooser))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAlarmContent(
    state: ShareUiState,
    onBack: () -> Unit,
    onShareLink: () -> Unit,
    onShareFile: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        val alarm = state.alarm
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            alarm == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = stringResource(R.string.share_not_found))
            }

            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
            ) {
                Column {
                    TimeDisplay(alarm.hour, alarm.minute)
                    alarm.label?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val qr = state.qr
                if (qr != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                        QrCode(qr, Modifier.widthIn(max = 280.dp).fillMaxWidth())
                        Text(
                            stringResource(R.string.share_qr_caption),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.share_qr_too_large),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
                    PrimaryAction(
                        text = stringResource(R.string.share_link_action),
                        onClick = onShareLink,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = onShareFile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    ) { Text(stringResource(R.string.share_file_action)) }
                }
            }
        }
    }
}
