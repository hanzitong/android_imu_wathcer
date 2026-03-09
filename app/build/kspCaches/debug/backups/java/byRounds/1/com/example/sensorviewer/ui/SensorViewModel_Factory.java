package com.example.sensorviewer.ui;

import com.example.sensorviewer.usecase.ObserveSensorsUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SensorViewModel_Factory implements Factory<SensorViewModel> {
  private final Provider<ObserveSensorsUseCase> observeSensorsProvider;

  public SensorViewModel_Factory(Provider<ObserveSensorsUseCase> observeSensorsProvider) {
    this.observeSensorsProvider = observeSensorsProvider;
  }

  @Override
  public SensorViewModel get() {
    return newInstance(observeSensorsProvider.get());
  }

  public static SensorViewModel_Factory create(
      javax.inject.Provider<ObserveSensorsUseCase> observeSensorsProvider) {
    return new SensorViewModel_Factory(Providers.asDaggerProvider(observeSensorsProvider));
  }

  public static SensorViewModel_Factory create(
      Provider<ObserveSensorsUseCase> observeSensorsProvider) {
    return new SensorViewModel_Factory(observeSensorsProvider);
  }

  public static SensorViewModel newInstance(ObserveSensorsUseCase observeSensors) {
    return new SensorViewModel(observeSensors);
  }
}
