package com.example.sensorviewer

import android.content.Context
import android.hardware.SensorManager
import com.example.sensorviewer.source.AndroidSensorSource
import com.example.sensorviewer.source.SensorDataSource
import com.example.sensorviewer.usecase.ObserveSensorsUseCase
import com.example.sensorviewer.usecase.ObserveSensorsUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSensorDataSource(impl: AndroidSensorSource): SensorDataSource

    @Binds
    @Singleton
    abstract fun bindObserveSensorsUseCase(impl: ObserveSensorsUseCaseImpl): ObserveSensorsUseCase

    companion object {
        @Provides
        @Singleton
        fun provideSensorManager(@ApplicationContext context: Context): SensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}
