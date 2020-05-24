package me.retrodaredevil.solarthing.android

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.retrodaredevil.solarthing.android.prefs.TemperatureNode
import me.retrodaredevil.solarthing.android.data.TemperatureUnit
import me.retrodaredevil.solarthing.android.data.convertTemperatureCelsiusTo
import me.retrodaredevil.solarthing.android.data.convertToCelsius
import me.retrodaredevil.solarthing.android.data.shortRepresentation
import me.retrodaredevil.solarthing.android.util.Formatting
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
                (if(temperatureNode.deviceCpu) listOf("CPU ${temperatureNode.deviceCpuIdsString}") else emptyList())
        val typesName = if(typesNameList.isEmpty()) "None" else typesNameList.joinToString(", ")
        holder.typesName.text = typesName
        holder.highTemperature.text = temperatureNode.highThresholdCelsius?.let {
            Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(temperatureNode.highThresholdCelsius, temperatureUnit)) + temperatureUnit.shortRepresentation
        } ?: "no high threshold"
        holder.lowTemperature.text = temperatureNode.lowThresholdCelsius?.let {
            Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(temperatureNode.lowThresholdCelsius, temperatureUnit)) + temperatureUnit.shortRepresentation
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
            val nodeData = TemperatureNodeData(UUID.randomUUID(), TemperatureNode())
            updatedNodes.add(nodeData)
            showEditDialog(temperatureUnit, nodeData)
        }
        customView.findViewById<RecyclerView>(R.id.settings_temperature_popup_recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TemperatureViewAdapter(updatedNodes, temperatureUnit) {
                dialog.dismiss()
                showEditDialog(temperatureUnit, it)
            }
        }
        dialog.show()
    }
    private fun showEditDialog(temperatureUnit: TemperatureUnit, nodeData: TemperatureNodeData) {
        val updatedNodes = updatedNodes ?: throw IllegalStateException("You should have initialized the nodes!")
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customView = layoutInflater.inflate(R.layout.content_settings_temperature_popup_edit, null)
        val batteryCheckbox = customView.findViewById<CheckBox>(R.id.settings_temperature_node_battery_checkbox)
        val controllerCheckbox = customView.findViewById<CheckBox>(R.id.settings_temperature_node_controller_checkbox)
        val deviceCpuCheckbox = customView.findViewById<CheckBox>(R.id.settings_temperature_node_device_cpu_checkbox)
        val deviceCpuIdsEditText = customView.findViewById<EditText>(R.id.settings_temperature_node_device_cpu_ids_text)
        val highTemperatureThresholdEditText = customView.findViewById<EditText>(R.id.settings_temperature_node_high_temperature_threshold)
        val lowTemperatureThresholdEditText = customView.findViewById<EditText>(R.id.settings_temperature_node_low_temperature_threshold)
        val isCriticalCheckbox = customView.findViewById<CheckBox>(R.id.settings_temperature_node_is_critical)

        deviceCpuCheckbox.setOnCheckedChangeListener { _, isChecked ->
            deviceCpuIdsEditText.isVisible = isChecked
        }

        nodeData.temperatureNode.let { node ->
            batteryCheckbox.isChecked = node.battery
            controllerCheckbox.isChecked = node.controller
            deviceCpuCheckbox.isChecked = node.deviceCpu
            deviceCpuIdsEditText.isVisible = node.deviceCpu
            deviceCpuIdsEditText.setText(node.deviceCpuIdsString)
            highTemperatureThresholdEditText.setText(node.highThresholdCelsius?.let { Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(it, temperatureUnit)) } ?: "")
            lowTemperatureThresholdEditText.setText(node.lowThresholdCelsius?.let { Formatting.OPTIONAL_TENTHS.format(convertTemperatureCelsiusTo(it, temperatureUnit)) } ?: "")
            isCriticalCheckbox.isChecked = node.isCritical
        }

        val dialog = Dialog(context).apply {
            setCanceledOnTouchOutside(true)
            setContentView(customView)
        }
        dialog.setOnDismissListener {  // this is also called if the dialog is cancelled
            nodeData.temperatureNode = TemperatureNode(
                    battery = batteryCheckbox.isChecked,
                    controller = controllerCheckbox.isChecked,
                    deviceCpu = deviceCpuCheckbox.isChecked,
                    deviceCpuIds = deviceCpuIdsEditText.text.split(",").map { it.trim().toIntOrNull() },
                    highThresholdCelsius = highTemperatureThresholdEditText.text.toString().toFloatOrNull()?.let { temperatureUnit.convertToCelsius(it) },
                    lowThresholdCelsius = lowTemperatureThresholdEditText.text.toString().toFloatOrNull()?.let { temperatureUnit.convertToCelsius(it) },
                    isCritical = isCriticalCheckbox.isChecked
            )
            showDialog(temperatureUnit)
        }
        val deleteButton = customView.findViewById<Button>(R.id.settings_temperature_popup_delete_button)
        deleteButton.setOnClickListener {
            dialog.dismiss()
            updatedNodes.remove(nodeData)
        }
        dialog.show()
    }
}
