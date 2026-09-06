package com.masesas.exercise.bcaf_test_1.core

// Akses global ViewModel digantikan Hilt (@HiltViewModel + activityViewModels).
//
//import android.content.Context
//import androidx.activity.ComponentActivity
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.masesas.exercise.bcaf_test_1.BcafApplication
//
//val Context.globalViewModelStoreOwner: BcafApplication
//    get() = checkNotNull(applicationContext as? BcafApplication) {
//        "Application bukan BcafApplication. Tambahkan " +
//            "android:name=\".BcafApplication\" pada <application> di AndroidManifest.xml."
//    }
//
//fun <VM : ViewModel> Context.requireGlobalViewModel(modelClass: Class<VM>): VM {
//    val owner = globalViewModelStoreOwner
//    return ViewModelProvider.create(owner).get(modelClass)
//}
//
//inline fun <reified VM : ViewModel> ComponentActivity.globalViewModels(): Lazy<VM> {
//    val modelClass = VM::class.java
//    return lazy(LazyThreadSafetyMode.NONE) { requireGlobalViewModel(modelClass) }
//}
//
//inline fun <reified VM : ViewModel> Fragment.globalViewModels(): Lazy<VM> {
//    val modelClass = VM::class.java
//    return lazy(LazyThreadSafetyMode.NONE) { requireContext().requireGlobalViewModel(modelClass) }
//}
//
//@Composable
//inline fun <reified VM : ViewModel> globalViewModel(key: String? = null): VM {
//    val owner = LocalContext.current.globalViewModelStoreOwner
//    return viewModel(viewModelStoreOwner = owner, key = key)
//}
