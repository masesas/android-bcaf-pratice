package com.masesas.exercise.bcaf_test_1.presentation.legacy.notification

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentNotificationBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationFragment : Fragment(R.layout.fragment_notification) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentNotificationBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotificationBinding.bind(view)

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvNotification?.text = state.summaryText(requireContext(), R.string.title_notification)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
