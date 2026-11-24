package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.localization.stringsFor
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.HoyoPayTheme
import com.example.utils.BiometricHelper
import com.example.utils.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)

        setContent {
            HoyoPayApp(activity = this)
        }
    }
}

@Composable
fun HoyoPayApp(activity: FragmentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { UserPreferencesRepository(context) }
    val prefs by prefsRepo.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val strings = remember(prefs.language) { stringsFor(prefs.language) }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { user -> currentUser = user },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    var biometricUnlocked by rememberSaveable { mutableStateOf(false) }

    HoyoPayTheme(themeMode = prefs.themeMode) {
        if (!prefs.onboardingCompleted) {
            OnboardingScreen(
                strings = strings,
                currentLanguage = prefs.language,
                onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } },
                onFinish = { scope.launch { prefsRepo.setOnboardingCompleted(true) } }
            )
        } else if (currentUser == null) {
            biometricUnlocked = false
            AuthScreen(
                strings = strings,
                currentLanguage = prefs.language,
                initialReferralCode = prefs.pendingReferralCode,
                initialMobile = prefs.pendingMobileNumber,
                onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } },
                onSavePreAuthInputs = { code, mobile ->
                    scope.launch { prefsRepo.savePendingAuthInputs(code, mobile) }
                },
                onAuthSuccess = { user ->
                    biometricUnlocked = true
                    currentUser = user
                }
            )
        } else {
            val uid = currentUser!!.uid
            val needsBiometric = prefs.biometricLockEnabled &&
                !biometricUnlocked &&
                BiometricHelper.canAuthenticate(activity)

            if (needsBiometric) {
                BiometricLockScreen(
                    strings = strings,
                    onUnlockClick = {
                        BiometricHelper.authenticate(
                            activity = activity,
                            title = strings.lockTitle,
                            subtitle = strings.lockSubtitle,
                            onSuccess = { biometricUnlocked = true },
                            onError = {}
                        )
                    },
                    onSignOutClick = {
                        signOut(credentialManager, scope) {
                            currentUser = null
                        }
                    }
                )
            } else {
                key(uid) {
                    MainAppScaffold(
                        currentUserId = uid,
                        prefs = prefs,
                        prefsRepo = prefsRepo,
                        strings = strings,
                        onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } },
                        onThemeChange = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                        onBiometricToggle = { enabled ->
                            scope.launch { prefsRepo.setBiometricLockEnabled(enabled) }
                        },
                        onNotificationsToggle = { enabled ->
                            scope.launch { prefsRepo.setNotificationsEnabled(enabled) }
                        },
                        onOpenTutorial = {
                            scope.launch { prefsRepo.setOnboardingCompleted(false) }
                        },
                        onSignOut = {
                            signOut(credentialManager, scope) {
                                biometricUnlocked = false
                                currentUser = null
                            }
                        }
                    )
                }
            }
        }
    }
}
