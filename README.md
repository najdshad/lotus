<div align="center">
  <img src="fastlane/metadata/android/en-US/images/icon-fit.png" width="200px" />

# Lotus

### Music player for Android
  
</div>

## Screenshots

<div align="center">
  <div>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width="24%" />
  </div>
</div>

## Features

- Lean fork to keep my personal use flow as smooth as possible.
- Enjoy your favorite music in a variety of formats, including MP3, FLAC, OGG, WAV, and more
- Easily browse tracks, albums, artists, and create custom playlists
- Designed with [Material You](https://m3.material.io/) and supports dynamic color palettes
- Supports AMOLED dark theme

## Build

```bash
./gradlew assembleDebug           # Build debug APK
./gradlew assembleRelease         # Build release APK
./gradlew clean                   # Clean build artifacts
adb install app/build/outputs/apk/debug/app-debug.apk    # Install debug APK
adb install app/build/outputs/apk/release/app-release.apk  # Install release APK
```

## Credits

Some UI elements are inspired by [Vanilla](https://github.com/vanilla-music/vanilla)

[Reorderable](https://github.com/Calvin-LL/Reorderable)

Most of this fork is built using AI assisted tools.

## License

Lotus is licensed under [GPLv3](LICENSE.md)
