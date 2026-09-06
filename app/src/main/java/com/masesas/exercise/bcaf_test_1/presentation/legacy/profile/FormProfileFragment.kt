package com.masesas.exercise.bcaf_test_1.presentation.legacy.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentFormProfileBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FormProfileFragment : Fragment(R.layout.fragment_form_profile) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentFormProfileBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFormProfileBinding.bind(view)

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvFormProfile?.text = state.summaryText(requireContext(), R.string.title_form_profile)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
