package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.NextMoveViewModel

@Composable
fun DailyCompassScreen(viewModel: NextMoveViewModel) {
    var timeSlider by remember { mutableStateOf(60f) }
    var energySlider by remember { mutableStateOf(2f) }
    var availableContext by remember { mutableStateOf("Anywhere") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Morning Compass",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set your daily baseline.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Usable Time: ${timeSlider.toInt()} mins", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = timeSlider,
                    onValueChange = { timeSlider = it },
                    valueRange = 15f..240f,
                    steps = 14
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val energyLabel = when(energySlider.toInt()) {
                    1 -> "Low (Brain Dead)"
                    2 -> "Medium (Normal)"
                    else -> "High (Deep Focus)"
                }
                Text(text = "Energy: $energyLabel", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = energySlider,
                    onValueChange = { energySlider = it },
                    valueRange = 1f..3f,
                    steps = 1
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "Available context", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val contexts = listOf("Anywhere", "Computer", "Phone", "Errands")
                    contexts.forEachIndexed { index, context ->
                        SegmentedButton(
                            selected = availableContext == context,
                            onClick = { availableContext = context },
                            shape = SegmentedButtonDefaults.itemShape(index, contexts.size),
                            label = { Text(context) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { 
                viewModel.submitDailyCompass(timeSlider.toInt(), energySlider.toInt(), availableContext)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_compass_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Set Compass", style = MaterialTheme.typography.titleMedium)
        }
    }
}
