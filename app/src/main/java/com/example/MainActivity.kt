package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.FirestoreGhestYarRepository
import com.example.data.NetworkConnectivityMonitor
import com.example.data.ThemePreferencesRepository
import com.example.ui.GhestYarApp
import com.example.ui.GhestYarViewModel
import com.example.ui.theme.GhestYarTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePrefs = ThemePreferencesRepository(applicationContext)

        setContent {
            val isDarkTheme by themePrefs.isDarkTheme.collectAsStateWithLifecycle(initialValue = false)

            GhestYarTheme(darkTheme = isDarkTheme) {
                GhestYarRoot(
                    auth = Firebase.auth,
                    themePrefs = themePrefs
                )
            }
        }
    }
}

@Composable
private fun GhestYarRoot(
    auth: FirebaseAuth,
    themePrefs: ThemePreferencesRepository
) {
    val user by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val currentUser = user!!
        key(currentUser.uid) {
            val appContext = context.applicationContext
            val vm: GhestYarViewModel = viewModel(
                key = currentUser.uid,
                factory = viewModelFactory {
                    initializer {
                        GhestYarViewModel(
                            repository = FirestoreGhestYarRepository(appContext),
                            themePrefs = themePrefs,
                            networkMonitor = NetworkConnectivityMonitor(appContext),
                            currentUserId = currentUser.uid
                        )
                    }
                }
            )
            GhestYarApp(
                viewModel = vm,
                userEmail = currentUser.email,
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}
