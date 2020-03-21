package me.retrodaredevil.solarthing.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.retrodaredevil.solarthing.packets.collection.PacketGroup
import me.retrodaredevil.solarthing.solar.event.SolarEventPacket
import me.retrodaredevil.solarthing.solar.event.SolarEventPacketType
import me.retrodaredevil.solarthing.solar.outback.fx.event.FXOperationalModeChangePacket
import java.text.DateFormat
import java.util.*

private class MyViewHolder(
    val textView: TextView
) : RecyclerView.ViewHolder(textView)

private class MyViewAdapter(
    packetGroups: List<PacketGroup>
) : RecyclerView.Adapter<MyViewHolder>() {
    private val data = mutableListOf<String>()
    init {
        for(packetGroup in packetGroups){
            for(packet in packetGroup.packets){
                val dateMillis = packetGroup.getDateMillis(packet) ?: packetGroup.dateMillis
                val timeString = DateFormat.getTimeInstance(DateFormat.MEDIUM)
                    .format(GregorianCalendar().apply { timeInMillis = dateMillis }.time)
                if(packet is SolarEventPacket){
                    when(packet.packetType){
                        SolarEventPacketType.FX_OPERATIONAL_MODE_CHANGE -> {
                            packet as FXOperationalModeChangePacket
                            val previousOperationalMode = packet.previousOperationalMode
                            if(previousOperationalMode != null) {
                                data.add("$timeString FX${packet.address} ${previousOperationalMode.modeName} -> ${packet.operationalMode.modeName}")
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_display_text_view, parent, false)
        return MyViewHolder(view as TextView)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = data[data.size - 1 - position]
    }

}

class EventDisplayActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_display)
        initializeDrawer(this)
        recyclerView = findViewById<RecyclerView>(R.id.events_display_recycler_view).apply {
            layoutManager = LinearLayoutManager(this@EventDisplayActivity)
        }
        update()
    }
    private fun update(){
        val application = application as SolarThingApplication
        val (packetGroups, updateTime) = application.solarEventData?.getLatestPacketGroups() ?: Pair(emptyList(), null)
        if(updateTime == null){
            return
        }
        recyclerView.adapter = MyViewAdapter(packetGroups)
    }
}
