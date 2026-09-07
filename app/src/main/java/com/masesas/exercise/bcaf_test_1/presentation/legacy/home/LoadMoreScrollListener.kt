package com.masesas.exercise.bcaf_test_1.presentation.legacy.home

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Memicu [onLoadMore] saat sisa item di bawah viewport tinggal [threshold]. */
class LoadMoreScrollListener(
    private val threshold: Int = DEFAULT_THRESHOLD,
    private val onLoadMore: () -> Unit,
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy <= 0) return

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (lastVisible == RecyclerView.NO_POSITION) return

        if (lastVisible >= layoutManager.itemCount - 1 - threshold) onLoadMore()
    }

    private companion object {
        const val DEFAULT_THRESHOLD = 3
    }
}
