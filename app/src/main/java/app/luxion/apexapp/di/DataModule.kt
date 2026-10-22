package app.luxion.apexapp.di

import android.content.Context
import androidx.room.Room
import app.luxion.apexapp.data.RoomAlarmRepository
import app.luxion.apexapp.data.RoomSoundRepository
import app.luxion.apexapp.data.local.AlarmDao
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.data.local.MIGRATION_1_2
import app.luxion.apexapp.data.local.SoundDao
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.SoundRepository
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
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun alarmDao(db: ApexDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun soundDao(db: ApexDatabase): SoundDao = db.soundDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun alarmRepository(impl: RoomAlarmRepository): AlarmRepository

    @Binds
    fun soundRepository(impl: RoomSoundRepository): SoundRepository
}
