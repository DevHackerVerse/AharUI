package com.example.aharui.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aharui.R
import com.example.aharui.util.DateUtils
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var currentChartType = DateUtils.ChartType.WEEKLY
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var greetingTextView: TextView
    private lateinit var waterProgressBar: ProgressBar
    private lateinit var waterTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var weightProgressChart: LineGraphView
    private lateinit var chartFilterChipGroup: ChipGroup
    private lateinit var logWaterButton: Button
    private lateinit var scanFoodButton: Button
    private lateinit var viewPlanButton: Button
    private lateinit var logWeightButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWaterGoal()
        viewModel.loadChartData(DateUtils.ChartType.WEEKLY)
    }

    private fun initViews(view: View) {
        greetingTextView = view.findViewById(R.id.greeting_textview)
        waterProgressBar = view.findViewById(R.id.water_progress_bar)
        waterTextView = view.findViewById(R.id.water_textview)
        caloriesTextView = view.findViewById(R.id.calories_textview)
        weightProgressChart = view.findViewById(R.id.weight_progress_chart)
        chartFilterChipGroup = view.findViewById(R.id.chart_filter_chip_group)
        logWaterButton = view.findViewById(R.id.log_water_button)
        scanFoodButton = view.findViewById(R.id.scan_food_button)
        viewPlanButton = view.findViewById(R.id.view_plan_button)
        logWeightButton = view.findViewById(R.id.log_weight_button)
    }

    private fun setupListeners() {
        chartFilterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            currentChartType = when (checkedId) { // UPDATE to save current type
                R.id.weekly_chip -> DateUtils.ChartType.WEEKLY
                R.id.monthly_chip -> DateUtils.ChartType.MONTHLY
                R.id.six_months_chip -> DateUtils.ChartType.SIX_MONTHS
                else -> DateUtils.ChartType.WEEKLY
            }
            viewModel.loadChartData(currentChartType)
        }

        logWaterButton.setOnClickListener {
            showWaterInputDialog()
        }

        scanFoodButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanFood)
        }

        logWeightButton.setOnClickListener {
            showLogWeightDialog()
        }

        viewPlanButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_diet)
        }

        // Set initial chart filter
        chartFilterChipGroup.check(R.id.weekly_chip)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: HomeUiState) {
        greetingTextView.text = if (state.userName.isNotEmpty()) {
            "Hi, ${state.userName}"
        } else {
            "Hi, User"
        }

        waterProgressBar.max = state.waterTarget
        waterProgressBar.progress = state.waterConsumed
        waterTextView.text = "${state.waterConsumed} / ${state.waterTarget} ml"

        caloriesTextView.text = "${state.caloriesConsumed} / ${state.calorieTarget} kcal"

        if (state.chartData.isNotEmpty() && state.chartLabels.isNotEmpty()) {
            weightProgressChart.setData(state.chartData, state.chartLabels)
        }

        // Show badge unlock notifications
        if (state.unlockedBadges.isNotEmpty()) {
            showBadgeUnlockedDialog(state.unlockedBadges)
            viewModel.clearBadgeNotifications()
        }

        if (state.weightLoggedSuccess) {
            Toast.makeText(requireContext(), "Weight logged! +15 points", Toast.LENGTH_SHORT).show()
            viewModel.clearWeightLoggedState()
        }

        // If you want to use currentWeight, update some view here
        // state.currentWeight?.let { weight -> ... }

        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }

        logWaterButton.isEnabled = !state.isLoading
    }

    private fun showWaterInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_water_input, null)
        val inputEditText = dialogView.findViewById<TextInputEditText>(R.id.water_amount_input)
        val preset250 = dialogView.findViewById<Button>(R.id.preset_250)
        val preset500 = dialogView.findViewById<Button>(R.id.preset_500)
        val preset1000 = dialogView.findViewById<Button>(R.id.preset_1000)

        preset250.setOnClickListener { inputEditText.setText("250") }
        preset500.setOnClickListener { inputEditText.setText("500") }
        preset1000.setOnClickListener { inputEditText.setText("1000") }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Water Intake")
            .setView(dialogView)
            .setPositiveButton("Log") { dialog, _ ->
                val amountText = inputEditText.text.toString()
                val amount = amountText.toIntOrNull()

                if (amount != null && amount > 0) {
                    viewModel.logWater(amount)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter a valid amount",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBadgeUnlockedDialog(badges: List<String>) {
        val message = if (badges.size == 1) {
            "🎉 Congratulations!\n\nYou unlocked:\n${badges[0]}"
        } else {
            "🎉 Congratulations!\n\nYou unlocked ${badges.size} badges:\n${badges.joinToString("\n")}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Badge Unlocked!")
            .setMessage(message)
            .setPositiveButton("View Rewards") { dialog, _ ->
                findNavController().navigate(R.id.navigation_rewards)
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogWeightDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_log_weight, null)
        val weightInput = dialogView.findViewById<TextInputEditText>(R.id.weight_input)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.date_input)

        // Set today's date by default
        val today = DateUtils.getTodayString()
        dateInput.setText(today)

        // Date picker
        dateInput.setOnClickListener {
            showDatePicker { selectedDate ->
                dateInput.setText(selectedDate)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Weight")
            .setView(dialogView)
            .setPositiveButton("Log") { dialog, _ ->
                val weightText = weightInput.text.toString()
                val weight = weightText.toDoubleOrNull()
                val date = dateInput.text.toString()

                if (weight != null && weight > 0) {
                    viewModel.logWeight(weight, date)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter a valid weight",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                onDateSelected(date)
            },
            year,
            month,
            day
        ).show()
    }
}
