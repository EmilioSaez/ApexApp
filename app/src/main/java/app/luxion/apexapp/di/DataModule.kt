package app.luxion.apexapp.di

import android.content.Context
import androidx.room.Room
import app.luxion.apexapp.data.RoomAdaptiveSettingsRepository
import app.luxion.apexapp.data.RoomAlarmRepository
import app.luxion.apexapp.data.RoomFireStateRepository
import app.luxion.apexapp.data.RoomSoundRepository
import app.luxion.apexapp.data.local.AdaptiveSettingsDao
import app.luxion.apexapp.data.local.AlarmDao
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.data.local.FireStateDao
import app.luxion.apexapp.data.local.MIGRATION_1_2
import app.luxion.apexapp.data.local.MIGRATION_2_3
import app.luxion.apexapp.data.local.MIGRATION_3_4
import app.luxion.apexapp.data.local.MIGRATION_4_5
import app.luxion.apexapp.data.local.MIGRATION_7_8
import app.luxion.apexapp.data.local.SleepDao
import app.luxion.apexapp.data.local.SoundDao
import app.luxion.apexapp.data.local.WakeEventDao
import app.luxion.apexapp.data.RoomSleepRepository
import app.luxion.apexapp.data.RoomWakeEventRecorder
import app.luxion.apexapp.data.RoomWakeHistoryRepository
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.FireStateRepository
import app.luxion.apexapp.domain.SleepRepository
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeHistoryRepository
import app.luxion.apexapp.domain.adaptive.AdaptiveSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ApexDatabase =
        Room.databaseBuilder(context, ApexDatabase::class.java, "apex.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_7_8)
            .build()

    @Provides
    fun wakeEventDao(db: ApexDatabase): WakeEventDao = db.wakeEventDao()

    @Provides
    fun sleepDao(db: ApexDatabase): SleepDao = db.sleepDao()

    @Provides
    fun fireStateDao(db: ApexDatabase): FireStateDao = db.fireStateDao()

    @Provides
    fun alarmDao(db: ApexDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun soundDao(db: ApexDatabase): SoundDao = db.soundDao()

    @Provides
    fun adaptiveSettingsDao(db: ApexDatabase): AdaptiveSettingsDao = db.adaptiveSettingsDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun alarmRepository(impl: RoomAlarmRepository): AlarmRepository

    @Binds
    fun soundRepository(impl: RoomSoundRepository): SoundRepository

    @Binds
    fun fireStateRepository(impl: RoomFireStateRepository): FireStateRepository

    @Binds
    fun wakeEventRecorder(impl: RoomWakeEventRecorder): WakeEventRecorder

    @Binds
    fun wakeHistoryRepository(impl: RoomWakeHistoryRepository): WakeHistoryRepository

    @Binds
    fun sleepRepository(impl: RoomSleepRepository): SleepRepository

    @Binds
    fun adaptiveSettingsRepository(impl: RoomAdaptiveSettingsRepository): AdaptiveSettingsRepository
}
