package app.luxion.apexapp.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.luxion.apexapp.domain.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    private fun repository(file: File) = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )

    @Test
    fun defaultsToSystem() = runTest {
        assertEquals(ThemeMode.SYSTEM, repository(folder.newFile("a.preferences_pb").also { it.delete() }).observeThemeMode().first())
    }

    @Test
    fun persistsSelectedMode() = runTest {
        val repository = repository(File(folder.root, "b.preferences_pb"))
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.observeThemeMode().first())
    }
}
