package com.omega.ordencompra.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omega.ordencompra.data.db.entities.UserEntity
import com.omega.ordencompra.data.firebase.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow(false)
    val loginError: StateFlow<Boolean> = _loginError.asStateFlow()

    val users: StateFlow<List<UserEntity>> = repo.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var savedUsername = sharedPrefs.getString("username", "") ?: ""
    var savedPassword = sharedPrefs.getString("password", "") ?: ""
    var savedRememberMe = sharedPrefs.getBoolean("remember_me", false)

    init {
        viewModelScope.launch {
            repo.getUserByUsername("admin").onSuccess { user ->
                if (user == null) {
                    repo.insertUser(UserEntity(username = "admin", password = "admin", rol = "admin", nombreCompleto = "Administrador"))
                }
            }
        }
        // Auto-login on startup if remember me is active
        if (savedRememberMe && savedUsername.isNotBlank() && savedPassword.isNotBlank()) {
            login(savedUsername, savedPassword, true)
        }
    }

    fun loadUsers() {} // Auto-collected via init

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            repo.login(username, password).onSuccess { user ->
                if (user != null) {
                    _currentUser.value = user
                    _loginError.value = false
                    if (rememberMe) {
                        sharedPrefs.edit()
                            .putString("username", username)
                            .putString("password", password)
                            .putBoolean("remember_me", true)
                            .apply()
                        savedUsername = username
                        savedPassword = password
                        savedRememberMe = true
                    } else {
                        sharedPrefs.edit().clear().apply()
                        savedUsername = ""
                        savedPassword = ""
                        savedRememberMe = false
                    }
                } else {
                    _loginError.value = true
                }
            }.onFailure {
                _loginError.value = true
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        // Clear remember me on explicit logout
        sharedPrefs.edit().clear().apply()
        savedUsername = ""
        savedPassword = ""
        savedRememberMe = false
    }

    fun isAdmin(): Boolean = _currentUser.value?.rol == "admin"

    fun insertUser(user: UserEntity) {
        viewModelScope.launch { repo.insertUser(user).onFailure { it.printStackTrace() } }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch { repo.updateUser(user).onFailure { it.printStackTrace() } }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch { repo.deleteUser(user).onFailure { it.printStackTrace() } }
    }
}
