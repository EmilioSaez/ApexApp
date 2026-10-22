package app.luxion.apexapp.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Consulta en cada llamada: el usuario puede conceder o revocar permisos en cualquier momento. */
class AndroidDeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceCapabilities {
    private val sensors get() = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val hasAccelerometer: Boolean
        get() = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null

    override val hasStepSensor: Boolean
        get() = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    override val hasCamera: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    override val hasActivityRecognitionPermission: Boolean
        get() = granted(Manifest.permission.ACTIVITY_RECOGNITION)

    override val hasCameraPermission: Boolean
        get() = granted(Manifest.permission.CAMERA)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
