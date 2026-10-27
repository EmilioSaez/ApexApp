package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.AlarmRinger
import app.luxion.apexapp.alarm.sound.AlarmSoundEngine
import app.luxion.apexapp.alarm.sound.AlarmSpeaker
import app.luxion.apexapp.alarm.sound.AlarmVibrator
import app.luxion.apexapp.alarm.sound.AndroidAlarmSpeaker
import app.luxion.apexapp.alarm.sound.AndroidAlarmVibrator
import app.luxion.apexapp.alarm.sound.AndroidTimeAnnouncer
import app.luxion.apexapp.alarm.sound.AndroidTonePlayer
import app.luxion.apexapp.alarm.sound.SoundAlarmRinger
import app.luxion.apexapp.alarm.sound.TimeAnnouncer
import app.luxion.apexapp.alarm.sound.TonePlayer
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.editor.sound.AndroidSoundImporter
import app.luxion.apexapp.ui.editor.sound.SoundImporter
import app.luxion.apexapp.ui.editor.sound.SoundSection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
interface SoundBindings {
    /** Sustituye al binding por defecto (`SystemToneRinger`) del audio del sonado. */
    @Binds
    fun ringer(impl: SoundAlarmRinger): AlarmRinger

    @Binds
    fun tonePlayer(impl: AndroidTonePlayer): TonePlayer

    @Binds
    fun vibrator(impl: AndroidAlarmVibrator): AlarmVibrator

    @Binds
    fun speaker(impl: AndroidAlarmSpeaker): AlarmSpeaker

    @Binds
    fun announcer(impl: AndroidTimeAnnouncer): TimeAnnouncer

    @Binds
    fun importer(impl: AndroidSoundImporter): SoundImporter

    @Binds
    @IntoSet
    fun soundSection(impl: SoundSection): EditorSection
}

@Module
@InstallIn(SingletonComponent::class)
object SoundModule {
    /** Un solo motor por proceso: una alarma sonando (o una previsualización) a la vez. */
    @Provides
    @Singleton
    fun engine(
        sounds: SoundRepository,
        tone: TonePlayer,
        vibrator: AlarmVibrator,
        speaker: AlarmSpeaker,
        announcer: TimeAnnouncer,
        clock: Clock,
    ) = AlarmSoundEngine(
        sounds, tone, vibrator, speaker, announcer, clock,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )
}
