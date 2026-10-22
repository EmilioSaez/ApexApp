package app.luxion.apexapp

import android.app.Application
import app.luxion.apexapp.anticheat.LockdownController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class ApexApplication : Application() {
    @Inject lateinit var lockdown: Provider<LockdownController>

    override fun onCreate() {
        super.onCreate()
        // Una sesión de sonado viva al arrancar el proceso significa que se interrumpió (muerte o reinicio).
        lockdown.get().checkInterrupted()
    }
}
