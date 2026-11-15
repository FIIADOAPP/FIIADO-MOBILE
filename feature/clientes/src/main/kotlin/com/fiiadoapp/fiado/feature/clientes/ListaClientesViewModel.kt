package com.fiiadoapp.fiado.feature.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiiadoapp.fiado.domain.ClienteConSaldo
import com.fiiadoapp.fiado.domain.RepositorioClientes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListaClientesViewModel
    @Inject
    constructor(
        repositorio: RepositorioClientes,
    ) : ViewModel() {
        private val _busqueda = MutableStateFlow("")
        val busqueda: StateFlow<String> = _busqueda.asStateFlow()

        /** `null` mientras se carga la primera vez. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val clientes: StateFlow<List<ClienteConSaldo>?> =
            _busqueda
                .flatMapLatest { repositorio.observarActivos(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun buscar(texto: String) {
            _busqueda.value = texto
        }
    }
