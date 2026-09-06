package com.masesas.exercise.bcaf_test_1.presentation.legacy.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentProfileBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentProfileBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view).apply {
            btnFormProfile.setOnClickListener {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToFormProfile()
                )
            }
            btnDokumenProfile.setOnClickListener {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToDokumenProfile()
                )
            }
        }

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvProfile?.text =
                state.summaryText(requireContext(), R.string.title_profile)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
