package app.luzzy.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsManager {

    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_TOKEN_REGISTERED = "token_registered"
    private const val KEY_CURRENT_TOKEN = "current_token"
    private const val KEY_REGISTRATION_DATE = "registration_date"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_GOOGLE_AUTH_TOKEN = "google_auth_token"
    private const val KEY_PHONE_NUMBER = "phone_number"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTokenRegistered(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOKEN_REGISTERED, false)
    }

    fun setTokenRegistered(context: Context, registered: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_TOKEN_REGISTERED, registered)
            .putLong(KEY_REGISTRATION_DATE, System.currentTimeMillis())
            .apply()
    }

    fun getCurrentToken(context: Context): String? {
        return getPrefs(context).getString(KEY_CURRENT_TOKEN, null)
    }

    fun saveCurrentToken(context: Context, token: String) {
        getPrefs(context).edit()
            .putString(KEY_CURRENT_TOKEN, token)
            .apply()
    }

    fun getRegistrationDate(context: Context): Long {
        return getPrefs(context).getLong(KEY_REGISTRATION_DATE, 0L)
    }

    fun clearTokenData(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_TOKEN_REGISTERED)
            .remove(KEY_CURRENT_TOKEN)
            .remove(KEY_REGISTRATION_DATE)
            .apply()
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun saveAuthToken(context: Context, token: String) {
        getPrefs(context).edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun saveGoogleAuthToken(context: Context, token: String) {
        getPrefs(context).edit()
            .putString(KEY_GOOGLE_AUTH_TOKEN, token)
            .apply()
    }

    fun getGoogleAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_GOOGLE_AUTH_TOKEN, null)
    }

    fun clearGoogleAuthToken(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_GOOGLE_AUTH_TOKEN)
            .apply()
    }

    // Prioriza el token de Google (email) sobre el token de dispositivo (teléfono)
    fun getAuthHeader(context: Context): String? {
        val googleToken = getGoogleAuthToken(context)?.takeIf { it.isNotBlank() }
        val token = googleToken ?: getAuthToken(context)
        return if (token != null) "Bearer $token" else null
    }

    fun areNotificationsDisabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("notifications_disabled", false)
    }

    fun setNotificationsDisabled(context: Context, disabled: Boolean) {
        getPrefs(context).edit().putBoolean("notifications_disabled", disabled).apply()
    }

    fun savePhoneNumber(context: Context, phone: String) {
        getPrefs(context).edit()
            .putString(KEY_PHONE_NUMBER, phone)
            .apply()
    }

    fun getPhoneNumber(context: Context): String? {
        return getPrefs(context).getString(KEY_PHONE_NUMBER, null)
    }

    fun saveAccountActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean("luzzy_account_active", active).apply()
    }

    fun isAccountActive(context: Context): Boolean {
        return getPrefs(context).getBoolean("luzzy_account_active", false)
    }

    fun isGlobalDraftModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("global_draft_mode", false)
    }

    fun setGlobalDraftModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("global_draft_mode", enabled).apply()
    }
}
