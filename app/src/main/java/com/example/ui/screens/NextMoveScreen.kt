package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Action
import com.example.ui.NextMoveViewModel

@Composable
fun NextMoveScreen(viewModel: NextMoveViewModel, action: Action?) {
    var showSplitDialog by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }
    var parkedThought by remember { mutableStateOf("") }

    var showEveningReview by remember { mutableStateOf(false) }

    if (isFocusMode && action != null) {
        // ... (keep previous isFocusMode implementation)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Focusing on",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = action.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            
            OutlinedTextField(
                value = parkedThought,
                onValueChange = { parkedThought = it },
                label = { Text("Park a thought...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = { 
                    viewModel.handleActionDecision(action, "completed")
                    isFocusMode = false
                    parkedThought = ""
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Complete", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { isFocusMode = false },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Stop Focus")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showEveningReview = true }) {
                Text("Evening Review")
            }
        }

        
        if (action == null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "All done",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You're all caught up.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No ready actions found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.setupSampleData() }) {
                Text("Load Sample Data")
            }
        } else {
            // Context Label
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Recommended for your current energy & time",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // The Action Card
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = action.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${action.estimatedDurationMins}m • Energy: ${action.energyDemand}/3",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Action
            Button(
                onClick = { isFocusMode = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Focus Session", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { showSplitDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Too big")
                }
                FilledTonalButton(
                    onClick = { viewModel.handleActionDecision(action, "wrong_context") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Not now")
                }
            }
        }
    }

    if (showSplitDialog && action != null) {
        SplitActionDialog(
            action = action,
            onDismiss = { showSplitDialog = false },
            onSplit = { p1, p2, d1, d2 ->
                viewModel.handleTooBig(action, p1, p2, d1, d2)
                showSplitDialog = false
            }
        )
    }

    if (showEveningReview) {
        EveningReviewDialog(
            onDismiss = { showEveningReview = false },
            onComplete = { showEveningReview = false }
        )
    }
}

@Composable
fun EveningReviewDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var completedText by remember { mutableStateOf("") }
    var blockedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close-and-Learn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What was completed?", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = completedText, onValueChange = { completedText = it }, maxLines = 2)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("What blocked unfinished work?", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = blockedText, onValueChange = { blockedText = it }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = onComplete) {
                Text("Save & Close Day")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SplitActionDialog(
    action: Action,
    onDismiss: () -> Unit,
    onSplit: (String, String, Int, Int) -> Unit
) {
    var part1 by remember { mutableStateOf(action.name + " (Part 1)") }
    var part2 by remember { mutableStateOf(action.name + " (Part 2)") }
    var dur1 by remember { mutableStateOf((action.estimatedDurationMins / 2).toString()) }
    var dur2 by remember { mutableStateOf((action.estimatedDurationMins / 2).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = part1, onValueChange = { part1 = it }, label = { Text("Part 1") })
                OutlinedTextField(value = dur1, onValueChange = { dur1 = it }, label = { Text("Duration (m)") })
                OutlinedTextField(value = part2, onValueChange = { part2 = it }, label = { Text("Part 2") })
                OutlinedTextField(value = dur2, onValueChange = { dur2 = it }, label = { Text("Duration (m)") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSplit(part1, part2, dur1.toIntOrNull() ?: 15, dur2.toIntOrNull() ?: 15) }) {
                Text("Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
