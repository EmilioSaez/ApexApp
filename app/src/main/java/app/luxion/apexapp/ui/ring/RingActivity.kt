package app.luxion.apexapp.ui.ring

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.luxion.apexapp.alarm.RingScreenLauncher
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.anticheat.ActiveLockdown
import app.luxion.apexapp.anticheat.LockdownController
import app.luxion.apexapp.anticheat.LockdownWindow
import app.luxion.apexapp.ui.theme.ApexAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Pantalla de sonar: se muestra sobre el bloqueo cuando se dispara una alarma. */
@AndroidEntryPoint
class RingActivity : ComponentActivity() {
    @Inject lateinit var lockdownController: LockdownController
    @Inject lateinit var registry: RingingRegistry
    @Inject lateinit var screenLauncher: RingScreenLauncher

    private val handler = Handler(Looper.getMainLooper())
    private var lockdown: ActiveLockdown? = null
    private val sessionId: String? get() = intent.getStringExtra(RingViewModel.EXTRA_SESSION_ID)

    /** Atrás no hace nada mientras el lockdown está activo; sin él, se comporta como siempre. */
    private val backGuard = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, backGuard)
        setContent {
            ApexAppTheme(darkTheme = isSystemInDarkTheme()) {
                RingScreen(onFinished = ::finish)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                lockdownController.active.collect { active ->
                    lockdown = active?.takeIf { it.sessionId == sessionId }
                    backGuard.isEnabled = lockdown != null
                    LockdownWindow.apply(this@RingActivity, lockdown)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val current = lockdown ?: return
        if (isFinishing) return
        // El usuario salió de la pantalla durante el lockdown: cuenta como intento y se intenta recuperar.
        lockdownController.exitAttempt(current.sessionId)
        if (current.protection.foregroundRecovery) {
            handler.postDelayed({ bringBack(current) }, RECOVERY_DELAY_MILLIS)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (isFinishing) LockdownWindow.apply(this, null)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (LockdownWindow.consumesKey(lockdown, keyCode)) {
            lockdownController.volumeKeyPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun bringBack(current: ActiveLockdown) {
        // Si la sesión terminó entretanto, no hay nada que recuperar.
        val session = registry.get(current.sessionId) ?: return
        runCatching { startActivity(screenLauncher.intentFor(session)) }
    }

    private companion object {
        const val RECOVERY_DELAY_MILLIS = 400L
    }
}
