
# Android Sensor Viewer App - Technical Stack

## Overview
Create an Android application that displays smartphone sensor data in real time.

## Technology Stack

### Programming Language
- Kotlin

### UI Framework
- Jetpack Compose

### Design System
- Material 3 (Compose Material3)

### Sensor Access
- SensorManager (Android SDK)

## Architecture

The application should follow this structure:

```
Android App
│
├─ UI Layer
│   └─ Jetpack Compose
│
├─ Design System
│   └─ Material 3
│
├─ Application Logic
│   └─ Kotlin
│
└─ Hardware Access
    └─ Android SDK
         └─ SensorManager
```

## Functional Requirements

1. Access smartphone sensors via `SensorManager`.
2. Retrieve sensor values in real time.
3. Display sensor data in the UI using Jetpack Compose.
4. Use Material 3 components for the UI design.

## Target Sensors (initial scope)

- Accelerometer
- Gyroscope
- Magnetometer
- Barometer
- Ambient light sensor
- Proximity sensor

## Development Constraints

- Use Kotlin as the only programming language.
- Use Jetpack Compose for all UI elements (do not use XML layouts).
- Use Material 3 Compose components for UI styling.
- Sensor data should update continuously and be reflected in the UI in real time.


## Test
1, logic unit test
2, compose UI test
3, real hardware test (will not implement here)

## Architecture separation
- Sensor layer
- State/Logic layer
- UI layer

