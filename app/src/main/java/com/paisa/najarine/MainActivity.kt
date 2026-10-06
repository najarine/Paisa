package com.paisa.najarine

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.paisa.najarine.ui.PaisaApp
import com.paisa.najarine.ui.PaisaViewModel
import com.paisa.najarine.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private val viewModel: PaisaViewModel by viewModels()
    private val pendingTargetScreen = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingTargetScreen.value = intent?.getStringExtra("EXTRA_TARGET_SCREEN")

        // Initialize Services
        com.paisa.najarine.notification.AdhanPreferences.init(this)
        com.paisa.najarine.notification.PaisaNotificationManager.createNotificationChannels(this)
        com.paisa.najarine.notification.HourlyIslamicScheduler.scheduleHourlySync(this)
        com.paisa.najarine.notification.HourlyIslamicScheduler.triggerImmediateSync(this)

        setContent {
            MyApplicationTheme {
                PaisaApp(
                    viewModel = viewModel,
                    activity = this,
                    targetScreenName = pendingTargetScreen.value,
                    onTargetScreenHandled = { pendingTargetScreen.value = null }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.securityManager.lock()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("EXTRA_TARGET_SCREEN")?.let {
            pendingTargetScreen.value = it
        }
    }
}
