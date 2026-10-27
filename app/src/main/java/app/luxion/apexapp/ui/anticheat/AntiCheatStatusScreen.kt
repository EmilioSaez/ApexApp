package app.luxion.apexapp.ui.anticheat

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.anticheat.AndroidCapabilityChecker
import app.luxion.apexapp.anticheat.Capabilities
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AntiCheatStatusViewModel @Inject constructor(
    private val checker: AndroidCapabilityChecker,
) : ViewModel() {
    private val _capabilities = MutableStateFlow(checker.current())
    val capabilities: StateFlow<Capabilities> = _capabilities

    fun refresh() {
        _capabilities.value = checker.current()
    }

    fun releaseDeviceOwner() {
        checker.clearDeviceOwner()
        refresh()
    }

    fun overlayIntent(): Intent = checker.overlaySettingsIntent()
    fun adminIntent(): Intent = checker.deviceAdminIntent()
}

@Composable
fun AntiCheatStatusScreen(onBack: () -> Unit, viewModel: AntiCheatStatusViewModel = hiltViewModel()) {
    val caps by viewModel.capabilities.collectAsStateWithLifecycle()
    // El usuario sale a Ajustes y vuelve: se comprueba de nuevo al reanudar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val context = androidx.compose.ui.platform.LocalContext.current
    AntiCheatStatusContent(
        capabilities = caps,
        onBack = onBack,
        onGrantOverlay = { context.launch(viewModel.overlayIntent()) },
        onGrantAdmin = { context.launch(viewModel.adminIntent()) },
        onReleaseOwner = viewModel::releaseDeviceOwner,
    )
}

private fun Context.launch(intent: Intent) {
    runCatching { startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiCheatStatusContent(
    capabilities: Capabilities,
    onBack: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAdmin: () -> Unit,
    onReleaseOwner: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.anticheat_status_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = ApexTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
        ) {
            CapabilityRow(
                title = R.string.anticheat_cap_overlay,
                description = R.string.anticheat_cap_overlay_desc,
                granted = capabilities.overlay,
                onGrant = onGrantOverlay,
            )
            CapabilityRow(
                title = R.string.anticheat_cap_admin,
                description = R.string.anticheat_cap_admin_desc,
                granted = capabilities.deviceAdmin,
                onGrant = onGrantAdmin,
            )
            // El propietario del dispositivo solo se concede por ADB: no hay botón.
            CapabilityRow(
                title = R.string.anticheat_cap_owner,
                description = R.string.anticheat_cap_owner_desc,
                granted = capabilities.deviceOwner,
                onGrant = null,
                onRelease = onReleaseOwner,
            )
            SectionHeader(stringResource(R.string.anticheat_limits_title))
            Text(
                stringResource(R.string.anticheat_limits_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    title: Int,
    description: Int,
    granted: Boolean,
    onGrant: (() -> Unit)?,
    onRelease: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(if (granted) R.string.anticheat_granted else R.string.anticheat_not_granted),
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            )
        }
        Text(
            stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted && onGrant != null) {
            OutlinedButton(onClick = onGrant, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.anticheat_grant), style = MaterialTheme.typography.labelLarge) }
        }
        if (granted && onRelease != null) {
            OutlinedButton(onClick = onRelease, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.anticheat_release), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Preview(name = "Claro", showBackground = true, heightDp = 800)
@Composable
private fun AntiCheatStatusLight() = ApexAppTheme(darkTheme = false) { AntiCheatStatusContent(Capabilities(overlay = true, deviceAdmin = false, deviceOwner = false), onBack = {}, onGrantOverlay = {}, onGrantAdmin = {}) }

@Preview(name = "Oscuro", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AntiCheatStatusDark() = ApexAppTheme(darkTheme = true) { AntiCheatStatusContent(Capabilities(overlay = true, deviceAdmin = false, deviceOwner = false), onBack = {}, onGrantOverlay = {}, onGrantAdmin = {}) }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 800, fontScale = 1.3f)
@Composable
private fun AntiCheatStatusLargeFont() = ApexAppTheme(darkTheme = false) { AntiCheatStatusContent(Capabilities(overlay = true, deviceAdmin = false, deviceOwner = false), onBack = {}, onGrantOverlay = {}, onGrantAdmin = {}) }
