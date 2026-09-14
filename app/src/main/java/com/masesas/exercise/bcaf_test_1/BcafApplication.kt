package com.masesas.exercise.bcaf_test_1

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.masesas.exercise.bcaf_test_1.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// DI manual di bawah ini digantikan Hilt.
//import androidx.lifecycle.HasDefaultViewModelProviderFactory
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.ViewModelStore
//import androidx.lifecycle.ViewModelStoreOwner
//import androidx.lifecycle.viewmodel.CreationExtras
//import androidx.lifecycle.viewmodel.MutableCreationExtras
//import com.masesas.exercise.bcaf_test_1.core.AppContainer
//import com.masesas.exercise.bcaf_test_1.core.DefaultAppContainer
//import com.masesas.exercise.bcaf_test_1.core.globalViewModelFactory

@HiltAndroidApp
class BcafApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodicSync()
    }
}
//    ViewModelStoreOwner,
//    HasDefaultViewModelProviderFactory {
//
//    val container: AppContainer by lazy { DefaultAppContainer(this) }
//
//    override val viewModelStore: ViewModelStore = ViewModelStore()
//
//    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
//        globalViewModelFactory(container)
//    }
//
//    override val defaultViewModelCreationExtras: CreationExtras
//        get() = MutableCreationExtras().apply {
//            set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, this@BcafApplication)
//        }
//}
