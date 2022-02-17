package me.retrodaredevil.solarthing.android.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.retrodaredevil.solarthing.DataSource
import me.retrodaredevil.solarthing.android.R
import me.retrodaredevil.solarthing.android.SolarThingApplication
import me.retrodaredevil.solarthing.android.util.DrawerHandler
import me.retrodaredevil.solarthing.android.util.initializeDrawer
import me.retrodaredevil.solarthing.packets.ChangePacket
import me.retrodaredevil.solarthing.packets.Modes
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.solar.event.SolarEventPacket
import me.retrodaredevil.solarthing.solar.event.SolarEventPacketType
import me.retrodaredevil.solarthing.solar.outback.command.packets.MateCommandFeedbackPacket
import me.retrodaredevil.solarthing.solar.outback.command.packets.MateCommandFeedbackPacketType
import me.retrodaredevil.solarthing.solar.outback.command.packets.SuccessMateCommandPacket
import me.retrodaredevil.solarthing.solar.outback.fx.FXErrorMode
import me.retrodaredevil.solarthing.solar.outback.fx.WarningMode
import me.retrodaredevil.solarthing.solar.outback.fx.event.*
import me.retrodaredevil.solarthing.solar.outback.mx.MXErrorMode
import me.retrodaredevil.solarthing.solar.outback.mx.event.MXAuxModeChangePacket
import me.retrodaredevil.solarthing.solar.outback.mx.event.MXChargerModeChangePacket
import me.retrodaredevil.solarthing.solar.outback.mx.event.MXErrorModeChangePacket
import me.retrodaredevil.solarthing.solar.renogy.rover.RoverErrorMode
import me.retrodaredevil.solarthing.solar.renogy.rover.event.RoverChargingStateChangePacket
import me.retrodaredevil.solarthing.solar.renogy.rover.event.RoverErrorModeChangePacket
import me.retrodaredevil.solarthing.solar.tracer.TracerChargingEquipmentStatus
import me.retrodaredevil.solarthing.solar.tracer.event.TracerChargingEquipmentStatusChangePacket
import me.retrodaredevil.solarthing.solar.tracer.mode.ChargingEquipmentError
import java.text.DateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

/*
Some useful documentation: https://developer.android.com/guide/topics/ui/layout/recyclerview#kotlin
https://developer.android.com/jetpack/androidx/releases/recyclerview
 */

private class MyViewHolder(
        view: View
) : RecyclerView.ViewHolder(view) {
    val nameText: TextView = view.findViewById(R.id.event_display_name)
    val titleText: TextView = view.findViewById(R.id.event_display_title)
    val textText: TextView = view.findViewById(R.id.event_display_text)
    val timeText: TextView = view.findViewById(R.id.event_display_time)
}
private class ViewData(
        val name: String,
        val title: String,
        val text: String,
        val dateMillis: Long
)

private class MyViewAdapter(
        packetGroups: List<PacketGroup>
) : RecyclerView.Adapter<MyViewHolder>() {
    private val data = mutableListOf<ViewData>()
    init {
        for(packetGroup in packetGroups){
            for(packet in packetGroup.packets){
                if (packet is ChangePacket && packet.isLastUnknown) {
                    continue
                }
                val dateMillis = packetGroup.getDateMillis(packet) ?: packetGroup.dateMillis
                if(packet is SolarEventPacket){
                    when(packet.packetType){
                        SolarEventPacketType.FX_OPERATIONAL_MODE_CHANGE -> {
                            packet as FXOperationalModeChangePacket
                            data.add(ViewData(
                                    "FX${packet.address}",
                                    packet.operationalMode.modeName,
                                    "was: " + packet.previousOperationalMode!!.modeName,
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.FX_AC_MODE_CHANGE -> {
                            packet as FXACModeChangePacket
                            data.add(ViewData(
                                    "FX${packet.address}",
                                    packet.acMode.modeName,
                                    "was: " + packet.previousACMode!!.modeName,
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.FX_AUX_STATE_CHANGE -> {
                            packet as FXAuxStateChangePacket
                            data.add(ViewData(
                                    "FX${packet.address}",
                                    if(packet.isAuxActive) "AUX ON" else "AUX Off",
                                    "was: " + if(packet.auxWasActive!!) "ON" else "Off",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.MXFM_CHARGER_MODE_CHANGE -> {
                            packet as MXChargerModeChangePacket
                            data.add(ViewData(
                                    "MX${packet.address}",
                                    packet.chargingMode.modeName,
                                    "was: " + packet.previousChargingMode!!.modeName,
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.MXFM_AUX_MODE_CHANGE -> {
                            packet as MXAuxModeChangePacket
                            data.add(ViewData(
                                    "MX${packet.address}",
                                    packet.auxMode.modeName,
                                    "was: " + packet.previousAuxMode!!.modeName,
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.FX_WARNING_MODE_CHANGE -> {
                            packet as FXWarningModeChangePacket
                            data.add(ViewData(
                                    "FX${packet.address}",
                                    Modes.toString(WarningMode::class.java, packet.warningModeValue),
                                    "was: ${Modes.toString(WarningMode::class.java, packet.previousWarningModeValue!!)}",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.FX_ERROR_MODE_CHANGE -> {
                            packet as FXErrorModeChangePacket
                            data.add(ViewData(
                                    "FX${packet.address}",
                                    Modes.toString(FXErrorMode::class.java, packet.errorModeValue),
                                    "was: ${Modes.toString(FXErrorMode::class.java, packet.previousErrorModeValue!!)}",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.MXFM_ERROR_MODE_CHANGE -> {
                            packet as MXErrorModeChangePacket
                            data.add(ViewData(
                                    "MX${packet.address}",
                                    Modes.toString(MXErrorMode::class.java, packet.errorModeValue),
                                    "was: ${Modes.toString(MXErrorMode::class.java, packet.previousErrorModeValue!!)}",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.ROVER_CHARGING_STATE_CHANGE -> {
                            packet as RoverChargingStateChangePacket
                            data.add(ViewData(
                                    "RVR",
                                    packet.chargingMode.modeName,
                                    "was: ${packet.previousChargingMode!!.modeName}",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.ROVER_ERROR_MODE_CHANGE -> {
                            packet as RoverErrorModeChangePacket
                            data.add(ViewData(
                                    "RVR",
                                    Modes.toString(RoverErrorMode::class.java, packet.errorModeValue),
                                    "was: ${Modes.toString(RoverErrorMode::class.java, packet.previousErrorModeValue!!)}",
                                    dateMillis
                            ))
                        }
                        SolarEventPacketType.TRACER_CHARGING_EQUIPMENT_STATUS_CHANGE -> {
                            packet as TracerChargingEquipmentStatusChangePacket
                            val current = packet.chargingEquipmentStatus
                            val previous = packet.previousChargingEquipmentStatus!!
                            if (current.chargingStatus != previous.chargingStatus) {
                                data.add(ViewData(
                                        "TCR",
                                        current.chargingStatus.modeName,
                                        "was: ${previous.chargingStatus.modeName}",
                                        dateMillis
                                ))
                            }
                            val currentErrors = current.errorModes - ChargingEquipmentError.FAULT
                            val previousErrors = previous.errorModes - ChargingEquipmentError.FAULT
                            if (currentErrors != previousErrors) {
                                data.add(ViewData(
                                        "TCR",
                                        Modes.toString(currentErrors, -1), // -1 will make all active
                                        "was: ${Modes.toString(previousErrors, -1)}",
                                        dateMillis
                                ))
                            }
                        }
                        else -> {}
                    }
                } else if(packet is MateCommandFeedbackPacket){
                    if(packet.packetType == MateCommandFeedbackPacketType.MATE_COMMAND_SUCCESS){
                        packet as SuccessMateCommandPacket
                        val dataSource = DataSource.createFromStringOrNull(packet.source)
                        val timeString = if(dataSource == null) "???" else DateFormat.getTimeInstance(DateFormat.MEDIUM)
                                .format(GregorianCalendar().apply { timeInMillis = dataSource.dateMillis }.time)
                        val senderString = dataSource?.sender ?: "???"
                        data.add(ViewData(
                                "cmd",
                                packet.command.commandName,
                                "Requested at $timeString by $senderString",
                                dateMillis
                        ))
                    }
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_display_view, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val viewData = data[data.size - 1 - position]
        val timeString = DateFormat.getTimeInstance(DateFormat.MEDIUM)
                .format(GregorianCalendar().apply { timeInMillis = viewData.dateMillis }.time)
        holder.nameText.text = viewData.name
        holder.titleText.text = viewData.title
        holder.textText.text = viewData.text
        holder.timeText.text = timeString
    }

}

class EventDisplayActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var lastUpdatedTextView: TextView

    private lateinit var drawerHandler: DrawerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_display)
        drawerHandler = initializeDrawer(this)
        recyclerView = findViewById<RecyclerView>(R.id.events_display_recycler_view).apply {
            layoutManager = LinearLayoutManager(this@EventDisplayActivity)
        }
        lastUpdatedTextView = findViewById(R.id.events_display_last_updated_text)
        update()
    }
    override fun onResume() {
        super.onResume()
        drawerHandler.closeDrawer()
        drawerHandler.highlight()
    }
    fun onRefreshClick(@Suppress("UNUSED_PARAMETER") view: View) {
        update()
    }
    @SuppressLint("SetTextI18n")
    private fun update(){
        val application = application as SolarThingApplication
        val (packetGroups, updateTime) = application.solarEventData?.useCacheGetLastUpdate { it.allCachedPackets } ?: Pair(emptyList(), null)
        if(updateTime == null){
            lastUpdatedTextView.text = "No data"
            return
        }
        val timeString = DateFormat.getTimeInstance(DateFormat.MEDIUM)
                .format(GregorianCalendar().apply { timeInMillis = updateTime }.time)
        lastUpdatedTextView.text = "Data from $timeString"
        recyclerView.adapter = MyViewAdapter(packetGroups)
    }
}
