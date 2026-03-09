package com.example.sensorviewer.domain.usecase;

import com.example.sensorviewer.data.source.SensorDataSource;
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
public final class ObserveSensorsUseCaseImpl_Factory implements Factory<ObserveSensorsUseCaseImpl> {
  private final Provider<SensorDataSource> dataSourceProvider;

  public ObserveSensorsUseCaseImpl_Factory(Provider<SensorDataSource> dataSourceProvider) {
    this.dataSourceProvider = dataSourceProvider;
  }

  @Override
  public ObserveSensorsUseCaseImpl get() {
    return newInstance(dataSourceProvider.get());
  }

  public static ObserveSensorsUseCaseImpl_Factory create(
      javax.inject.Provider<SensorDataSource> dataSourceProvider) {
    return new ObserveSensorsUseCaseImpl_Factory(Providers.asDaggerProvider(dataSourceProvider));
  }

  public static ObserveSensorsUseCaseImpl_Factory create(
      Provider<SensorDataSource> dataSourceProvider) {
    return new ObserveSensorsUseCaseImpl_Factory(dataSourceProvider);
  }

  public static ObserveSensorsUseCaseImpl newInstance(SensorDataSource dataSource) {
    return new ObserveSensorsUseCaseImpl(dataSource);
  }
}
