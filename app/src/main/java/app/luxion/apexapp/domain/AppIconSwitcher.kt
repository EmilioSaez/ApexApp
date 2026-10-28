package app.luxion.apexapp.domain

/** Cambia el icono del launcher; la implementación de Android habilita el alias de la variante y deshabilita el resto. */
interface AppIconSwitcher {
    fun apply(variant: AppIconVariant)
}
