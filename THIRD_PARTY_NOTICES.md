# Third-party notices

## RootEncoder

Unictoos uses [RootEncoder](https://github.com/pedroSG94/RootEncoder) version 2.8.0 for the Android media and RTMP/RTMPS/SRT pipeline integration.

RootEncoder is distributed under the Apache License 2.0. Its source and license are available at the upstream repository. The dependency is included through JitPack using:

```kotlin
implementation("com.github.pedroSG94.RootEncoder:library:2.8.0")
```

Unictoos does not include platform stream keys, OAuth secrets, or private creator data in the dependency configuration.

## AndroidX and Kotlin/Compose

Unictoos uses AndroidX, Jetpack Compose, Kotlin, and related libraries. Their licenses and notices are provided by their respective artifacts and should be included in any redistributed binary according to their terms.

Unictoos uses Jetpack Media3 Transformer, Effect, and Common version 1.11.0 for local recording trim/export. Media3 is distributed under the Apache License 2.0; redistributed binaries must preserve the relevant copyright and license notices.

```kotlin
implementation("androidx.media3:media3-transformer:1.11.0")
implementation("androidx.media3:media3-effect:1.11.0")
implementation("androidx.media3:media3-common:1.11.0")
```

This file will be expanded before the first public release with a generated dependency report and the exact versions resolved by Gradle.
