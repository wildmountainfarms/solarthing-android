package me.retrodaredevil.solarthing.android

import android.content.ComponentName
import kotlin.reflect.KClass

object WearConstants {
    /**
     * [ComponentName]s used for ComplicationProviderServices
     */
    val PROVIDER_COMPONENT_NAMES = listOf(
            createFromClass(BatteryVoltageComplicationProviderService::class),
            createFromClass(BatteryTemperatureComplicationProviderService::class),
    )

    private fun createFromClass(clazz: KClass<*>): ComponentName {
        return ComponentName(clazz.java.`package`!!.name, clazz.java.name)
    }
}
