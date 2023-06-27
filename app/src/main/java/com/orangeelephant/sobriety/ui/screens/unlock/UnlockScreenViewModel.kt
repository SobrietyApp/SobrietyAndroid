package com.orangeelephant.sobriety.ui.screens.unlock

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangeelephant.sobriety.ApplicationDependencies
import com.orangeelephant.sobriety.storage.database.SqlCipherKey
import com.orangeelephant.sobriety.util.SobrietyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class UnlockScreenViewModel @Inject constructor(
    @ApplicationContext app: Context
): ViewModel() {
    private val preferences = SobrietyPreferences(context = app)

    val loadingValues = mutableStateOf(true)
    val retrievingKey = mutableStateOf(false)
    val cipherKeyLoaded = mutableStateOf(false)

    val fingerprintUnlockEnabled = mutableStateOf(false)
    val encrypted = mutableStateOf(false)

    init {
        viewModelScope.launch {
            fingerprintUnlockEnabled.value = preferences.biometricUnlock.first()
            encrypted.value = preferences.encryptedByPassword.first()

            if (!encrypted.value && !fingerprintUnlockEnabled.value) {
                ApplicationDependencies.setSqlcipherKey(SqlCipherKey(isEncrypted = false))
                cipherKeyLoaded.value = true
            }

            loadingValues.value = false
        }
    }

    fun onSubmitPassword(password: String?) {
        viewModelScope.launch(Dispatchers.Default) {
            retrievingKey.value = true

            if (!encrypted.value) {
                ApplicationDependencies.setSqlcipherKey(SqlCipherKey(isEncrypted = false))
                cipherKeyLoaded.value = true
            } else if (encrypted.value && password == null) {
                println("Must provide password if DB is encrypted")
            } else {
                val salt = Base64.decode(preferences.passwordSalt.first(), Base64.DEFAULT)
                val key = SqlCipherKey(
                    isEncrypted = true,
                    password = password!!.encodeToByteArray(),
                    salt = salt
                )

                if (ApplicationDependencies.getDatabase().keyIsCorrect(key)) {
                    ApplicationDependencies.setSqlcipherKey(key)
                    cipherKeyLoaded.value = true
                } else {
                    println("Incorrect key")
                }
            }

            retrievingKey.value = false
        }
    }
}