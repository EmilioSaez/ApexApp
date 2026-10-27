package app.luxion.apexapp.alarm

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDeviceProbe(
    override var sdkInt: Int = 34,
    override var manufacturer: String = "Google",
    var exact: Boolean = true,
    var notifications: Boolean = true,
    var channelMuted: Boolean = false,
    var fullScreen: Boolean = true,
    var batteryIgnored: Boolean = true,
) : DeviceProbe {
    override fun exactAlarmsAllowed() = exact
    override fun notificationsEnabled() = notifications
    override fun ringingChannelMuted() = channelMuted
    override fun fullScreenIntentAllowed() = fullScreen
    override fun ignoringBatteryOptimizations() = batteryIgnored
}

class FakeGuideStore(var confirmed: Boolean = false) : ManufacturerGuideStore {
    override suspend fun isConfirmed() = confirmed
    override suspend fun setConfirmed(confirmed: Boolean) {
        this.confirmed = confirmed
    }
}

class ReliabilityCheckerTest {
    private val probe = FakeDeviceProbe()
    private val store = FakeGuideStore()
    private val checker = ReliabilityChecker(probe, store)

    @Test
    fun everythingGranted_isReliable() = runBlocking {
        assertTrue(checker.check().isReliable)
    }

    @Test
    fun missingExactOrNotifications_isCritical() = runBlocking {
        probe.exact = false
        probe.notifications = false
        val state = checker.check()
        assertTrue(state.hasCritical)
        assertEquals(setOf(ReliabilityIssue.ExactAlarmDenied, ReliabilityIssue.NotificationsDisabled), state.critical)
    }

    @Test
    fun mutedChannel_isCritical() = runBlocking {
        probe.channelMuted = true
        assertEquals(setOf(ReliabilityIssue.RingingChannelMuted), checker.check().critical)
    }

    @Test
    fun onlyBatteryOrFullScreen_isRecommendedNotCritical() = runBlocking {
        probe.batteryIgnored = false
        probe.fullScreen = false
        val state = checker.check()
        assertFalse(state.isReliable)
        assertFalse(state.hasCritical)
        assertEquals(setOf(ReliabilityIssue.BatteryOptimized, ReliabilityIssue.FullScreenIntentDenied), state.recommended)
    }

    @Test
    fun exactAlarmPermission_isOnlyCheckedFromApi31() = runBlocking {
        probe.sdkInt = 30
        probe.exact = false
        assertTrue(checker.check().isReliable)
        probe.sdkInt = 31
        assertEquals(setOf(ReliabilityIssue.ExactAlarmDenied), checker.check().issues)
    }

    @Test
    fun fullScreenPermission_isOnlyCheckedFromApi34() = runBlocking {
        probe.fullScreen = false
        probe.sdkInt = 33
        assertTrue(checker.check().isReliable)
        probe.sdkInt = 34
        assertEquals(setOf(ReliabilityIssue.FullScreenIntentDenied), checker.check().issues)
    }

    @Test
    fun manufacturerGuide_pendingUntilConfirmed() = runBlocking {
        probe.manufacturer = "Xiaomi"
        assertEquals(setOf(ReliabilityIssue.ManufacturerSetup), checker.check().issues)
        store.setConfirmed(true)
        assertTrue(checker.check().isReliable)
    }

    @Test
    fun manufacturerWithoutKnownRestrictions_hasNoGuide() = runBlocking {
        probe.manufacturer = "Google"
        assertNull(ManufacturerGuides.forManufacturer("Google"))
        assertTrue(checker.check().isReliable)
    }

    @Test
    fun manufacturerAliases_mapToGuides() {
        assertEquals("xiaomi", ManufacturerGuides.forManufacturer("Redmi")!!.id)
        assertEquals("oppo", ManufacturerGuides.forManufacturer("realme")!!.id)
        assertEquals("huawei", ManufacturerGuides.forManufacturer("HONOR")!!.id)
        assertNotNull(ManufacturerGuides.forManufacturer("samsung"))
    }
}
