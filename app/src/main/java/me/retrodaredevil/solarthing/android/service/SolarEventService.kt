package me.retrodaredevil.solarthing.android.service

import android.app.Notification
import android.app.Service
import android.os.Build
import me.retrodaredevil.solarthing.DataSource
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.notifications.NotificationChannels
import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.solar.outback.command.packets.MateCommandFeedbackPacket
import me.retrodaredevil.solarthing.solar.outback.command.packets.MateCommandFeedbackPacketType
import me.retrodaredevil.solarthing.solar.outback.command.packets.SuccessMateCommandPacket
import java.text.DateFormat
import java.util.*

class SolarEventService(
    private val service: Service,
    private val data: SolarEventData
) : DataService {

    override fun onInit() { // nothing
    }

    override fun onCancel() { // we don't need to cancel notifications because we didn't create any Persistent notifications
        data.lastCancel = System.currentTimeMillis()
    }

    override fun onEnd() {
    }

    override fun onNewDataRequestLoadStart() {
    }

    override fun onDataRequest(dataRequest: DataRequest) {
        val newPackets = dataRequest.packetGroupList
        data.packetGroups.addAll(newPackets)
        data.lastUpdate = System.currentTimeMillis()
        for(packetGroup in newPackets){
            val dateMillis = packetGroup.dateMillis
            for(packet in packetGroup.packets){
                if(packet is MateCommandFeedbackPacket){
                    when(packet.packetType){
                        MateCommandFeedbackPacketType.MATE_COMMAND_SUCCESS -> doNotify(packet as SuccessMateCommandPacket, dateMillis)
                        else -> System.err.println("unknown packet type: ${packet.packetType}")
                    }
                }
            }
        }
    }
    private fun doNotify(packet: SuccessMateCommandPacket, dateMillis: Long){ // dateMillis is the time that the command was executed
        if(dateMillis + 10 * 60 * 1000 < System.currentTimeMillis()){
            return // We got a packet from a long time ago. We don't need to display it
        }
        val source = packet.source!!
        val dataSource = DataSource.createFromStringOrNull(source) // the dateMillis in here is the time the request for the CommandSequence was made, and may be the same for multiple commands
        if(dataSource == null){
            System.err.println("dataSource was null!")
            return
        }
        val id = Objects.hash(dateMillis, packet.command, source)
        var groupBuilder: Notification.Builder? = null
        val builder = getBuilder()
            .setSmallIcon(R.drawable.solar_panel)
            .setContentTitle("Command executed! ${packet.command.commandName}")
            .setWhen(dateMillis)
            .setShowWhen(false)
            .setContentText("From: '${dataSource.sender}' at " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(GregorianCalendar().apply { timeInMillis = dateMillis}.time))
            .setSubText(dataSource.data + " requested at " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(GregorianCalendar().apply { timeInMillis = dataSource.dateMillis}.time))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val group = "command-feedback-$source"
            groupBuilder = getBuilder()
                .setGroup(group)
                .setGroupSummary(true)
                .setWhen(dataSource.dateMillis)
                .setShowWhen(true)
                .setSmallIcon(R.drawable.solar_panel)

            builder.setGroup(group)
        }
        service.getManager().apply {
            notify(id, builder.build())
            if(groupBuilder != null){
                notify(Objects.hash(source), groupBuilder.build())
            }
        }
    }
    @SuppressWarnings("deprecated")
    private fun getBuilder(): Notification.Builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        Notification.Builder(service.applicationContext, NotificationChannels.COMMAND_FEEDBACK.id)
    } else {
        Notification.Builder(service.applicationContext)
    }

    override fun onTimeout() {
        data.lastTimeout = System.currentTimeMillis()
    }

    override val updatePeriodType = UpdatePeriodType.SMALL_DATA
    override val startKey: Long
        get() = data.packetGroups.lastOrNull()?.dateMillis?.plus(1) ?: (System.currentTimeMillis() - (18 * 60 * 60 * 1000))
    override val shouldUpdate: Boolean
        get() = NotificationChannels.COMMAND_FEEDBACK.isCurrentlyEnabled(service) || NotificationChannels.SOLAR_STATUS.isCurrentlyEnabled(service)

}
