package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.AlarmResolver
import android.content.Context
import app.luxion.apexapp.alarm.RingScreenLauncher
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.anticheat.ActiveSessionStore
import app.luxion.apexapp.anticheat.AlarmVolumeGateway
import app.luxion.apexapp.anticheat.AndroidAlarmVolumeGateway
import app.luxion.apexapp.anticheat.AndroidCapabilityChecker
import app.luxion.apexapp.anticheat.AndroidLockTaskPolicy
import app.luxion.apexapp.anticheat.AndroidSnoozeScheduler
import app.luxion.apexapp.anticheat.CapabilityChecker
import app.luxion.apexapp.anticheat.LockTaskPolicy
import app.luxion.apexapp.anticheat.LockdownController
import app.luxion.apexapp.anticheat.PrefsActiveSessionStore
import app.luxion.apexapp.anticheat.PrefsSnoozeCounts
import app.luxion.apexapp.anticheat.SessionIntegrityTracker
import app.luxion.apexapp.anticheat.SnoozeController
import app.luxion.apexapp.anticheat.SnoozeCounts
import app.luxion.apexapp.anticheat.SnoozeScheduler
import app.luxion.apexapp.anticheat.VolumeLock
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.ui.anticheat.AntiCheatEditorSection
import app.luxion.apexapp.ui.editor.EditorSection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
interface AntiCheatBindings {
    @Binds fun capabilities(impl: AndroidCapabilityChecker): CapabilityChecker
    @Binds fun volumeGateway(impl: AndroidAlarmVolumeGateway): AlarmVolumeGateway
    @Binds fun lockTaskPolicy(impl: AndroidLockTaskPolicy): LockTaskPolicy
    @Binds fun activeSessions(impl: PrefsActiveSessionStore): ActiveSessionStore
    @Binds fun snoozeCounts(impl: PrefsSnoozeCounts): SnoozeCounts
    @Binds fun snoozeScheduler(impl: AndroidSnoozeScheduler): SnoozeScheduler

    @Binds @IntoSet fun editorSection(impl: AntiCheatEditorSection): EditorSection
}

@Module
@InstallIn(SingletonComponent::class)
object AntiCheatProviders {
    @Provides @Singleton
    fun tracker() = SessionIntegrityTracker()

    @Provides @Singleton
    fun volumeLock(gateway: AlarmVolumeGateway, tracker: SessionIntegrityTracker) =
        VolumeLock(gateway, onTamper = tracker::volumeReset)

    @Provides @Singleton
    fun snoozeController(
        resolver: AlarmResolver,
        scheduler: SnoozeScheduler,
        counts: SnoozeCounts,
        recorder: WakeEventRecorder,
        clock: Clock,
    ) = SnoozeController(resolver, scheduler, counts, recorder, clock)

    @Provides @Singleton
    fun lockdown(
        capabilities: CapabilityChecker,
        volume: VolumeLock,
        tracker: SessionIntegrityTracker,
        lockTaskPolicy: LockTaskPolicy,
        activeSessions: ActiveSessionStore,
        snoozeCounts: SnoozeCounts,
        recorder: WakeEventRecorder,
        clock: Clock,
        @ApplicationContext context: Context,
        screenLauncher: RingScreenLauncher,
    ) = LockdownController(
        capabilities, volume, tracker, lockTaskPolicy, activeSessions, snoozeCounts, recorder, clock,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        showRingScreen = { context.startActivity(screenLauncher.intentFor(it)) },
    )

    @Provides @IntoSet
    fun lifecycleListener(controller: LockdownController): RingingLifecycleListener = controller
}
