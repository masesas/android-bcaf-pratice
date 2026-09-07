package com.masesas.exercise.bcaf_test_1.presentation.compose.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner

/**
 * ViewModel yang di-scope ke Activity, bukan ke NavBackStackEntry.
 *
 * Instansinya sama dengan yang didapat fragment legacy lewat `activityViewModels()`.
 */
@Composable
inline fun <reified VM : ViewModel> sharedActivityViewModel(): VM {
    val owner = checkNotNull(LocalActivity.current as? ViewModelStoreOwner) {
        "Composable harus berjalan di dalam ComponentActivity."
    }
    return hiltViewModel(owner)
}
