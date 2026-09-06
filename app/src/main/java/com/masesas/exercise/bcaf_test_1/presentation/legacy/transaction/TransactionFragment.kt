package com.masesas.exercise.bcaf_test_1.presentation.legacy.transaction

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentTransactionBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionFragment : Fragment(R.layout.fragment_transaction) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentTransactionBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransactionBinding.bind(view).apply {
            btnTransactionDetail.setOnClickListener {
                val direction = TransactionFragmentDirections
                    .actionTransactionToTransactionDetail(transactionId = "TRX-001")
                findNavController().navigate(direction)
            }
        }

        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvTransaction?.text =
                state.summaryText(requireContext(), R.string.title_transaction)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
