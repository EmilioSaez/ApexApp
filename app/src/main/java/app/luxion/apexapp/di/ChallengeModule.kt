package app.luxion.apexapp.di

import app.luxion.apexapp.domain.adaptive.AdaptiveDifficultyProvider
import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeEventSink
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.SessionFinishedListener
import app.luxion.apexapp.domain.challenge.WakeLoggingChallengeSink
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.ResolvedWakeListener
import app.luxion.apexapp.ui.challenge.ChallengeParamsEditor
import app.luxion.apexapp.ui.challenge.ChallengeRenderer
import app.luxion.apexapp.ui.challenge.ChallengesSection
import app.luxion.apexapp.ui.challenge.MathRenderer
import app.luxion.apexapp.ui.challenge.FlappyParamsEditor
import app.luxion.apexapp.ui.challenge.MemoryParamsEditor
import app.luxion.apexapp.ui.challenge.MemoryRenderer
import app.luxion.apexapp.domain.challenge.memory.MemoryChallenge
import app.luxion.apexapp.ui.challenge.QrParamsEditor
import app.luxion.apexapp.ui.challenge.ShakeParamsEditor
import app.luxion.apexapp.ui.challenge.StepsParamsEditor
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.challenge.FlappyRenderer
import app.luxion.apexapp.ui.challenge.QrRenderer
import app.luxion.apexapp.ui.challenge.ShakeRenderer
import app.luxion.apexapp.ui.challenge.StepsRenderer
import app.luxion.apexapp.domain.challenge.holdrelease.HoldReleaseChallenge
import app.luxion.apexapp.domain.challenge.flappy.FlappyChallenge
import app.luxion.apexapp.domain.challenge.qr.QrChallenge
import app.luxion.apexapp.ui.challenge.HoldReleaseParamsEditor
import app.luxion.apexapp.ui.challenge.HoldReleaseRenderer
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import app.luxion.apexapp.domain.challenge.sensor.MotionSensors
import app.luxion.apexapp.domain.challenge.sensor.ShakeChallenge
import app.luxion.apexapp.domain.challenge.sensor.StepsChallenge
import app.luxion.apexapp.platform.AndroidDeviceCapabilities
import app.luxion.apexapp.platform.AndroidMotionSensors
import app.luxion.apexapp.domain.challenge.PassedAlarmsReset
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

    /** Oyentes del fin de sesión: `@Binds @IntoSet SessionFinishedListener`. */
    @Multibinds
    fun sessionFinishedListeners(): Set<SessionFinishedListener>

    /** Cada sonado empieza y termina sin la alarma marcada como superada. */
    @Binds
    @IntoSet
    fun passedAlarmsReset(impl: PassedAlarmsReset): RingingLifecycleListener

    @Binds
    @IntoSet
    fun shake(impl: ShakeChallenge): Challenge

    @Binds
    @IntoSet
    fun steps(impl: StepsChallenge): Challenge

    @Binds
    @IntoSet
    fun qr(impl: QrChallenge): Challenge

    @Binds
    @IntoSet
    fun flappy(impl: FlappyChallenge): Challenge

    @Binds
    @IntoSet
    fun shakeRenderer(impl: ShakeRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun stepsRenderer(impl: StepsRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun qrRenderer(impl: QrRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun flappyRenderer(impl: FlappyRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun shakeEditor(impl: ShakeParamsEditor): ChallengeParamsEditor

    @Binds
    @IntoSet
    fun stepsEditor(impl: StepsParamsEditor): ChallengeParamsEditor

    @Binds
    @IntoSet
    fun qrEditor(impl: QrParamsEditor): ChallengeParamsEditor

    @Binds
    @IntoSet
    fun memory(impl: MemoryChallenge): Challenge

    @Binds
    @IntoSet
    fun memoryRenderer(impl: MemoryRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun memoryEditor(impl: MemoryParamsEditor): ChallengeParamsEditor

    @Binds
    @IntoSet
    fun flappyEditor(impl: FlappyParamsEditor): ChallengeParamsEditor

    @Binds
    @IntoSet
    fun holdRelease(impl: HoldReleaseChallenge): Challenge

    @Binds
    @IntoSet
    fun holdReleaseRenderer(impl: HoldReleaseRenderer): ChallengeRenderer

    @Binds
    @IntoSet
    fun holdReleaseEditor(impl: HoldReleaseParamsEditor): ChallengeParamsEditor

    @Binds
    fun deviceCapabilities(impl: AndroidDeviceCapabilities): DeviceCapabilities

    @Binds
    fun motionSensors(impl: AndroidMotionSensors): MotionSensors

    @Binds
    fun difficultyProvider(impl: AdaptiveDifficultyProvider): DifficultyProvider

    /** Sink real: registra fallos de reto y fin de sesión en el historial de despertar. */
    @Binds
    fun eventSink(impl: WakeLoggingChallengeSink): ChallengeEventSink

    /** Registra COMPLETED al resolverse una alarma (cubre las que no tienen retos). */
    @Binds
    @IntoSet
    fun resolvedWakeListener(impl: ResolvedWakeListener): RingingLifecycleListener

    companion object {
        @Provides
        fun registry(challenges: @JvmSuppressWildcards Set<Challenge>): ChallengeRegistry =
            ChallengeRegistry(challenges)
    }
}
