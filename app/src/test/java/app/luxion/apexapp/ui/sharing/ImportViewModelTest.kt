package app.luxion.apexapp.ui.sharing

import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.sharing.AlarmImporter
import app.luxion.apexapp.sharing.ImportError
import app.luxion.apexapp.sharing.ImportInput
import app.luxion.apexapp.sharing.PendingImport
import app.luxion.apexapp.sharing.SharedAlarmCodec
import app.luxion.apexapp.sharing.toShared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    private val alarms = FakeAlarmRepository()
    private val pending = PendingImport()
    private val noSounds = object : SoundRepository {
        override fun observeAll() = flowOf(emptyList<Sound>())
        override suspend fun get(id: Long): Sound? = null
        override suspend fun save(sound: Sound): Long = error("unused")
        override suspend fun delete(id: Long) = error("unused")
    }
    private lateinit var viewModel: ImportViewModel

    private val link = SharedAlarmCodec.toLink(Alarm(hour = 8, minute = 5, label = "Clase").toShared(null))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ImportViewModel(AlarmImporter(ChallengeRegistry(listOf(MathChallenge())), alarms, noSounds), pending)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsAtEntry_andPreviewDoesNotSaveAnything() = runTest {
        assertEquals(ImportUiState.Entry, viewModel.uiState.value)
        viewModel.onLink(link)
        assertTrue(viewModel.uiState.value is ImportUiState.Preview)
        assertTrue(alarms.observeAll().first().isEmpty())
    }

    @Test
    fun discard_returnsToEntryWithoutSaving() = runTest {
        viewModel.onLink(link)
        viewModel.reset()
        assertEquals(ImportUiState.Entry, viewModel.uiState.value)
        assertTrue(alarms.observeAll().first().isEmpty())
    }

    @Test
    fun accept_savesOneDisabledAlarm_evenIfPressedTwice() = runTest {
        viewModel.onLink(link)
        viewModel.accept()
        viewModel.accept()
        assertEquals(ImportUiState.Imported, viewModel.uiState.value)
        val saved = alarms.observeAll().first().single()
        assertEquals("Clase", saved.label)
        assertEquals(false, saved.enabled)
    }

    @Test
    fun invalidContent_failsWithoutSaving() = runTest {
        viewModel.onLink("apexapp://alarm?d=***")
        assertEquals(ImportUiState.Failed(ImportError.MALFORMED), viewModel.uiState.value)
        viewModel.onFileUnreadable()
        assertEquals(ImportUiState.Failed(null), viewModel.uiState.value)
        assertTrue(alarms.observeAll().first().isEmpty())
    }

    @Test
    fun pendingContentFromAnIntent_isShownAndConsumed() = runTest {
        pending.offer(ImportInput.Link(link))
        assertTrue(viewModel.uiState.value is ImportUiState.Preview)
        assertNull(pending.content.value)
    }
}
