package com.masesas.exercise.bcaf_test_1.presentation.legacy.transaction

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.databinding.FragmentTransactionDetailBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionDetailFragment : Fragment(R.layout.fragment_transaction_detail) {

    private val args: TransactionDetailFragmentArgs by navArgs()

    private val authViewModel: AuthViewModel by activityViewModels()

    private var binding: FragmentTransactionDetailBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransactionDetailBinding.bind(view)

        val title = getString(R.string.transaction_detail_content, args.transactionId)
        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvTransactionDetail?.text = state.summaryText(requireContext(), title)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
