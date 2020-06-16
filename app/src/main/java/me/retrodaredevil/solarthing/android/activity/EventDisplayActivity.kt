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
import me.retrodaredevil.solarthing.android.util.initializeDrawer
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
import java.text.DateFormat
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
                val dateMillis = packetGroup.getDateMillis(packet) ?: packetGroup.dateMillis
                if(packet is SolarEventPacket){
                    when(packet.packetType){
                        SolarEventPacketType.FX_OPERATIONAL_MODE_CHANGE -> {
                            packet as FXOperationalModeChangePacket
                            val previousOperationalMode = packet.previousOperationalMode
                            if(previousOperationalMode != null) {
                                data.add(ViewData(
                                    "FX${packet.address}",
                                    packet.operationalMode.modeName,
                                    "was: " + previousOperationalMode.modeName,
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.FX_AC_MODE_CHANGE -> {
                            packet as FXACModeChangePacket
                            val previousACMode = packet.previousACMode
                            if(previousACMode != null){
                                data.add(ViewData(
                                    "FX${packet.address}",
                                    packet.acMode.modeName,
                                    "was: " + previousACMode.modeName,
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.FX_AUX_STATE_CHANGE -> {
                            packet as FXAuxStateChangePacket
                            val auxWasActive = packet.auxWasActive
                            if(auxWasActive != null){
                                data.add(ViewData(
                                    "FX${packet.address}",
                                    if(packet.isAuxActive) "AUX ON" else "AUX Off",
                                    "was: " + if(auxWasActive) "ON" else "Off",
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.MXFM_CHARGER_MODE_CHANGE -> {
                            packet as MXChargerModeChangePacket
                            val previousChargingMode = packet.previousChargingMode
                            if(previousChargingMode != null){
                                data.add(ViewData(
                                    "MX${packet.address}",
                                    packet.chargingMode.modeName,
                                    "was: " + previousChargingMode.modeName,
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.MXFM_AUX_MODE_CHANGE -> {
                            packet as MXAuxModeChangePacket
                            if(packet.previousRawAuxModeValue != null){
                                data.add(ViewData(
                                    "MX${packet.address}",
                                    packet.auxMode.modeName,
                                    "was: " + packet.previousAuxMode!!.modeName,
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.FX_WARNING_MODE_CHANGE -> {
                            packet as FXWarningModeChangePacket
                            val previousWarningModeValue = packet.previousWarningModeValue
                            if(previousWarningModeValue != null){
                                data.add(ViewData(
                                    "FX${packet.address}",
                                    Modes.toString(WarningMode::class.java, packet.warningModeValue),
                                    "was: ${Modes.toString(WarningMode::class.java, previousWarningModeValue)}",
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.FX_ERROR_MODE_CHANGE -> {
                            packet as FXErrorModeChangePacket
                            val previousErrorModeValue = packet.previousErrorModeValue
                            if(previousErrorModeValue != null){
                                data.add(ViewData(
                                    "FX${packet.address}",
                                    Modes.toString(FXErrorMode::class.java, packet.errorModeValue),
                                    "was: ${Modes.toString(FXErrorMode::class.java, previousErrorModeValue)}",
                                    dateMillis
                                ))
                            }
                        }
                        SolarEventPacketType.MXFM_ERROR_MODE_CHANGE -> {
                            packet as MXErrorModeChangePacket
                            val previousErrorModeValue = packet.previousErrorModeValue
                            if(previousErrorModeValue != null){
                                data.add(ViewData(
                                    "MX${packet.address}",
                                    Modes.toString(MXErrorMode::class.java, packet.errorModeValue),
                                    "was: ${Modes.toString(MXErrorMode::class.java, previousErrorModeValue)}",
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_display)
        initializeDrawer(this)
        recyclerView = findViewById<RecyclerView>(R.id.events_display_recycler_view).apply {
            layoutManager = LinearLayoutManager(this@EventDisplayActivity)
        }
        lastUpdatedTextView = findViewById(R.id.events_display_last_updated_text)
        update()
    }
    fun onRefreshClick(@Suppress("UNUSED_PARAMETER") view: View) {
        update()
    }
    @SuppressLint("SetTextI18n")
    private fun update(){
        val application = application as SolarThingApplication
        val (packetGroups, updateTime) = application.solarEventData?.getLatestPacketGroups() ?: Pair(emptyList(), null)
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
