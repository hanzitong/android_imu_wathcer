package com.example.sensorviewer.source;

import android.hardware.SensorManager;
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
public final class AndroidSensorSource_Factory implements Factory<AndroidSensorSource> {
  private final Provider<SensorManager> sensorManagerProvider;

  public AndroidSensorSource_Factory(Provider<SensorManager> sensorManagerProvider) {
    this.sensorManagerProvider = sensorManagerProvider;
  }

  @Override
  public AndroidSensorSource get() {
    return newInstance(sensorManagerProvider.get());
  }

  public static AndroidSensorSource_Factory create(
      javax.inject.Provider<SensorManager> sensorManagerProvider) {
    return new AndroidSensorSource_Factory(Providers.asDaggerProvider(sensorManagerProvider));
  }

  public static AndroidSensorSource_Factory create(Provider<SensorManager> sensorManagerProvider) {
    return new AndroidSensorSource_Factory(sensorManagerProvider);
  }

  public static AndroidSensorSource newInstance(SensorManager sensorManager) {
    return new AndroidSensorSource(sensorManager);
  }
}
