package app.luxion.apexapp.ui.sharing

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.sharing.SharedFileWriter
import app.luxion.apexapp.sharing.StreakStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** [streak] es null mientras carga; con `best == 0` aún no hay logros que compartir. */
data class StreakCardUiState(val streak: Streak? = null) {
    val hasAchievements: Boolean get() = streak != null && streak.best > 0
}

@HiltViewModel
class StreakCardViewModel @Inject constructor(
    stats: StreakStatsSource,
    private val files: SharedFileWriter,
) : ViewModel() {
    val uiState: StateFlow<StreakCardUiState> = stats.observeStreak()
        .map { StreakCardUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakCardUiState())

    /** Guarda la tarjeta como PNG en el directorio compartido y devuelve su URI de lectura. */
    fun imageUri(card: Bitmap): Uri {
        val out = ByteArrayOutputStream()
        card.compress(Bitmap.CompressFormat.PNG, 100, out)
        return files.write("racha.png", out.toByteArray())
    }
}
