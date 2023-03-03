package net.faizud.exam.ui

import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExamViewModel(private val sharedPreference: SharedPreferences) : ViewModel() {
    private val _locked = mutableStateOf(false)
    private val _examOn = MutableStateFlow(false)

    val locked: State<Boolean>
        get() = _locked

    val examOn: StateFlow<Boolean>
        get() = _examOn.asStateFlow()

    fun loadState() {
        _locked.value = sharedPreference.getBoolean("locked", false)
    }

    fun lock() {
        _locked.value = true
        sharedPreference.edit().putBoolean("locked", _locked.value).apply()
    }

    fun unlock() {
        _locked.value = false
        sharedPreference.edit().putBoolean("locked", _locked.value).apply()
    }
}