package com.fiiadoapp.fiado

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** "Hoy" y "este mes" se calculan en hora de Lima, aunque el teléfono tenga otra zona (ADR-0002). */
    @Provides
    fun reloj(): Clock = Clock.system(ZoneId.of("America/Lima"))
}
