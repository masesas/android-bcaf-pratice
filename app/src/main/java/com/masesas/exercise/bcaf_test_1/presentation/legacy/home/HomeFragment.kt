package com.masesas.exercise.bcaf_test_1.presentation.legacy.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentHomeBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentHomeBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvHome?.text = state.summaryText(requireContext(), R.string.title_home)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
