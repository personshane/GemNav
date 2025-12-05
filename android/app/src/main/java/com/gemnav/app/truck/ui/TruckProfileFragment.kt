package com.gemnav.app.truck.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gemnav.app.R
import com.gemnav.app.truck.domain.TruckProfileManager
import com.gemnav.app.truck.model.HazmatClass
import com.gemnav.app.truck.model.TruckProfile
import com.gemnav.app.truck.model.TruckType
import kotlin.math.roundToInt

class TruckProfileFragment : Fragment() {

    private lateinit var manager: TruckProfileManager
    private var isMetric = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_truck_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = TruckProfileManager(requireContext())
        isMetric = manager.getUnitPreference()

        val nameEdit = view.findViewById<EditText>(R.id.truck_name_input)
        val heightEdit = view.findViewById<EditText>(R.id.truck_height_input)
        val widthEdit = view.findViewById<EditText>(R.id.truck_width_input)
        val lengthEdit = view.findViewById<EditText>(R.id.truck_length_input)
        val weightEdit = view.findViewById<EditText>(R.id.truck_weight_input)
        val axlesEdit = view.findViewById<EditText>(R.id.truck_axles_input)
        val hazmatSpinner = view.findViewById<Spinner>(R.id.truck_hazmat_spinner)
        val typeSpinner = view.findViewById<Spinner>(R.id.truck_type_spinner)
        val avoidTollsSwitch = view.findViewById<Switch>(R.id.switch_avoid_tolls)
        val avoidFerriesSwitch = view.findViewById<Switch>(R.id.switch_avoid_ferries)
        val avoidLowBridgesSwitch = view.findViewById<Switch>(R.id.switch_avoid_low_bridges)
        val saveButton = view.findViewById<Button>(R.id.button_save_truck_profile)
        val radioGroup = view.findViewById<RadioGroup>(R.id.unit_radio_group)
        val labelHeight = view.findViewById<TextView>(R.id.label_height)
        val labelWidth = view.findViewById<TextView>(R.id.label_width)
        val labelLength = view.findViewById<TextView>(R.id.label_length)
        val labelWeight = view.findViewById<TextView>(R.id.label_weight)

        // Set up spinners
        val hazmatAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            HazmatClass.values().map { it.name.replace('_', ' ') }
        )
        hazmatAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        hazmatSpinner.adapter = hazmatAdapter

        val typeAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            TruckType.values().map { it.name.replace('_', ' ') }
        )
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        // Set radio button based on saved preference
        if (isMetric) {
            radioGroup.check(R.id.radio_metric)
        } else {
            radioGroup.check(R.id.radio_imperial)
        }

        // Load existing profile ONCE on startup
        val profile = manager.getProfile()
        nameEdit.setText(profile.name)
        if (isMetric) {
            heightEdit.setText(roundToOne(profile.heightMeters).toString())
            widthEdit.setText(roundToOne(profile.widthMeters).toString())
            lengthEdit.setText(roundToOne(profile.lengthMeters).toString())
            weightEdit.setText(roundToOne(profile.weightTons).toString())
            labelHeight.text = "Height (meters)"
            labelWidth.text = "Width (meters)"
            labelLength.text = "Length (meters)"
            labelWeight.text = "Weight (metric tons)"
        } else {
            heightEdit.setText(metersToFeet(profile.heightMeters).roundToInt().toString())
            widthEdit.setText(metersToFeet(profile.widthMeters).roundToInt().toString())
            lengthEdit.setText(metersToFeet(profile.lengthMeters).roundToInt().toString())
            weightEdit.setText(tonsToLbs(profile.weightTons).roundToInt().toString())
            labelHeight.text = "Height (feet)"
            labelWidth.text = "Width (feet)"
            labelLength.text = "Length (feet)"
            labelWeight.text = "Weight (pounds)"
        }
        axlesEdit.setText(profile.axleCount.toString())
        hazmatSpinner.setSelection(HazmatClass.values().indexOf(profile.hazmatClass))
        typeSpinner.setSelection(TruckType.values().indexOf(profile.truckType))
        avoidTollsSwitch.isChecked = profile.avoidTolls
        avoidFerriesSwitch.isChecked = profile.avoidFerries
        avoidLowBridgesSwitch.isChecked = profile.avoidLowBridges

        // Handle unit system changes
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            isMetric = checkedId == R.id.radio_metric
            updateLabelsAndValues(
                isMetric, 
                heightEdit, widthEdit, lengthEdit, weightEdit,
                labelHeight, labelWidth, labelLength, labelWeight
            )
        }

        // Save button handler
        saveButton.setOnClickListener {
            val height = heightEdit.text.toString().toDoubleOrNull()
            val width = widthEdit.text.toString().toDoubleOrNull()
            val length = lengthEdit.text.toString().toDoubleOrNull()
            val weight = weightEdit.text.toString().toDoubleOrNull()
            val axles = axlesEdit.text.toString().toIntOrNull()

            if (height == null || width == null || length == null || weight == null || axles == null) {
                Toast.makeText(requireContext(), R.string.truck_profile_invalid_numbers, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert to metric if in imperial mode
            val heightMeters = if (isMetric) height else feetToMeters(height)
            val widthMeters = if (isMetric) width else feetToMeters(width)
            val lengthMeters = if (isMetric) length else feetToMeters(length)
            val weightTons = if (isMetric) weight else lbsToTons(weight)

            val updatedProfile = TruckProfile(
                name = nameEdit.text.toString(),
                heightMeters = heightMeters,
                widthMeters = widthMeters,
                lengthMeters = lengthMeters,
                weightTons = weightTons,
                axleCount = axles,
                hazmatClass = HazmatClass.values()[hazmatSpinner.selectedItemPosition],
                truckType = TruckType.values()[typeSpinner.selectedItemPosition],
                avoidTolls = avoidTollsSwitch.isChecked,
                avoidFerries = avoidFerriesSwitch.isChecked,
                avoidLowBridges = avoidLowBridgesSwitch.isChecked
            )

            manager.saveProfile(updatedProfile)
            manager.saveUnitPreference(isMetric)
            Toast.makeText(requireContext(), R.string.truck_profile_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLabelsAndValues(
        toMetric: Boolean,
        heightEdit: EditText,
        widthEdit: EditText,
        lengthEdit: EditText,
        weightEdit: EditText,
        labelHeight: TextView,
        labelWidth: TextView,
        labelLength: TextView,
        labelWeight: TextView
    ) {
        val height = heightEdit.text.toString().toDoubleOrNull() ?: 0.0
        val width = widthEdit.text.toString().toDoubleOrNull() ?: 0.0
        val length = lengthEdit.text.toString().toDoubleOrNull() ?: 0.0
        val weight = weightEdit.text.toString().toDoubleOrNull() ?: 0.0

        if (toMetric) {
            // Converting from imperial to metric
            heightEdit.setText(roundToOne(feetToMeters(height)).toString())
            widthEdit.setText(roundToOne(feetToMeters(width)).toString())
            lengthEdit.setText(roundToOne(feetToMeters(length)).toString())
            weightEdit.setText(roundToOne(lbsToTons(weight)).toString())
            labelHeight.text = "Height (meters)"
            labelWidth.text = "Width (meters)"
            labelLength.text = "Length (meters)"
            labelWeight.text = "Weight (metric tons)"
        } else {
            // Converting from metric to imperial
            heightEdit.setText(metersToFeet(height).roundToInt().toString())
            widthEdit.setText(metersToFeet(width).roundToInt().toString())
            lengthEdit.setText(metersToFeet(length).roundToInt().toString())
            weightEdit.setText(tonsToLbs(weight).roundToInt().toString())
            labelHeight.text = "Height (feet)"
            labelWidth.text = "Width (feet)"
            labelLength.text = "Length (feet)"
            labelWeight.text = "Weight (pounds)"
        }
    }

    private fun roundToOne(value: Double): Double = (value * 10).roundToInt() / 10.0
    private fun metersToFeet(meters: Double): Double = meters * 3.28084
    private fun feetToMeters(feet: Double): Double = feet / 3.28084
    private fun tonsToLbs(tons: Double): Double = tons * 2204.62
    private fun lbsToTons(lbs: Double): Double = lbs / 2204.62
}
