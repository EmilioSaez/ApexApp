package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.AlarmClockGateway
import app.luxion.apexapp.alarm.AlarmClockScheduler
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmPermissions
import app.luxion.apexapp.alarm.AndroidAlarmPermissions
import app.luxion.apexapp.alarm.AlarmScheduler
import app.luxion.apexapp.alarm.AndroidAlarmClockGateway
import app.luxion.apexapp.alarm.AndroidAlarmNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
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
    fun notifier(impl: AndroidAlarmNotifier): AlarmNotifier

    companion object {
        @Provides
        fun clock(): Clock = Clock.systemDefaultZone()
    }
}
