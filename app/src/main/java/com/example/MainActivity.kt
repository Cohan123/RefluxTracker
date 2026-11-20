package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminder.ReminderScheduler
import com.example.reminder.ReminderSettings
import com.example.ui.RefluxViewModel
import com.example.ui.navigation.RefluxNavHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    restoreReminderIfEnabled()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val app = application
          val viewModel: RefluxViewModel = viewModel(
            factory = RefluxViewModel.provideFactory(app)
          )
          RefluxNavHost(viewModel = viewModel)
        }
      }
    }
  }

  /**
   * Safety net for cases where BOOT_COMPLETED was not delivered (for example
   * when the app was force-stopped before the reboot).
   */
  private fun restoreReminderIfEnabled() {
    val settings = ReminderSettings(this)
    if (!settings.isEnabled()) return
    val time = settings.getReminderTime() ?: return
    ReminderScheduler.schedule(this, time.hourOfDay, time.minute)
  }
}
