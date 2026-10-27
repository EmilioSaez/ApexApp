package app.luxion.apexapp.domain.challenge

/** Hardware y permisos del dispositivo que condicionan los retos de sensores. */
interface DeviceCapabilities {
    val hasAccelerometer: Boolean
    val hasStepSensor: Boolean
    val hasActivityRecognitionPermission: Boolean
}
