package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.AlarmFiringHandler
import app.luxion.apexapp.alarm.AndroidAwakeVerificationGateway
import app.luxion.apexapp.alarm.AndroidAwakeVerificationNotifier
import app.luxion.apexapp.alarm.AwakeVerificationCanceller
import app.luxion.apexapp.alarm.AwakeVerificationGateway
import app.luxion.apexapp.alarm.AwakeVerificationListener
import app.luxion.apexapp.alarm.AwakeVerificationNotifier
import app.luxion.apexapp.alarm.AwakeVerificationStore
import app.luxion.apexapp.alarm.AwakeVerifier
import app.luxion.apexapp.alarm.PrefsAwakeVerificationStore
import app.luxion.apexapp.alarm.RingAgain
import app.luxion.apexapp.alarm.RingingLifecycleListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface AwakeVerificationModule {
    @Binds fun store(impl: PrefsAwakeVerificationStore): AwakeVerificationStore

    @Binds fun gateway(impl: AndroidAwakeVerificationGateway): AwakeVerificationGateway

    @Binds fun notifier(impl: AndroidAwakeVerificationNotifier): AwakeVerificationNotifier

    @Binds fun ringAgain(impl: AlarmFiringHandler): RingAgain

    @Binds fun canceller(impl: AwakeVerifier): AwakeVerificationCanceller

    @Binds @IntoSet fun lifecycleListener(impl: AwakeVerificationListener): RingingLifecycleListener
}
