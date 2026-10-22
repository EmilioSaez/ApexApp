package app.luxion.apexapp.alarm

import javax.inject.Inject

enum class IssueSeverity { Critical, Recommended }

enum class ReliabilityIssue(val severity: IssueSeverity) {
    ExactAlarmDenied(IssueSeverity.Critical),
    NotificationsDisabled(IssueSeverity.Critical),
    RingingChannelMuted(IssueSeverity.Critical),
    FullScreenIntentDenied(IssueSeverity.Recommended),
    BatteryOptimized(IssueSeverity.Recommended),
    ManufacturerSetup(IssueSeverity.Recommended),
}

data class ReliabilityState(val issues: Set<ReliabilityIssue> = emptySet()) {
    val critical: Set<ReliabilityIssue> get() = issues.filterTo(mutableSetOf()) { it.severity == IssueSeverity.Critical }
    val recommended: Set<ReliabilityIssue> get() = issues - critical

    /** Fiable: no falta nada. Con solo recomendaciones pendientes el estado es "mejorable". */
    val isReliable: Boolean get() = issues.isEmpty()
    val hasCritical: Boolean get() = critical.isNotEmpty()
}

/** Lo que se puede preguntar al dispositivo, tras una interfaz para testear sin Android. */
interface DeviceProbe {
    val sdkInt: Int
    val manufacturer: String

    fun exactAlarmsAllowed(): Boolean
    fun notificationsEnabled(): Boolean
    fun ringingChannelMuted(): Boolean
    fun fullScreenIntentAllowed(): Boolean
    fun ignoringBatteryOptimizations(): Boolean
}

/** Persistencia de "ya completé los pasos del fabricante": el sistema no permite verificarlo. */
interface ManufacturerGuideStore {
    suspend fun isConfirmed(): Boolean
    suspend fun setConfirmed(confirmed: Boolean)
}

class ReliabilityChecker @Inject constructor(
    private val probe: DeviceProbe,
    private val guideStore: ManufacturerGuideStore,
) {
    suspend fun check(): ReliabilityState {
        val sdk = probe.sdkInt
        val issues = buildSet {
            // Antes de API 31 las alarmas exactas no necesitan permiso.
            if (sdk >= 31 && !probe.exactAlarmsAllowed()) add(ReliabilityIssue.ExactAlarmDenied)
            if (!probe.notificationsEnabled()) add(ReliabilityIssue.NotificationsDisabled)
            if (probe.ringingChannelMuted()) add(ReliabilityIssue.RingingChannelMuted)
            // El permiso de pantalla completa es revocable solo desde API 34.
            if (sdk >= 34 && !probe.fullScreenIntentAllowed()) add(ReliabilityIssue.FullScreenIntentDenied)
            if (!probe.ignoringBatteryOptimizations()) add(ReliabilityIssue.BatteryOptimized)
            if (ManufacturerGuides.forManufacturer(probe.manufacturer) != null && !guideStore.isConfirmed()) {
                add(ReliabilityIssue.ManufacturerSetup)
            }
        }
        return ReliabilityState(issues)
    }
}
