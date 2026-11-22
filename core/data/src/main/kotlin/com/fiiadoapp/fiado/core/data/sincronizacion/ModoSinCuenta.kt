package com.fiiadoapp.fiado.core.data.sincronizacion

import javax.inject.Inject

/**
 * Sin cuenta (ADR-0010) nada se sube, así que la cola de pendientes solo crecería. Si el teléfono nunca
 * se vinculó, se vacía: al vincularse por primera vez la cola se arma de nuevo con toda la libreta.
 */
class ModoSinCuenta
    @Inject
    constructor(
        private val dao: RestauracionDao,
    ) {
        suspend fun vaciarColaSinVinculo() {
            if (dao.vinculo() == null) dao.vaciarCola()
        }
    }
