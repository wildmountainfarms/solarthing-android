package me.retrodaredevil.solarthing.android

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.retrodaredevil.solarthing.android.prefs.TemperatureNode
import me.retrodaredevil.solarthing.android.util.TemperatureUnit
import me.retrodaredevil.solarthing.android.util.convertTemperatureCelsius
import me.retrodaredevil.solarthing.android.util.shortRepresentation
import java.util.*

private class TemperatureViewHolder(
    val view: View
) : RecyclerView.ViewHolder(view) {
    val typesName: TextView = view.findViewById(R.id.temperature_popup_types_name)
    val highTemperature: TextView = view.findViewById(R.id.temperature_popup_high_temperature)
    val lowTemperature: TextView = view.findViewById(R.id.temperature_popup_low_temperature)
}

private class TemperatureViewAdapter(
    private val data: List<TemperatureNodeData>,
    private val temperatureUnit: TemperatureUnit,
    private val onClick: (TemperatureNodeData) -> Unit
) : RecyclerView.Adapter<TemperatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemperatureViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.temperature_popup_node, parent, false)
        return TemperatureViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: TemperatureViewHolder, position: Int) {
        val viewData = data[data.size - 1 - position]
        val temperatureNode = viewData.temperatureNode
        val typesNameList = (if(temperatureNode.battery) listOf("Battery") else emptyList()) +
                (if(temperatureNode.controller) listOf("Controller") else emptyList()) +
                (if(temperatureNode.deviceCpu) listOf("CPU") else emptyList())
        val typesName = if(typesNameList.isEmpty()) "None" else typesNameList.joinToString(", ")
        holder.typesName.text = typesName
        holder.highTemperature.text = temperatureNode.highThresholdCelsius?.let {
            Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsius(temperatureNode.highThresholdCelsius, temperatureUnit)) + temperatureUnit.shortRepresentation
        } ?: "no high threshold"
        holder.lowTemperature.text = temperatureNode.lowThresholdCelsius?.let {
            Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsius(temperatureNode.lowThresholdCelsius, temperatureUnit)) + temperatureUnit.shortRepresentation
        } ?: "no low threshold"
        holder.view.setOnClickListener {
            onClick(viewData)
        }
        println("Hi!")
    }
}
private class TemperatureNodeData(
    val uuid: UUID,
    var temperatureNode: TemperatureNode
)
class SettingsTemperatureNotifyHandler(
    private val context: Context
) {
    private var updatedNodes: MutableList<TemperatureNodeData>? = null

    fun loadTemperatureNodes(temperatureNodes: List<TemperatureNode>) {
        updatedNodes = temperatureNodes.map { TemperatureNodeData(UUID.randomUUID(), it) }.toMutableList()
    }
    fun getTemperatureNodesToSave(): List<TemperatureNode> {
        return updatedNodes?.map { it.temperatureNode } ?: throw IllegalStateException("updatedNodes is null! Did you not initialize it?")
    }

    fun showDialog(temperatureUnit: TemperatureUnit){
        val updatedNodes = updatedNodes ?: throw IllegalStateException("You should have initialized the nodes!")
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customView = layoutInflater.inflate(R.layout.content_settings_temperature_popup, null)
        val dialog = Dialog(context).apply {
            setCanceledOnTouchOutside(true)
            setContentView(customView)
        }
        val newButton = customView.findViewById<Button>(R.id.settings_temperature_popup_new_button)
        newButton.setOnClickListener {
            dialog.dismiss()
            updatedNodes.add(TemperatureNodeData(UUID.randomUUID(), TemperatureNode()))
        }
        val recyclerView = customView.findViewById<RecyclerView>(R.id.settings_temperature_popup_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
        }
        recyclerView.adapter = TemperatureViewAdapter(updatedNodes, temperatureUnit) {
            dialog.dismiss()
            println("You clicked!")
        }
        dialog.show()
    }
}
