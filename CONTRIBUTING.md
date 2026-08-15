# Contributing to Unictoos

Keep one-file-per-screen organization: place each top-level screen in its own file under `app/src/main/java/com/unictoai/unictoos/ui/screens/` and keep shared composables in the components package.

Unictoos is an Android live-streaming application. Keep changes focused, preserve secure credential storage, and run the relevant automated checks before opening a pull request.

## ViewModel constructor changes

When changing a `ViewModel` constructor, update every production construction call site and its explicit `ViewModelProvider.Factory` in the same change. Unit tests can instantiate injectable constructors successfully while Android's default factory still fails at runtime. Therefore, after any constructor signature change, launch the actual debug APK on a physical Android device and verify that the app reaches Home without a `Cannot create an instance of class` or `NoSuchMethodException` crash.

The minimum validation for an Android change is:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=1
python3 tools/feature_smoke_test.py
```

For capture or preview changes, also run the physical-device Practice-mode test described in the release notes and keep stream keys out of logs, bugreports, and screenshots.
