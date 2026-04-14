package com.money.codex.data

import android.content.Context

object AuthSessionStore {
    private const val PREF_NAME = "money_auth_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_REMEMBER_EMAIL = "remember_email"
    private const val KEY_REMEMBER_PASSWORD = "remember_password"
    private const val KEY_REMEMBER_ENABLED = "remember_password_enabled"

    @Volatile
    private var initialized = false
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (initialized) {
            return
        }
        appContext = context.applicationContext
        initialized = true
    }

    private fun prefs() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val token: String?
        get() = if (initialized) prefs().getString(KEY_TOKEN, null) else null

    fun currentUser(): UserInfo? {
        if (!initialized) {
            return null
        }
        val userId = prefs().getLong(KEY_USER_ID, -1L)
        val email = prefs().getString(KEY_EMAIL, null)
        val nickname = prefs().getString(KEY_NICKNAME, null)
        if (userId <= 0L || email.isNullOrBlank() || nickname.isNullOrBlank()) {
            return null
        }
        return UserInfo(userId, email, nickname)
    }

    fun saveSession(token: String, userInfo: UserInfo) {
        prefs().edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userInfo.id)
            .putString(KEY_EMAIL, userInfo.email)
            .putString(KEY_NICKNAME, userInfo.nickname)
            .apply()
    }

    fun saveRememberedCredentials(email: String, password: String, enabled: Boolean) {
        if (!initialized) {
            return
        }
        if (enabled) {
            prefs().edit()
                .putBoolean(KEY_REMEMBER_ENABLED, true)
                .putString(KEY_REMEMBER_EMAIL, email)
                .putString(KEY_REMEMBER_PASSWORD, password)
                .apply()
        } else {
            clearRememberedCredentials()
        }
    }

    fun rememberedEmail(): String {
        if (!initialized) {
            return ""
        }
        return prefs().getString(KEY_REMEMBER_EMAIL, "").orEmpty()
    }

    fun rememberedPassword(): String {
        if (!initialized) {
            return ""
        }
        return prefs().getString(KEY_REMEMBER_PASSWORD, "").orEmpty()
    }

    fun isRememberPasswordEnabled(): Boolean {
        if (!initialized) {
            return false
        }
        val hasLegacyCredentials = rememberedEmail().isNotBlank() || rememberedPassword().isNotBlank()
        return prefs().getBoolean(KEY_REMEMBER_ENABLED, hasLegacyCredentials)
    }

    fun setRememberPasswordEnabled(enabled: Boolean) {
        if (!initialized) {
            return
        }
        prefs().edit()
            .putBoolean(KEY_REMEMBER_ENABLED, enabled)
            .apply()
        if (!enabled) {
            clearRememberedCredentials()
        }
    }

    fun clearRememberedCredentials() {
        if (!initialized) {
            return
        }
        prefs().edit()
            .putBoolean(KEY_REMEMBER_ENABLED, false)
            .remove(KEY_REMEMBER_EMAIL)
            .remove(KEY_REMEMBER_PASSWORD)
            .apply()
    }

    fun clearSession() {
        if (!initialized) {
            return
        }
        prefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_NICKNAME)
            .apply()
    }
}
