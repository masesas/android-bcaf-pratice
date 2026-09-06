package com.masesas.exercise.bcaf_test_1.presentation.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.AppRoot
import com.masesas.exercise.bcaf_test_1.presentation.compose.ui.theme.MyBcafTest1Theme

class HomeActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBcafTest1Theme {
                AppRoot()
            }
        }
    }
}