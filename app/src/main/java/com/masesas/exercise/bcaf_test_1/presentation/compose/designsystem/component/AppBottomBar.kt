package com.masesas.exercise.bcaf_test_1.presentation.compose.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.masesas.exercise.bcaf_test_1.presentation.compose.navigation.TopLevelDestination

@Composable
fun AppBottomBar(
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            val label = stringResource(destination.labelRes)

            NavigationBarItem(
                selected = isSelected(destination),
                onClick = { onSelect(destination) },
                icon = {
                    Icon(painter = painterResource(destination.iconRes), contentDescription = label)
                },
                label = { Text(label) },
            )
        }
    }
}
