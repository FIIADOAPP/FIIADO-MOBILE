package com.fiiadoapp.fiado.core.data

import com.fiiadoapp.fiado.core.data.sincronizacion.ModoSinCuenta
import com.fiiadoapp.fiado.core.data.sincronizacion.VinculoEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class ModoSinCuentaTest : BaseDeDatosDePrueba() {
    private val modo = ModoSinCuenta(db.restauracionDao())

    @Test
    fun `sin vinculo vacia la cola sin tocar la libreta`() =
        runTest {
            val rosa = cliente("Doña Rosa")
            clientes.guardar(rosa)

            modo.vaciarColaSinVinculo()

            assertEquals(0, db.colaDao().todas().size)
            assertNotNull(db.clienteDao().obtener(rosa.id.toString()))
        }

    @Test
    fun `un telefono que ya se vinculo conserva su cola`() =
        runTest {
            db.restauracionDao().guardarVinculo(VinculoEntity(correo = "rosa@bodega.pe", cursor = null, restaurada = true, primera = false))
            clientes.guardar(cliente("Doña Rosa"))

            modo.vaciarColaSinVinculo()

            assertEquals(1, db.colaDao().todas().size)
        }
}
