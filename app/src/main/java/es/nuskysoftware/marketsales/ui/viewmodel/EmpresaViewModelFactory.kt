package es.nuskysoftware.marketsales.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.nuskysoftware.marketsales.data.repository.EmpresaRepository

/** Factory para EmpresaViewModel que inyecta EmpresaRepository con Context */
class EmpresaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmpresaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmpresaViewModel(EmpresaRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}