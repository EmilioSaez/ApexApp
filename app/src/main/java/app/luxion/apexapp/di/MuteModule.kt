package app.luxion.apexapp.di

import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.alarm.mute.ChallengeMuteController
import app.luxion.apexapp.alarm.mute.RingMuter
import app.luxion.apexapp.alarm.sound.AlarmSoundEngine
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.editor.mute.MuteSection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object MuteModule {
    @Provides
    @Singleton
    fun muteController(registry: RingingRegistry, engine: AlarmSoundEngine, clock: Clock): ChallengeMuteController =
        ChallengeMuteController(
            registry,
            RingMuter(engine::setMuted),
            clock,
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )

    @Provides
    @IntoSet
    fun editorSection(impl: MuteSection): EditorSection = impl

    /** Al terminar una sesión se descarta su estado de silencio. */
    @Provides
    @IntoSet
    fun lifecycleListener(controller: ChallengeMuteController): RingingLifecycleListener = controller
}
