package com.orangeelephant.sobriety.ui.screens.counterfullview

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.orangeelephant.sobriety.R
import com.orangeelephant.sobriety.storage.models.Counter
import com.orangeelephant.sobriety.storage.repositories.CounterRepository
import com.orangeelephant.sobriety.storage.repositories.DatabaseCounterRepository
import com.orangeelephant.sobriety.util.CounterViewUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sqlcipher.CursorIndexOutOfBoundsException
import java.util.Calendar

class CounterFullScreenViewModel(
    private val counterId: Int,
    private val counterRepository: CounterRepository = DatabaseCounterRepository()
): ViewModel() {
    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _counter.value = _counter.value?.let {
                    it.copy(
                        currentDurationString = CounterViewUtil.formatDurationAsString(
                            Calendar.getInstance().timeInMillis - it.startTimeMillis
                        )
                    )
                }
            }
        }
    }

    private val _counter = mutableStateOf(
        try {
            counterRepository.getCounter(counterId)
        } catch (e: CursorIndexOutOfBoundsException) {
            null
        }
    )
    val counter: MutableState<Counter?>
        get() = _counter


    fun onResetCounter() {
        val startTimeMillis = Calendar.getInstance().timeInMillis
        val newRecord = counterRepository.resetCounter(counterId, null)
        _counter.value = _counter.value?.copy(
            startTimeMillis = startTimeMillis,
            recordTimeSoberInMillis = newRecord,
            currentDurationString = CounterViewUtil.formatDurationAsString(startTimeMillis)
        )
    }

    fun onDeleteCounter(context: Context, navController: NavController) {
        counterRepository.deleteCounter(counterId)
        Toast.makeText(context, R.string.deleted_successfully, Toast.LENGTH_LONG).show()
        navController.popBackStack()
    }
}