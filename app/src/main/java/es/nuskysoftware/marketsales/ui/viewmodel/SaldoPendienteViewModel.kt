package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.nuskysoftware.marketsales.data.local.entity.SaldoGuardadoEntity
import es.nuskysoftware.marketsales.data.repository.SaldoGuardadoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SaldoPendienteViewModel(private val repository: SaldoGuardadoRepository) : ViewModel() {
    private val _saldoGuardado = MutableStateFlow<SaldoGuardadoEntity?>(null)
    val saldoGuardado: StateFlow<SaldoGuardadoEntity?> get() = _saldoGuardado

    init {
        viewModelScope.launch {
            _saldoGuardado.value = repository.getUltimoNoConsumido()
        }
    }
}

class SaldoPendienteViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SaldoGuardadoRepository(context)
        @Suppress("UNCHECKED_CAST")
        return SaldoPendienteViewModel(repo) as T
    }
}