package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.AlarmClockGateway
import app.luxion.apexapp.alarm.AlarmClockScheduler
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmPermissions
import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.AlarmRinger
import app.luxion.apexapp.alarm.AlarmScheduler
import app.luxion.apexapp.alarm.AndroidAlarmClockGateway
import app.luxion.apexapp.alarm.AndroidAlarmNotifier
import app.luxion.apexapp.alarm.SessionIdGenerator
import app.luxion.apexapp.alarm.UuidSessionIdGenerator
import app.luxion.apexapp.ui.ring.DefaultDismissGate
import app.luxion.apexapp.ui.ring.DismissGate
import app.luxion.apexapp.alarm.AndroidAlarmPermissions
import app.luxion.apexapp.alarm.AndroidDeviceProbe
import app.luxion.apexapp.alarm.AndroidRingScreenLauncher
import app.luxion.apexapp.alarm.AndroidRingingLauncher
import app.luxion.apexapp.alarm.DataStoreManufacturerGuideStore
import app.luxion.apexapp.alarm.DeviceProbe
import app.luxion.apexapp.alarm.ManufacturerGuideStore
import app.luxion.apexapp.alarm.NoOpWakeEventRecorder
import app.luxion.apexapp.alarm.RingScreenLauncher
import app.luxion.apexapp.alarm.RingingController
import app.luxion.apexapp.alarm.RingingLauncher
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.SystemToneRinger
import app.luxion.apexapp.domain.WakeEventRecorder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
interface AlarmModule {
    @Binds
    fun scheduler(impl: AlarmClockScheduler): AlarmScheduler

    @Binds
    fun gateway(impl: AndroidAlarmClockGateway): AlarmClockGateway

    @Binds
    fun permissions(impl: AndroidAlarmPermissions): AlarmPermissions

    @Binds
    fun sessionIds(impl: UuidSessionIdGenerator): SessionIdGenerator

    @Binds
    fun dismissGate(impl: DefaultDismissGate): DismissGate

    @Binds
    fun notifier(impl: AndroidAlarmNotifier): AlarmNotifier

    /** Binding por defecto del audio; alarm-sound lo sustituye. */
    @Binds
    fun ringer(impl: SystemToneRinger): AlarmRinger

    /** Pantalla de sonar por defecto; alarm-core la sustituye por su actividad. */
    @Binds
    fun ringScreenLauncher(impl: AndroidRingScreenLauncher): RingScreenLauncher

    @Binds
    fun ringingLauncher(impl: AndroidRingingLauncher): RingingLauncher

    @Binds
    fun resolver(impl: RingingController): AlarmResolver

    @Binds
    fun deviceProbe(impl: AndroidDeviceProbe): DeviceProbe

    @Binds
    fun manufacturerGuideStore(impl: DataStoreManufacturerGuideStore): ManufacturerGuideStore

    /** Sin implementación real hasta que wake-stats la aporte. */
    @Binds
    fun wakeEventRecorder(impl: NoOpWakeEventRecorder): WakeEventRecorder

    /** Observadores del ciclo de vida del sonado (anti-cheat, etc.); vacío por defecto. */
    @Multibinds
    fun lifecycleListeners(): Set<RingingLifecycleListener>

    companion object {
        @Provides
        fun clock(): Clock = Clock.systemDefaultZone()
    }
}
