package com.masesas.exercise.bcaf_test_1.presentation.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = modifier
                .padding(10.dp),
        ) {
            Text("Hello World from Welcome Screen Compose")
        }

        Surface(
            modifier = modifier.padding(10.dp)
        ) {
            Text("Hello World from Welcome Screen Compose")

        }

        Surface(
            modifier = modifier.padding(10.dp)
        ) {
            Text("Hello World from Welcome Screen Compose")

        }
    }
}