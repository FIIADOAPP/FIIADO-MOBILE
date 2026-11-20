package com.fiiadoapp.fiado.core.testing

import com.fiiadoapp.fiado.domain.PlantillaRecordatorio
import com.fiiadoapp.fiado.domain.RepositorioPlantillaRecordatorio
import kotlinx.coroutines.flow.MutableStateFlow

/** Plantilla del recordatorio en memoria. */
class PlantillaFalsa : RepositorioPlantillaRecordatorio {
    val plantilla = MutableStateFlow(PlantillaRecordatorio.POR_DEFECTO)

    override fun observar() = plantilla

    override suspend fun guardar(plantilla: PlantillaRecordatorio) {
        this.plantilla.value = plantilla
    }

    override suspend fun restablecer() {
        plantilla.value = PlantillaRecordatorio.POR_DEFECTO
    }
}
