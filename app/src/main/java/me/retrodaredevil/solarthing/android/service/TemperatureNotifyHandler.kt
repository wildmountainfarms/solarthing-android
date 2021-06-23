package me.retrodaredevil.solarthing.android.service

import android.app.Service
import me.retrodaredevil.solarthing.android.notifications.NotificationHandler
import me.retrodaredevil.solarthing.android.notifications.getBatteryTemperatureId
import me.retrodaredevil.solarthing.android.notifications.getControllerTemperatureId
import me.retrodaredevil.solarthing.android.notifications.getDeviceCpuTemperatureId
import me.retrodaredevil.solarthing.android.prefs.*
import me.retrodaredevil.solarthing.packets.identification.IdentifierFragment

private class NotifyInfo(
        val timeMillis: Long,
        val wasCritical: Boolean
)

class TemperatureNotifyHandler(
        private val service: Service,
        private val solarProfileProvider: ProfileProvider<SolarProfile>,
        private val miscProfileProvider: ProfileProvider<MiscProfile>
) {
    private val lastBatteryOverNotify = HashMap<IdentifierFragment, NotifyInfo>()
    private val lastBatteryUnderNotify = HashMap<IdentifierFragment, NotifyInfo>()
    private val lastControllerOverNotify = HashMap<IdentifierFragment, NotifyInfo>()
    private val lastControllerUnderNotify = HashMap<IdentifierFragment, NotifyInfo>()
    private val lastDeviceCpuOverNotify = HashMap<Int, NotifyInfo>()
    private val lastDeviceCpuUnderNotify = HashMap<Int, NotifyInfo>()

    private val temperatureNodes: List<TemperatureNode>
        get() = solarProfileProvider.activeProfile.profile.temperatureNodes
    private fun shouldDisplay(temperatureNode: TemperatureNode, lastNotifyInfo: NotifyInfo?): Boolean {
        lastNotifyInfo ?: return true
        if(!lastNotifyInfo.wasCritical && temperatureNode.isCritical){
            return true
        }
        return lastNotifyInfo.timeMillis + DefaultOptions.importantAlertIntervalMillis <= System.currentTimeMillis()
    }
    private inline fun checkTemperature(dateMillis: Long, deviceName: String, identifierFragment: IdentifierFragment, temperatureCelsius: Float, notificationId: Int, lastOverNotify: MutableMap<IdentifierFragment, NotifyInfo>, lastUnderNotify: MutableMap<IdentifierFragment, NotifyInfo>, useNode: (TemperatureNode) -> Boolean) {
        for(node in temperatureNodes){
            if(useNode(node)){
                val temperatureName = "Battery Temperature"
                if(node.isOver(temperatureCelsius) && shouldDisplay(node, lastOverNotify[identifierFragment])){
                    lastOverNotify[identifierFragment] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastUnderNotify.remove(identifierFragment)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, true, node.isCritical, notificationId)
                }
                if(node.isUnder(temperatureCelsius) && shouldDisplay(node, lastUnderNotify[identifierFragment])){
                    lastUnderNotify[identifierFragment] = NotifyInfo(System.currentTimeMillis(), node.isCritical)
                    lastOverNotify.remove(identifierFragment)
                    notify(dateMillis, temperatureName, deviceName, temperatureCelsius, false, node.isCritical, notificationId)
                }
            }
        }
    }
    fun checkBatteryTemperature(dateMillis: Long, deviceName: String, identifierFragment: IdentifierFragment, temperatureCelsius: Float) =
            checkTemperature(dateMillis, deviceName, identifierFragment, temperatureCelsius, getBatteryTemperatureId(identifierFragment), lastBatteryOverNotify, lastBatteryUnderNotify) { it.battery }
    fun checkControllerTemperature(dateMillis: Long, deviceName: String, identifierFragment: IdentifierFragment, temperatureCelsius: Float) =
            checkTemperature(dateMillis, deviceName, identifierFragment, temperatureCelsius, getControllerTemperatureId(identifierFragment), lastControllerOverNotify, lastControllerUnderNotify) { it.controller }

    fun checkDeviceCpuTemperature(dateMillis: Long, fragmentId: Int, temperatureCelsius: Float) {
        for(node in temperatureNodes){
            if(node.deviceCpu && (fragmentId in node.deviceCpuIds || node.deviceCpuIds.isEmpty())){
                val temperatureName = "Device CPU Temperature"
                val deviceName = "Device #${fragmentId}"
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
