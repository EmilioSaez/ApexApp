package app.luxion.apexapp.di

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeEventSink
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.FixedDifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.NoOpChallengeEventSink
import app.luxion.apexapp.ui.challenge.ChallengeParamsEditor
import app.luxion.apexapp.ui.challenge.ChallengeRenderer
import app.luxion.apexapp.ui.challenge.ChallengesSection
import app.luxion.apexapp.ui.challenge.MathRenderer
import app.luxion.apexapp.ui.editor.EditorSection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
interface ChallengeModule {
    /** Otros changes añaden sus tipos con su propio `@Binds @IntoSet Challenge`. */
    @Binds
    @IntoSet
    fun math(impl: MathChallenge): Challenge

    /** Cada tipo registra también su UI con `@Binds @IntoSet ChallengeRenderer`. */
    @Binds
    @IntoSet
    fun mathRenderer(impl: MathRenderer): ChallengeRenderer

    /** Sección «Retos» del editor de alarmas. */
    @Binds
    @IntoSet
    fun challengesSection(impl: ChallengesSection): EditorSection

    /** Declarados aquí para que los conjuntos existan aunque ningún módulo aporte elementos. */
    @Multibinds
    fun editorSections(): Set<EditorSection>

    @Multibinds
    fun paramsEditors(): Set<ChallengeParamsEditor>

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
