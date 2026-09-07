package com.masesas.exercise.bcaf_test_1.presentation.legacy.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ext.toRupiahDigits
import com.masesas.exercise.bcaf_test_1.databinding.ItemLoanProductBinding
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct

class LoanProductAdapter : ListAdapter<LoanProduct, LoanProductAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLoanProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemLoanProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LoanProduct) = with(binding) {
            val context = root.context

            tvName.text = item.name
            tvCode.text = item.code
            tvStatus.visibility = if (item.isActive) View.GONE else View.VISIBLE

            tvInterest.text = context.getString(
                R.string.loan_product_interest_format,
                item.interestPercent.toString(),
            )
            tvTenor.text = context.getString(
                R.string.loan_product_tenor_format,
                item.tenorMinMonths,
                item.tenorMaxMonths,
            )
            tvPlafond.text = context.getString(
                R.string.loan_product_plafond_format,
                item.plafondMin.toRupiahDigits(),
                item.plafondMax.toRupiahDigits(),
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LoanProduct>() {
            override fun areItemsTheSame(oldItem: LoanProduct, newItem: LoanProduct) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: LoanProduct, newItem: LoanProduct) =
                oldItem == newItem
        }
    }
}
