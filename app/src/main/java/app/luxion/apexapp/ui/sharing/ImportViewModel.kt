package app.luxion.apexapp.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.sharing.AlarmImporter
import app.luxion.apexapp.sharing.ImportError
import app.luxion.apexapp.sharing.ImportInput
import app.luxion.apexapp.sharing.ImportPreview
import app.luxion.apexapp.sharing.PendingImport
import app.luxion.apexapp.sharing.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    /** Nada recibido todavía: escanear, elegir archivo o esperar un enlace. */
    data object Entry : ImportUiState
    data class Preview(val preview: ImportPreview) : ImportUiState
    data class Failed(val error: ImportError?) : ImportUiState
    data object Saving : ImportUiState
    data object Imported : ImportUiState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: AlarmImporter,
    private val pending: PendingImport,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Entry)
    val uiState: StateFlow<ImportUiState> = _uiState

    init {
        viewModelScope.launch {
            pending.content.filterNotNull().collect { input ->
                pending.clear()
                when (input) {
                    is ImportInput.Link -> onLink(input.link)
                    is ImportInput.File -> onFile(input.bytes)
                }
            }
        }
    }

    fun onLink(link: String) = show(importer.previewOf(link))

    fun onFile(bytes: ByteArray) = show(importer.previewOf(bytes))

    fun onFileUnreadable() {
        _uiState.value = ImportUiState.Failed(null)
    }

    private fun show(result: ValidationResult) {
        _uiState.value = when (result) {
            is ValidationResult.Valid -> ImportUiState.Preview(result.preview)
            is ValidationResult.Invalid -> ImportUiState.Failed(result.error)
        }
    }

    fun accept() {
        val preview = (_uiState.value as? ImportUiState.Preview)?.preview ?: return
        // Evita guardar dos veces si se pulsa Importar de nuevo antes de que termine el guardado.
        _uiState.value = ImportUiState.Saving
        viewModelScope.launch {
            importer.import(preview)
            _uiState.value = ImportUiState.Imported
        }
    }

    fun reset() {
        _uiState.value = ImportUiState.Entry
    }
}
