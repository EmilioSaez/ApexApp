package app.luxion.apexapp.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import app.luxion.apexapp.domain.challenge.sensor.AccelSample
import app.luxion.apexapp.domain.challenge.sensor.MotionSample
import app.luxion.apexapp.domain.challenge.sensor.MotionSensors
import app.luxion.apexapp.domain.challenge.sensor.StepReading
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** Adaptador de `SensorManager`: registra los listeners solo mientras alguien colecciona el flujo. */
class AndroidMotionSensors @Inject constructor(
    @ApplicationContext context: Context,
) : MotionSensors {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun linearAcceleration(): Flow<AccelSample> = sensorFlow(Sensor.TYPE_LINEAR_ACCELERATION) { e ->
        AccelSample(e.timestamp, e.values[0], e.values[1], e.values[2])
    }

    override fun motion(): Flow<MotionSample> = callbackFlow {
        val linear = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (linear == null || gravity == null) {
            close()
            return@callbackFlow
        }
        var g = floatArrayOf(0f, 0f, 9.81f)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                    g = event.values.copyOf()
                    return
                }
                val norm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).coerceAtLeast(1e-3f)
                val v = event.values
                val vertical = (v[0] * g[0] + v[1] * g[1] + v[2] * g[2]) / norm
                trySend(MotionSample(AccelSample(event.timestamp, v[0], v[1], v[2]), vertical))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        manager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_GAME)
        manager.registerListener(listener, linear, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }

    override fun stepCounter(): Flow<StepReading> = sensorFlow(Sensor.TYPE_STEP_COUNTER) { e ->
        StepReading(e.timestamp, e.values[0])
    }

    private fun <T> sensorFlow(type: Int, map: (SensorEvent) -> T): Flow<T> {
        val sensor = manager.getDefaultSensor(type) ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(map(event))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            awaitClose { manager.unregisterListener(listener) }
        }
    }
}
