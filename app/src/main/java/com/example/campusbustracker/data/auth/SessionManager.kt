package com.example.campusbustracker.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

object SessionManager {
    private val KEY_TOKEN = stringPreferencesKey("auth_token")
    private val KEY_ROLE = stringPreferencesKey("user_role")
    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_LAST_BUS_CODE = stringPreferencesKey("last_bus_code")
    private val KEY_REGISTERED_EMAILS = stringPreferencesKey("registered_emails")
    private val KEY_REGISTERED_DETAILS = stringPreferencesKey("registered_details") // email:name:role

    private val DEFAULT_ACCOUNTS = mapOf(
        "student@college.edu" to Pair("Student User", "student"),
        "driver@college.edu" to Pair("Campus Driver", "driver"),
        "parent@college.edu" to Pair("Parent Monitor", "parent"),
        "admin@college.edu" to Pair("Fleet Admin", "management"),
        "management@college.edu" to Pair("Fleet Admin", "management")
    )

    suspend fun saveSession(context: Context, token: String, role: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_ROLE] = role
            prefs[KEY_NAME] = name
        }
    }

    suspend fun addRegisteredAccount(context: Context, email: String, name: String, role: String) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()
        val trimmedRole = role.trim().lowercase()
        
        context.dataStore.edit { prefs ->
            val existingEmails = prefs[KEY_REGISTERED_EMAILS] ?: ""
            val emailSet = existingEmails.split(",").filter { it.isNotBlank() }.toMutableSet()
            emailSet.add(trimmedEmail)
            prefs[KEY_REGISTERED_EMAILS] = emailSet.joinToString(",")

            val existingDetails = prefs[KEY_REGISTERED_DETAILS] ?: ""
            val detailMap = existingDetails.split(";").filter { it.contains(":") }.associate {
                val parts = it.split(":")
                parts[0] to Pair(if (parts.size > 1) parts[1] else trimmedName, if (parts.size > 2) parts[2] else trimmedRole)
            }.toMutableMap()
            detailMap[trimmedEmail] = Pair(trimmedName, trimmedRole)
            prefs[KEY_REGISTERED_DETAILS] = detailMap.entries.joinToString(";") { "${it.key}:${it.value.first}:${it.value.second}" }
        }
    }

    suspend fun getRegisteredName(context: Context, email: String): String {
        val trimmedEmail = email.trim().lowercase()
        if (DEFAULT_ACCOUNTS.containsKey(trimmedEmail)) return DEFAULT_ACCOUNTS[trimmedEmail]!!.first

        val saved = context.dataStore.data.map { it[KEY_REGISTERED_DETAILS] }.firstOrNull() ?: ""
        val detailMap = saved.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to (if (parts.size > 1) parts[1] else "User")
        }
        return detailMap[trimmedEmail] ?: extractNameFromEmail(trimmedEmail)
    }

    suspend fun getRegisteredRole(context: Context, email: String): String {
        val trimmedEmail = email.trim().lowercase()
        if (DEFAULT_ACCOUNTS.containsKey(trimmedEmail)) return DEFAULT_ACCOUNTS[trimmedEmail]!!.second

        val saved = context.dataStore.data.map { it[KEY_REGISTERED_DETAILS] }.firstOrNull() ?: ""
        val detailMap = saved.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to (if (parts.size > 2) parts[2] else "student")
        }
        return detailMap[trimmedEmail] ?: when {
            trimmedEmail.contains("driver") -> "driver"
            trimmedEmail.contains("admin") || trimmedEmail.contains("management") -> "management"
            trimmedEmail.contains("parent") -> "parent"
            else -> "student"
        }
    }

    suspend fun isAccountRegistered(context: Context, email: String): Boolean {
        val trimmed = email.trim().lowercase()
        if (DEFAULT_ACCOUNTS.containsKey(trimmed)) return true
        val saved = context.dataStore.data.map { it[KEY_REGISTERED_EMAILS] }.firstOrNull() ?: ""
        val registeredList = saved.split(",").filter { it.isNotBlank() }.toSet()
        return registeredList.contains(trimmed)
    }

    suspend fun saveLastBusCode(context: Context, busCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_BUS_CODE] = busCode
        }
    }

    fun getLastBusCode(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_LAST_BUS_CODE] }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_NAME)
        }
    }

    fun getRole(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_ROLE] }

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_TOKEN] }

    fun getName(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_NAME] }

    fun isLoggedIn(context: Flow<Boolean>): Flow<Boolean> = context

    fun isLoggedIn(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_TOKEN] != null }

    private fun extractNameFromEmail(email: String): String {
        val part = email.split("@").firstOrNull() ?: "User"
        return part.replace(".", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}