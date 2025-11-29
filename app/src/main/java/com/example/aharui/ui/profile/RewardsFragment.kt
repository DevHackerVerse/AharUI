package com.example.aharui.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aharui.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    private val viewModel: RewardsViewModel by viewModels()

    private lateinit var pointsTextView: TextView
    private lateinit var streakTextView: TextView
    private lateinit var badgesRecyclerView: RecyclerView

    private val badgeAdapter = BadgeAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rewards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        observeViewModel()
    }

    private fun initViews(view: View) {
        pointsTextView = view.findViewById(R.id.points_textview)
        streakTextView = view.findViewById(R.id.streak_textview)
        badgesRecyclerView = view.findViewById(R.id.badges_recyclerview)
    }

    private fun setupRecyclerView() {
        badgesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = badgeAdapter
        }
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

    private fun updateUI(state: RewardsUiState) {
        pointsTextView.text = state.pointsTotal.toString()
        streakTextView.text = "${state.streakDays} days"
        badgeAdapter.submitList(state.badges)
    }
}