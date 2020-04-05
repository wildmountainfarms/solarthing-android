package me.retrodaredevil.solarthing.android.service

import android.app.Service
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.notifications.getBatteryTemperatureId
import me.retrodaredevil.solarthing.android.notifications.getControllerTemperatureId
import me.retrodaredevil.solarthing.android.notifications.getDeviceCpuTemperatureId
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.packets.identification.Identifier
import me.retrodaredevil.solarthing.solar.SolarStatusPacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverStatusPacket

private class NotifyInfo(
    val timeMillis: Long,
    val wasCritical: Boolean
)

class TemperatureNotifyHandler(
    private val service: Service,
    private val solarProfileProvider: ProfileProvider<SolarProfile>,
    private val miscProfileProvider: ProfileProvider<MiscProfile>
) {
    private val lastBatteryOverNotify = HashMap<Identifier, NotifyInfo>()
    private val lastBatteryUnderNotify = HashMap<Identifier, NotifyInfo>()
    private val lastControllerOverNotify = HashMap<Identifier, NotifyInfo>()
    private val lastControllerUnderNotify = HashMap<Identifier, NotifyInfo>()
    private val lastDeviceCpuOverNotify = HashMap<Int?, NotifyInfo>()
    private val lastDeviceCpuUnderNotify = HashMap<Int?, NotifyInfo>()

    private val temperatureNodes: List<TemperatureNode>
        get() = solarProfileProvider.activeProfile.profile.temperatureNodes
    private fun shouldDisplay(temperatureNode: TemperatureNode, lastNotifyInfo: NotifyInfo?): Boolean {
        lastNotifyInfo ?: return true
        if(!lastNotifyInfo.wasCritical && temperatureNode.isCritical){
            return true
        }
        return lastNotifyInfo.timeMillis + DefaultOptions.importantAlertIntervalMillis <= System.currentTimeMillis()
    }
    fun checkBatteryTemperature(dateMillis: Long, device: RoverStatusPacket, temperatureCelsius: Float) {
        for(node in temperatureNodes){
            if(node.battery){
                val temperatureName = "Battery Temperature"
                val deviceName = "Rover with serial: ${device.productSerialNumber}"
                if(node.isOver(temperatureCelsius) && shouldDisplay(node, lastBatteryOverNotify[device.identifier])){
                    lastBatteryOverNotify[device.identifier] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastBatteryUnderNotify.remove(device.identifier)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, true, node.isCritical, getBatteryTemperatureId(device))
                }
                if(node.isUnder(temperatureCelsius) && shouldDisplay(node, lastBatteryUnderNotify[device.identifier])){
                    lastBatteryUnderNotify[device.identifier] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastBatteryOverNotify.remove(device.identifier)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, false, node.isCritical, getBatteryTemperatureId(device))
                }
            }
        }
    }
    fun checkControllerTemperature(dateMillis: Long, device: RoverStatusPacket, temperatureCelsius: Float) {
        for(node in temperatureNodes){
            if(node.controller){
                val temperatureName = "Controller Temperature"
                val deviceName = "Rover with serial: ${device.productSerialNumber}"
                if(node.isOver(temperatureCelsius) && shouldDisplay(node, lastControllerOverNotify[device.identifier])){
                    lastControllerOverNotify[device.identifier] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastControllerUnderNotify.remove(device.identifier)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, true, node.isCritical, getControllerTemperatureId(device))
                }
                if(node.isUnder(temperatureCelsius) && shouldDisplay(node, lastControllerUnderNotify[device.identifier])){
                    lastControllerUnderNotify[device.identifier] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastControllerOverNotify.remove(device.identifier)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, false, node.isCritical, getControllerTemperatureId(device))
                }
            }
        }
    }
    fun checkDeviceCpuTemperature(dateMillis: Long, fragmentId: Int?, temperatureCelsius: Float) {
        for(node in temperatureNodes){
            println("id: $fragmentId ids: ${node.deviceCpuIds}")
            if(node.deviceCpu && (fragmentId in node.deviceCpuIds || node.deviceCpuIds.isEmpty())){
                val temperatureName = "Device CPU Temperature"
                val deviceName = "Device #${fragmentId ?: "Default"}"
                if(node.isOver(temperatureCelsius) && shouldDisplay(node, lastDeviceCpuOverNotify[fragmentId])){
                    lastDeviceCpuOverNotify[fragmentId] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastDeviceCpuUnderNotify.remove(fragmentId)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, true, node.isCritical, getDeviceCpuTemperatureId(fragmentId))
                }
                if(node.isUnder(temperatureCelsius) && shouldDisplay(node, lastDeviceCpuUnderNotify[fragmentId])){
                    lastDeviceCpuUnderNotify[fragmentId] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastDeviceCpuOverNotify.remove(fragmentId)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, false, node.isCritical, getDeviceCpuTemperatureId(fragmentId))
                }
            }
        }
    }
    private fun notify(dateMillis: Long, temperatureName: String, deviceName: String, temperatureCelsius: Float, over: Boolean, critical: Boolean, notificationId: Int) {
        service.getManager().notify(notificationId, NotificationHandler.createTemperatureNotification(service, dateMillis, temperatureName, deviceName, temperatureCelsius, over, critical, miscProfileProvider.activeProfile.profile.temperatureUnit))
    }
}
private fun TemperatureNode.isOver(temperatureCelsius: Float): Boolean {
    return temperatureCelsius + 0.001f >= (this.highThresholdCelsius ?: return false)
}
private fun TemperatureNode.isUnder(temperatureCelsius: Float): Boolean {
    return temperatureCelsius - 0.001f <= (this.lowThresholdCelsius ?: return false)
}
