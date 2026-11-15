package com.fiiadoapp.fiado.domain

import kotlin.uuid.Uuid

/** Genera el identificador definitivo de un registro nuevo (UUID v7, según el ADR-0002). */
fun interface GeneradorDeIds {
    fun nuevo(): Uuid
}
