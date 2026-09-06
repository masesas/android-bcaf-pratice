package com.masesas.exercise.bcaf_test_1.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectOnLifecycle(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit,
): Job = owner.lifecycleScope.launch {
    owner.lifecycle.repeatOnLifecycle(minActiveState) {
        collect { value -> collector(value) }
    }
}
