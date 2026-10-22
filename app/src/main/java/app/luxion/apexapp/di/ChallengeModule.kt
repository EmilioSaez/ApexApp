package app.luxion.apexapp.di

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeEventSink
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.FixedDifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.NoOpChallengeEventSink
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface ChallengeModule {
    /** Otros changes añaden sus tipos con su propio `@Binds @IntoSet Challenge`. */
    @Binds
    @IntoSet
    fun math(impl: MathChallenge): Challenge

    @Binds
    fun difficultyProvider(impl: FixedDifficultyProvider): DifficultyProvider

    @Binds
    fun eventSink(impl: NoOpChallengeEventSink): ChallengeEventSink

    companion object {
        @Provides
        fun registry(challenges: @JvmSuppressWildcards Set<Challenge>): ChallengeRegistry =
            ChallengeRegistry(challenges)
    }
}
