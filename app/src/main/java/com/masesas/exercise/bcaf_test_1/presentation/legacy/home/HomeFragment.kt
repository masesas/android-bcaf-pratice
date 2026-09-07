package com.masesas.exercise.bcaf_test_1.presentation.legacy.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.collectOnLifecycle
import com.masesas.exercise.bcaf_test_1.core.ui.resolve
import com.masesas.exercise.bcaf_test_1.core.ui.toUiMessage
import com.masesas.exercise.bcaf_test_1.databinding.FragmentHomeBinding
import com.masesas.exercise.bcaf_test_1.presentation.legacy.summaryText
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthViewModel
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductUiState
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val authViewModel: AuthViewModel by activityViewModels()
    private val loanProductViewModel: LoanProductViewModel by activityViewModels()

    private var binding: FragmentHomeBinding? = null
    private var adapter: LoanProductAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        setUpList()
        observeAuth()
        observeLoanProducts()
    }

    override fun onDestroyView() {
        binding?.rvLoanProduct?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun setUpList() {
        val binding = binding ?: return
        val listAdapter = LoanProductAdapter().also { adapter = it }

        binding.rvLoanProduct.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
            setHasFixedSize(true)
            addOnScrollListener(LoadMoreScrollListener { loanProductViewModel.loadMore() })
        }

        listAdapter.notifyDataSetChanged()

        binding.btnRetry.setOnClickListener { loanProductViewModel.refresh(initialLoad = true) }
    }

    private fun observeAuth() {
        authViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            binding?.tvHome?.text = state.summaryText(requireContext(), R.string.title_home)
        }
    }

    private fun observeLoanProducts() {
        loanProductViewModel.uiState.collectOnLifecycle(viewLifecycleOwner) { state ->
            adapter?.submitList(state.items)
            renderStatus(state)
        }
    }

    private fun renderStatus(state: LoanProductUiState) {
        val binding = binding ?: return
        val failure = state.blockingFailure

        binding.progressLoadMore.isVisible = state.isLoadingMore
        binding.progressInitial.isVisible = state.isRefreshing && state.items.isEmpty()

        val message = failure?.let { requireContext().resolve(it.toUiMessage()) }
            ?: getString(R.string.loan_product_empty).takeIf { state.isEmpty }

        binding.tvMessage.text = message.orEmpty()
        binding.groupMessage.isVisible = message != null
        binding.btnRetry.isVisible = failure != null
    }
}
