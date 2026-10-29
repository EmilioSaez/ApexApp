package app.luxion.apexapp.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.luxion.apexapp.domain.AppIconSwitcher
import app.luxion.apexapp.domain.AppIconVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Alterna los `activity-alias` `.icon.<variante>` con `DONT_KILL_APP`. Se habilita primero el nuevo y luego se
 * deshabilitan los demás, para que siempre haya un acceso al launcher. El launcher puede tardar en refrescar y
 * algunos cierran la app al cambiar de alias.
 */
class AndroidAppIconSwitcher @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppIconSwitcher {

    override fun apply(variant: AppIconVariant) {
        setState(variant, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        AppIconVariant.entries.filter { it != variant }
            .forEach { setState(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
    }

    private fun setState(variant: AppIconVariant, state: Int) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, "app.luxion.apexapp.icon.${variant.name}"),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }
}
