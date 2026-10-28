package app.luxion.apexapp.domain

import javax.inject.Inject

/** Aplica el icono en el sistema y, solo si eso no falla, guarda la elección. */
class ApplyAppIcon @Inject constructor(
    private val settings: SettingsRepository,
    private val switcher: AppIconSwitcher,
) {
    suspend operator fun invoke(variant: AppIconVariant) {
        switcher.apply(variant)
        settings.setAppIcon(variant)
    }
}
