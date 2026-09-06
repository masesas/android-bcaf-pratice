package com.masesas.exercise.bcaf_test_1.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiMessage(
    @param:StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
)

fun Context.resolve(message: UiMessage): String =
    getString(message.resId, *message.formatArgs.toTypedArray())

@Composable
fun UiMessage.asString(): String =
    stringResource(resId, *formatArgs.toTypedArray())
