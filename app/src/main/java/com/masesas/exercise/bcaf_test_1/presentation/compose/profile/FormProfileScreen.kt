package com.masesas.exercise.bcaf_test_1.presentation.compose.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component.PlaceholderScreen

@Composable
fun FormProfileScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PlaceholderScreen(
            modifier = Modifier.padding(innerPadding),
            title = stringResource(
                R.string.screen_title_format,
                stringResource(R.string.title_form_profile),
            ),
        )
    }
}
