package com.masesas.exercise.bcaf_test_1.presentation.legacy

import android.content.Context
import androidx.annotation.StringRes
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.auth.AuthUiState

/** Judul layar + ringkasan session, dipakai semua fragment legacy. */
internal fun AuthUiState.summaryText(context: Context, @StringRes titleRes: Int): String =
    summaryText(context, context.getString(titleRes))

internal fun AuthUiState.summaryText(context: Context, title: String): String =
    context.getString(
        R.string.screen_session_summary,
        title,
        status.name,
        user?.email ?: context.getString(R.string.session_user_none),
    )
