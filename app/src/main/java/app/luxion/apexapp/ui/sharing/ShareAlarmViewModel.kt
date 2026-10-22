package app.luxion.apexapp.ui.sharing

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.sharing.AlarmExporter
import app.luxion.apexapp.sharing.QrCodeMatrix
import app.luxion.apexapp.sharing.SharedAlarmCodec
import app.luxion.apexapp.sharing.SharedFiles
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShareUiState(
    val isLoading: Boolean = true,
    val alarm: Alarm? = null,
    val link: String? = null,
    /** Null si el enlace no cabe en un QR escaneable. */
    val qr: BitMatrix? = null,
)

@HiltViewModel
class ShareAlarmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AlarmRepository,
    private val exporter: AlarmExporter,
    private val files: SharedFiles,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState

    init {
        val alarmId: Long = savedStateHandle["alarmId"] ?: 0L
        viewModelScope.launch {
            val alarm = repository.get(alarmId)
            _uiState.value = if (alarm == null) {
                ShareUiState(isLoading = false)
            } else {
                val link = exporter.linkOf(alarm)
                ShareUiState(isLoading = false, alarm = alarm, link = link, qr = QrCodeMatrix.of(link))
            }
        }
    }

    /** URI de lectura del archivo `.apexalarm` de la alarma, listo para la share sheet. */
    suspend fun fileUri(): Uri? {
        val alarm = _uiState.value.alarm ?: return null
        return files.write("alarma.${SharedAlarmCodec.FILE_EXTENSION}", exporter.fileBytesOf(alarm))
    }
}
