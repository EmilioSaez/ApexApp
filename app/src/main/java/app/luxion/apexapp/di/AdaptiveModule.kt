package app.luxion.apexapp.di

import app.luxion.apexapp.domain.adaptive.WakeHistorySource
import app.luxion.apexapp.domain.adaptive.WakeStatsHistorySource
import app.luxion.apexapp.ui.adaptive.AdaptiveEditorSection
import app.luxion.apexapp.ui.editor.EditorSection
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface AdaptiveModule {
    @Binds
    fun wakeHistorySource(impl: WakeStatsHistorySource): WakeHistorySource

    @Binds
    @IntoSet
    fun editorSection(impl: AdaptiveEditorSection): EditorSection
}
