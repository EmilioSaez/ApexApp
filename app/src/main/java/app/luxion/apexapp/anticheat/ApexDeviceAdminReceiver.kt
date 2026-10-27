package app.luxion.apexapp.anticheat

import android.app.admin.DeviceAdminReceiver

/**
 * Administrador de dispositivo sin políticas activas: su única función es impedir la desinstalación
 * directa mientras esté activado. Si la app es propietaria del dispositivo, es también el componente
 * que gestiona el lock task.
 */
class ApexDeviceAdminReceiver : DeviceAdminReceiver()
