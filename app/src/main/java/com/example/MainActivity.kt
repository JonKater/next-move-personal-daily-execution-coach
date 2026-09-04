package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.NextMoveViewModel
import com.example.ui.NextMoveViewModelFactory
import com.example.ui.screens.DailyCompassScreen
import com.example.ui.screens.NextMoveScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    
    private val viewModel: NextMoveViewModel by viewModels {
        NextMoveViewModelFactory((application as NextMoveApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val dailyContext by viewModel.dailyContext.collectAsState()
                    val topAction by viewModel.topAction.collectAsState()
                    
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val needsCompass = dailyContext == null || dailyContext!!.dateMs < todayStart

                    Modifier.padding(innerPadding).let { mod ->
                        if (needsCompass) {
                            DailyCompassScreen(viewModel)
                        } else {
                            NextMoveScreen(viewModel, topAction)
                        }
                    }
                }
            }
        }
    }
}
