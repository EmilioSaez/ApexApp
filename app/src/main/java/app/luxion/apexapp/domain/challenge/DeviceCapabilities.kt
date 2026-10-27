package app.luxion.apexapp.domain.challenge

/** Hardware y permisos del dispositivo que condicionan los retos de cámara y de sensores. */
interface DeviceCapabilities {
    val hasAccelerometer: Boolean
    val hasStepSensor: Boolean
    val hasCamera: Boolean
    val hasActivityRecognitionPermission: Boolean
    val hasCameraPermission: Boolean
}
