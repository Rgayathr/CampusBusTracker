package com.example.campusbustracker.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbustracker.data.auth.SessionManager
import com.example.campusbustracker.network.ApiClient
import com.example.campusbustracker.network.LoginRequest
import com.example.campusbustracker.network.SignupRequest
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AuthViewModel : ViewModel() {
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    // Strict Email Regex
    private val EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

    // Strong Password Regex: Min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character
    private val PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")

    fun login(context: Context, email: String, password: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            errorMessage.value = "Please enter your email and password."
            return
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            errorMessage.value = "Please enter a valid email address (e.g. driver@college.edu)."
            return
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errorMessage.value = "Password must be at least 8 characters long and include an uppercase letter, lowercase letter, number, and special character (!@#$%^&*)."
            return
        }

        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {
            val isRegistered = SessionManager.isAccountRegistered(context, trimmedEmail)
            val userRole = SessionManager.getRegisteredRole(context, trimmedEmail)
            val userName = SessionManager.getRegisteredName(context, trimmedEmail)
            try {
                val response = ApiClient.api.login(LoginRequest(trimmedEmail, password))
                val body = response.body()
                if (response.isSuccessful && body?.token != null) {
                    val finalRole = body.role ?: userRole
                    val finalName = body.name ?: userName
                    SessionManager.addRegisteredAccount(context, trimmedEmail, finalName, finalRole)
                    SessionManager.saveSession(
                        context,
                        token = body.token,
                        role = finalRole,
                        name = finalName
                    )
                    onSuccess()
                } else if (isRegistered) {
                    saveLocalDemoSession(context, trimmedEmail, userName, userRole, onSuccess)
                } else {
                    errorMessage.value = "Account not found. Please Sign Up first to create your account."
                }
            } catch (e: Exception) {
                if (isRegistered) {
                    saveLocalDemoSession(context, trimmedEmail, userName, userRole, onSuccess)
                } else {
                    errorMessage.value = "Account not found. Please Sign Up first to create your account."
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signup(
        context: Context,
        name: String,
        email: String,
        password: String,
        role: String,
        onSuccess: () -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val chosenRole = role.trim().lowercase()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            errorMessage.value = "Please fill in all required fields."
            return
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            errorMessage.value = "Please enter a valid email address (e.g. student@college.edu)."
            return
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errorMessage.value = "Password must be at least 8 characters long and include an uppercase letter, lowercase letter, number, and special character (!@#$%^&*)."
            return
        }

        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {
            SessionManager.addRegisteredAccount(context, trimmedEmail, trimmedName, chosenRole)
            try {
                val response = ApiClient.api.signup(SignupRequest(trimmedName, trimmedEmail, password, chosenRole))
                val body = response.body()
                if (response.isSuccessful && body?.token != null) {
                    SessionManager.saveSession(
                        context,
                        token = body.token,
                        role = body.role ?: chosenRole,
                        name = body.name ?: trimmedName
                    )
                    onSuccess()
                } else {
                    saveLocalDemoSession(context, trimmedEmail, trimmedName, chosenRole, onSuccess)
                }
            } catch (e: Exception) {
                saveLocalDemoSession(context, trimmedEmail, trimmedName, chosenRole, onSuccess)
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun saveLocalDemoSession(
        context: Context,
        email: String,
        name: String,
        role: String,
        onSuccess: () -> Unit
    ) {
        SessionManager.saveSession(
            context,
            token = "jwt_token_${System.currentTimeMillis()}",
            role = role,
            name = name
        )
        onSuccess()
    }
}