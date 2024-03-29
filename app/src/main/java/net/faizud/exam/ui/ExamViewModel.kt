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
    private val _apiFailed = mutableStateOf(false)
    private val _webViewFailed = mutableStateOf(false)
    private val _url = mutableStateOf<String?>(null)

    val locked: State<Boolean>
        get() = _locked

    val loading: State<Boolean>
        get() = _loading

    val apiLoading: State<Boolean>
        get() = _apiLoading

    val apiFailed: State<Boolean>
        get() = _apiFailed

    val webViewFailed: State<Boolean>
        get() = _webViewFailed

    val url: State<String?>
        get() = _url

    fun getStatus() {
        viewModelScope.launch {
            _apiFailed.value = false
            _apiLoading.value = true
            try {
                val result = SitumanApi.retrofitService.getStatus()
                Log.d("DEBUG", result.toString())
                _examOn.value = result.enable
                _url.value = result.ip
                if (_examOn.value) {
                    lock()
                } else {
                    unlock()
                }
            } catch (ex: Exception) {
                ex.message?.let { Log.d("DEBUG", it) }
                _apiFailed.value = true
            } finally {
                _apiLoading.value = false
            }
        }
    }

    fun verify(passwd: String, onError: () -> Unit) {
        viewModelScope.launch {
            _apiFailed.value = false
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
                _apiFailed.value = true
            } finally {
                _apiLoading.value = false
            }
        }
    }

    fun lock() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun setLoading(value: Boolean) {
        _loading.value = value
        if (value) {
            _webViewFailed.value = false
        }
    }

    fun setFail(value: Boolean) {
        _webViewFailed.value = value
    }
}