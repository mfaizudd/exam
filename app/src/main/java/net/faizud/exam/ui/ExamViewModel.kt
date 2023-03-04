package net.faizud.exam.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.faizud.exam.network.SitumanApi
import net.faizud.exam.network.SitumanApiService
import net.faizud.exam.network.Verify

class ExamViewModel(private val sharedPreference: SharedPreferences) : ViewModel() {
    private val _locked = mutableStateOf(false)
    private val _examOn = mutableStateOf(false)
    private val _loading = mutableStateOf(true)
    private val _apiLoading = mutableStateOf(false)

    val locked: State<Boolean>
        get() = _locked

    val loading: State<Boolean>
        get() = _loading

    val apiLoading: State<Boolean>
        get() = _apiLoading
    
    fun loadState() {
        _locked.value = sharedPreference.getBoolean("locked", false)
        getStatus()
    }

    fun getStatus() {
        viewModelScope.launch {
            try {
                _apiLoading.value = true
                val result = SitumanApi.retrofitService.getStatus()
                Log.d("DEBUG", result.toString())
                _examOn.value = result.enable
                if (_examOn.value && _locked.value) {
                    lock()
                } else {
                    unlock()
                }
                _apiLoading.value = false
            } catch (ex: Exception) {
                ex.message?.let { Log.d("DEBUG", it) }
            }
        }
    }

    fun verify(passwd: String, onError: () -> Unit) {
        viewModelScope.launch {
            _apiLoading.value = true
            try {
                val verify = Verify(passwd)
                val response = SitumanApi.retrofitService.verify(verify)
                Log.d("DEBUG", response.toString())
                if (response.code() in 201..299) {
                    unlock()
                } else {
                    lock()
                    onError()
                }
            } catch (ex: Exception) {
                ex.message?.let { Log.d("DEBUG", it) }
                lock()
                onError()
            } finally {
                _apiLoading.value = false
            }
        }
    }

    fun lock() {
        _locked.value = true
        sharedPreference.edit().putBoolean("locked", _locked.value).apply()
    }

    fun unlock() {
        _locked.value = false
        sharedPreference.edit().putBoolean("locked", _locked.value).apply()
    }

    fun setLoading(value: Boolean) {
        _loading.value = value
    }
}