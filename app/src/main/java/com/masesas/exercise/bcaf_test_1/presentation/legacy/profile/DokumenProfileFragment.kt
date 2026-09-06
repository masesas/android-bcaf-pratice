package com.masesas.exercise.bcaf_test_1.presentation.legacy.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentDokumenProfileBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DokumenProfileFragment : Fragment(R.layout.fragment_dokumen_profile) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentDokumenProfileBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDokumenProfileBinding.bind(view)

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvDokumenProfile?.text = state.summaryText(requireContext(), R.string.title_dokumen_profile)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
