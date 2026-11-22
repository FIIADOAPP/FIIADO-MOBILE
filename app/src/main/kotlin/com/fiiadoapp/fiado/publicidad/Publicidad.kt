package com.fiiadoapp.fiado.publicidad

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.fiiadoapp.fiado.domain.CompraDePlay
import com.fiiadoapp.fiado.domain.EstadoQuitarAnuncios
import com.fiiadoapp.fiado.domain.PRODUCTO_QUITAR_ANUNCIOS
import com.fiiadoapp.fiado.domain.ReglasDeCompra
import com.fiiadoapp.fiado.domain.RepositorioCompra
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anuncios y compra «Quitar anuncios» (ADR-0011): consulta a Google Play, recuerda el último estado
 * para cuando no hay internet y pide el consentimiento antes de cargar anuncios.
 */
@Singleton
class Publicidad
    @Inject
    constructor(
        @param:ApplicationContext private val contexto: Context,
        private val repositorio: RepositorioCompra,
    ) : PurchasesUpdatedListener {
        private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val _estado = MutableStateFlow<EstadoQuitarAnuncios>(EstadoQuitarAnuncios.Consultando)
        val estado: StateFlow<EstadoQuitarAnuncios> = _estado.asStateFlow()

        private val puedePedirAnuncios = MutableStateFlow(false)
        private val _opcionesDePrivacidadRequeridas = MutableStateFlow(false)
        val opcionesDePrivacidadRequeridas: StateFlow<Boolean> = _opcionesDePrivacidadRequeridas.asStateFlow()

        private val anunciosIniciados = AtomicBoolean(false)
        private var producto: ProductDetails? = null
        private val consentimiento: ConsentInformation = UserMessagingPlatform.getConsentInformation(contexto)

        /** Se muestra el banner solo con consentimiento y si no consta que se pagó; sin saberlo aún, no. */
        val mostrarAnuncios: StateFlow<Boolean> =
            combine(puedePedirAnuncios, repositorio.observarAnunciosQuitados(), _estado) { puede, quitados, estado ->
                puede && !quitados && estado != EstadoQuitarAnuncios.Comprado
            }.stateIn(alcance, SharingStarted.Eagerly, false)

        private val cliente: BillingClient =
            BillingClient
                .newBuilder(contexto)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()

        init {
            alcance.launch {
                // Sin respuesta de Play, vale lo último que se supo.
                if (repositorio.observarAnunciosQuitados().first()) _estado.value = EstadoQuitarAnuncios.Comprado
            }
            conectar()
        }

        private fun conectar() {
            cliente.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(resultado: BillingResult) {
                        if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
                            restaurar()
                        } else if (_estado.value == EstadoQuitarAnuncios.Consultando) {
                            _estado.value = EstadoQuitarAnuncios.NoDisponible
                        }
                    }

                    override fun onBillingServiceDisconnected() = Unit
                },
            )
        }

        /** Consulta el producto y las compras; Google Play manda sobre lo guardado. */
        fun restaurar() {
            if (!cliente.isReady) {
                conectar()
                return
            }
            alcance.launch {
                val detalles =
                    cliente.queryProductDetails(
                        QueryProductDetailsParams
                            .newBuilder()
                            .setProductList(
                                listOf(
                                    QueryProductDetailsParams.Product
                                        .newBuilder()
                                        .setProductId(PRODUCTO_QUITAR_ANUNCIOS)
                                        .setProductType(BillingClient.ProductType.INAPP)
                                        .build(),
                                ),
                            ).build(),
                    )
                producto = detalles.productDetailsList?.firstOrNull()
                val compras =
                    cliente.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    )
                if (compras.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    procesar(compras.purchasesList)
                } else if (_estado.value == EstadoQuitarAnuncios.Consultando) {
                    _estado.value = EstadoQuitarAnuncios.NoDisponible
                }
            }
        }

        fun comprar(actividad: Activity) {
            val detalles = producto ?: return restaurar()
            val params =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams
                                .newBuilder()
                                .setProductDetails(detalles)
                                .build(),
                        ),
                    ).build()
            cliente.launchBillingFlow(actividad, params)
        }

        override fun onPurchasesUpdated(
            resultado: BillingResult,
            compras: MutableList<Purchase>?,
        ) {
            when (resultado.responseCode) {
                BillingClient.BillingResponseCode.OK -> alcance.launch { procesar(compras.orEmpty()) }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restaurar()
                else -> Unit
            }
        }

        private suspend fun procesar(compras: List<Purchase>) {
            val deplay = compras.map { it.aCompraDePlay() }
            ReglasDeCompra.porReconocer(deplay).forEach { compra ->
                cliente.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.token).build())
            }
            val quitados = ReglasDeCompra.quitaAnuncios(deplay)
            repositorio.guardarAnunciosQuitados(quitados)
            _estado.value =
                when {
                    quitados -> EstadoQuitarAnuncios.Comprado
                    ReglasDeCompra.hayPendiente(deplay) -> EstadoQuitarAnuncios.Pendiente
                    else -> producto?.precio()?.let(EstadoQuitarAnuncios::Disponible) ?: EstadoQuitarAnuncios.NoDisponible
                }
        }

        /** Pide el consentimiento donde la ley lo exige y, si se puede, inicia los anuncios. */
        fun pedirConsentimiento(actividad: Activity) {
            consentimiento.requestConsentInfoUpdate(
                actividad,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(actividad) { alTerminarConsentimiento() }
                },
                { alTerminarConsentimiento() },
            )
            // Lo decidido en una sesión anterior permite pedir anuncios sin esperar.
            if (consentimiento.canRequestAds()) iniciarAnuncios()
        }

        fun abrirOpcionesDePrivacidad(actividad: Activity) {
            UserMessagingPlatform.showPrivacyOptionsForm(actividad) { alTerminarConsentimiento() }
        }

        private fun alTerminarConsentimiento() {
            _opcionesDePrivacidadRequeridas.value =
                consentimiento.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            if (consentimiento.canRequestAds()) iniciarAnuncios() else puedePedirAnuncios.value = false
        }

        private fun iniciarAnuncios() {
            if (anunciosIniciados.getAndSet(true)) {
                puedePedirAnuncios.value = true
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(contexto) {}
                puedePedirAnuncios.value = true
            }
        }
    }

private fun Purchase.aCompraDePlay() =
    CompraDePlay(
        productos = products,
        pagada = purchaseState == Purchase.PurchaseState.PURCHASED,
        pendiente = purchaseState == Purchase.PurchaseState.PENDING,
        reconocida = isAcknowledged,
        token = purchaseToken,
    )

private fun ProductDetails.precio(): String? = oneTimePurchaseOfferDetails?.formattedPrice
