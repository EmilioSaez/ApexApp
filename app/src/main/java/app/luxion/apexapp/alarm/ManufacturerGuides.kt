package app.luxion.apexapp.alarm

import android.content.ComponentName
import android.content.Intent
import androidx.annotation.StringRes
import app.luxion.apexapp.R

/**
 * Guía de ajustes de un fabricante con ahorro de batería o autoarranque agresivo. Los componentes
 * cambian entre versiones de ROM: se prueban en orden y, si ninguno abre, se usan los ajustes de la app.
 */
data class ManufacturerGuide(
    val id: String,
    @StringRes val brandRes: Int,
    @StringRes val stepsRes: Int,
    val settingsComponents: List<ComponentName>,
) {
    fun intents(): List<Intent> = settingsComponents.map { Intent().setComponent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
}

object ManufacturerGuides {
    private val guides = listOf(
        ManufacturerGuide(
            "xiaomi", R.string.guide_brand_xiaomi, R.string.guide_steps_xiaomi,
            listOf(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        ),
        ManufacturerGuide(
            "huawei", R.string.guide_brand_huawei, R.string.guide_steps_huawei,
            listOf(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            ),
        ),
        ManufacturerGuide(
            "oppo", R.string.guide_brand_oppo, R.string.guide_steps_oppo,
            listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ),
        ),
        ManufacturerGuide(
            "vivo", R.string.guide_brand_vivo, R.string.guide_steps_vivo,
            listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ),
        ),
        ManufacturerGuide(
            "samsung", R.string.guide_brand_samsung, R.string.guide_steps_samsung,
            listOf(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
        ),
        ManufacturerGuide(
            "oneplus", R.string.guide_brand_oneplus, R.string.guide_steps_oneplus,
            listOf(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")),
        ),
    )

    private val aliases = mapOf(
        "xiaomi" to "xiaomi", "redmi" to "xiaomi", "poco" to "xiaomi",
        "huawei" to "huawei", "honor" to "huawei",
        "oppo" to "oppo", "realme" to "oppo",
        "vivo" to "vivo", "iqoo" to "vivo",
        "samsung" to "samsung",
        "oneplus" to "oneplus",
    )

    fun forManufacturer(manufacturer: String): ManufacturerGuide? {
        val name = manufacturer.lowercase()
        val id = aliases.entries.firstOrNull { name.contains(it.key) }?.value ?: return null
        return guides.first { it.id == id }
    }
}
