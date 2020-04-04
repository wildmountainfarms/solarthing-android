package me.retrodaredevil.solarthing.android.util

enum class TemperatureUnit {
    FAHRENHEIT,
    CELSIUS;
}

fun convertTemperatureCelsius(temperatureCelsius: Float, desiredTemperatureUnit: TemperatureUnit): Float {
    return when(desiredTemperatureUnit){
        TemperatureUnit.FAHRENHEIT -> temperatureCelsius * 1.8f + 32
        TemperatureUnit.CELSIUS -> temperatureCelsius
    }
}
val TemperatureUnit.shortRepresentation: String
    get() {
        return when(this){
            TemperatureUnit.FAHRENHEIT -> "F"
            TemperatureUnit.CELSIUS -> "C"
        }
    }
