package app.luxion.apexapp.di

import app.luxion.apexapp.sharing.SharedFileWriter
import app.luxion.apexapp.sharing.SharedFiles
import app.luxion.apexapp.sharing.StreakStatsSource
import app.luxion.apexapp.sharing.WakeStreakStatsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SharingModule {
    @Binds
    fun streakStats(impl: WakeStreakStatsSource): StreakStatsSource

    @Binds
    fun fileWriter(impl: SharedFiles): SharedFileWriter
}
