package com.fiiadoapp.fiado.core.data.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** Preferencias que viven solo en este teléfono: no se respaldan en el servidor. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Preferencias

private val Context.preferenciasDataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

@Module
@InstallIn(SingletonComponent::class)
object PreferenciasModule {
    /** Una sola instancia por archivo: DataStore falla si dos escriben el mismo. */
    @Provides
    @Singleton
    @Preferencias
    fun preferencias(
        @ApplicationContext contexto: Context,
    ): DataStore<Preferences> = contexto.preferenciasDataStore
}
