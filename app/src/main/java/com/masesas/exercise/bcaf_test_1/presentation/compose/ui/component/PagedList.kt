package com.masesas.exercise.bcaf_test_1.presentation.compose.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.core.ui.asString
import com.masesas.exercise.bcaf_test_1.core.ui.toUiMessage
import com.masesas.exercise.bcaf_test_1.domain.common.AppFailure
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 3

/** Memicu load more saat item terakhir yang terlihat mendekati ujung daftar. */
@Composable
fun LoadMoreEffect(
    listState: LazyListState,
    enabled: Boolean,
    onLoadMore: () -> Unit,
) {
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 1 - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}

@Composable
fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Error dan empty state di atas daftar kosong; loading refresh dipegang PullToRefreshBox. */
@Composable
fun BoxScope.ListStatusOverlay(
    failure: AppFailure?,
    isEmpty: Boolean,
    emptyMessage: String,
    onRetry: () -> Unit,
) {
    when {
        failure != null -> Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = failure.toUiMessage().asString())
            Button(onClick = onRetry) { Text(text = stringResource(R.string.action_retry)) }
        }

        isEmpty -> Text(
            text = emptyMessage,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
        )
    }
}
