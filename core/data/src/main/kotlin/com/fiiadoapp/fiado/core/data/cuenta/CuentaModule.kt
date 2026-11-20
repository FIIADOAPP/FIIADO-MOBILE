package com.fiiadoapp.fiado.core.data.cuenta

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.fiiadoapp.fiado.core.data.BuildConfig
import com.fiiadoapp.fiado.domain.RepositorioCuenta
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.sesionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sesion")

@Module
@InstallIn(SingletonComponent::class)
object CuentaModule {
    @Provides
    @Singleton
    fun almacenSesion(
        @ApplicationContext contexto: Context,
    ): AlmacenSesion = AlmacenSesion(contexto.sesionDataStore, CifradorKeystore())

    /** Uno solo para toda la app: dos clientes podrían renovar a la vez con el mismo token y cerrar la sesión. */
    @Provides
    @Singleton
    fun retrofitConSesion(almacen: AlmacenSesion): Retrofit = crearRetrofitConSesion(BuildConfig.API_URL, almacen)

    @Provides
    @Singleton
    fun repositorioCuenta(
        retrofit: Retrofit,
        almacen: AlmacenSesion,
    ): RepositorioCuenta = RepositorioCuentaRemoto(retrofit.create(ApiCuentas::class.java), almacen)
}

/**
 * Cliente HTTP de la API: agrega el token a las llamadas marcadas y renueva la sesión.
 * Las pruebas usan esta misma función contra MockWebServer.
 */
fun crearRetrofitConSesion(
    urlBase: String,
    almacen: AlmacenSesion,
): Retrofit {
    val base =
        OkHttpClient
            .Builder()
            .connectTimeout(TIEMPO_ESPERA_S, TimeUnit.SECONDS)
            .readTimeout(TIEMPO_ESPERA_S, TimeUnit.SECONDS)
            .build()
    val refresco = retrofit(urlBase, base).create(ApiRefresco::class.java)
    val conSesion =
        base
            .newBuilder()
            .addInterceptor(TokenDeAcceso(almacen))
            .authenticator(RenovacionDeSesion(almacen, refresco))
            .build()
    return retrofit(urlBase, conSesion)
}

private fun retrofit(
    urlBase: String,
    cliente: OkHttpClient,
): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()

private const val TIEMPO_ESPERA_S = 15L
