package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.domain.GeneradorDeIds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** `Uuid.generateV7` sigue siendo experimental en Kotlin 2.4; el formato v7 es estándar (RFC 9562). */
class GeneradorUuidV7 : GeneradorDeIds {
    @OptIn(ExperimentalUuidApi::class)
    override fun nuevo(): Uuid = Uuid.generateV7()
}
