# Unictoos v0.1.0-alpha16

## Confirmed Android 15 startup-crash fix

The supplied Infinix X6853 bugreport identified the exact failure:

> `java.lang.RuntimeException: Cannot create an instance of class com.unictoai.unictoos.StudioViewModel`
>
> `Caused by: java.lang.NoSuchMethodException: com.unictoai.unictoos.StudioViewModel.<init> [class android.app.Application]`

Android’s `AndroidViewModelFactory` was looking for the required public one-argument `Application` constructor. `StudioViewModel` had additional constructor parameters with defaults, but Kotlin did not expose the exact Java constructor signature required by Android’s reflective factory. The app therefore crashed during the first Compose composition, before any screen could appear.

Alpha16 adds `@JvmOverloads` to `StudioViewModel`, generating the public `StudioViewModel(Application)` constructor required by AndroidViewModelFactory. A regression test now verifies that constructor through Java reflection.

The earlier startup persistence hardening remains included. CredentialStore encryption, credential semantics, stream reconnect behavior, and authentication-error handling are unchanged.

## Validation

Alpha16 passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, constructor reflection verification, and the repository feature smoke suite with **47/47 checks passed**.

Install alpha16 after uninstalling alpha15 if Android reports an update conflict.
