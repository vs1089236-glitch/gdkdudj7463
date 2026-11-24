package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hoyoDataStore: DataStore<Preferences> by preferencesDataStore(name = "hoyo_pay_prefs")

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val pendingReferralCode: String = "",
    val pendingMobileNumber: String = ""
)

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.hoyoDataStore

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PENDING_REFERRAL_CODE = stringPreferencesKey("pending_referral_code")
        val PENDING_MOBILE = stringPreferencesKey("pending_mobile")
    }

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        val langRaw = prefs[Keys.LANGUAGE] ?: AppLanguage.EN.name
        val themeRaw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        AppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            language = enumValues<AppLanguage>().firstOrNull { it.name == langRaw } ?: AppLanguage.EN,
            themeMode = enumValues<ThemeMode>().firstOrNull { it.name == themeRaw } ?: ThemeMode.SYSTEM,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            pendingReferralCode = prefs[Keys.PENDING_REFERRAL_CODE] ?: "",
            pendingMobileNumber = prefs[Keys.PENDING_MOBILE] ?: ""
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun savePendingAuthInputs(referralCode: String, mobileNumber: String) {
        dataStore.edit {
            it[Keys.PENDING_REFERRAL_CODE] = referralCode.trim().uppercase()
            it[Keys.PENDING_MOBILE] = mobileNumber.trim()
        }
    }

    suspend fun clearPendingAuthInputs() {
        dataStore.edit {
            it.remove(Keys.PENDING_REFERRAL_CODE)
            it.remove(Keys.PENDING_MOBILE)
        }
    }
}
