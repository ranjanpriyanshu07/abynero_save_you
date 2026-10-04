package com.phoneguard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.ui.navigation.NavigationRequests
import com.phoneguard.ui.navigation.PendingNavigation
import com.phoneguard.ui.navigation.PhoneGuardRoot
import com.phoneguard.ui.theme.PhoneGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The only Activity. Hosts the Compose UI and turns incoming intents (notification taps and
 * text shared from other apps) into navigation requests.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigationRequests: NavigationRequests

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            PhoneGuardTheme {
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Wait until we know whether onboarding is done; this takes milliseconds.
                    onboardingCompleted?.let { done ->
                        PhoneGuardRoot(startWithOnboarding = !done, navigationRequests = navigationRequests)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when {
            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return
                navigationRequests.submit(PendingNavigation.SharedText(text.take(MAX_SHARED_TEXT)))
            }
            intent.getStringExtra(EXTRA_OPEN) == OPEN_SCAN -> navigationRequests.submit(PendingNavigation.OpenScan)
            intent.getStringExtra(EXTRA_OPEN) == OPEN_APP -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
                navigationRequests.submit(PendingNavigation.OpenApp(packageName))
            }
        }
    }

    companion object {
        private const val EXTRA_OPEN = "com.phoneguard.extra.OPEN"
        private const val EXTRA_PACKAGE = "com.phoneguard.extra.PACKAGE"
        private const val OPEN_SCAN = "scan"
        private const val OPEN_APP = "app"
        private const val MAX_SHARED_TEXT = 5_000

        /** Intent that opens the scan results (used by notifications). */
        fun openScanIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_SCAN)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** Intent that opens one app's details (used by notifications). */
        fun openAppIntent(context: Context, packageName: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_APP)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
