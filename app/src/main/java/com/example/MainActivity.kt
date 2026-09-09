package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.RefluxViewModel
import com.example.ui.navigation.RefluxNavHost
import com.example.ui.theme.RefluxTrackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RefluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RefluxTrackTheme {
                RefluxNavHost(viewModel = viewModel)
            }
        }
    }
}
