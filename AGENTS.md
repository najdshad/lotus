# Environment

- Java: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`
- Android SDK: `/home/najdu/Work/lotus`
- Proxy required for all network access: `http://127.0.0.1:10808`

## Build & Test Commands

**Build:**

```bash
./gradlew assembleDebug           # Build debug APK
./gradlew assembleRelease         # Build release APK
./gradlew clean                   # Clean build artifacts
```

**Tests:**

```bash
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest     # Run instrumented tests on connected device
./gradlew test --tests "*ExampleUnitTest"            # Run specific test class
./gradlew test --tests "*ExampleUnitTest.addition_isCorrect"  # Run single test method
```

## Project Overview

Android music player (Lotus) built with Jetpack Compose, following clean architecture.

- **UI:** Jetpack Compose with Material3, Navigation, and dynamic theming
- **DI:** Koin for dependency injection
- **Media:** Media3 Exoplayer for audio playback
- **Data:** Room for local storage, Ktor for network, MediaStore for device scanning
- **Serialization:** kotlinx.serialization for JSON handling

## Architecture

**Layers:** `presentation/` → `domain/` → `data/` → `core/`

**Repository Pattern:** All data access goes through repository interfaces (TrackRepository, PlaylistRepository, LyricsRepository) implemented in data layer.

**Database:** Room database with entities for playlists and playlist tracks. DAO pattern for database operations with Flow-based reactive queries.

**Dependency Injection:** Use Koin modules in `di/` packages. Singletons for repositories, viewModels for ViewModels.

**State Management:** Use StateFlow with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialValue)`. MutableStateFlow as backing properties with underscore prefix.

**Result Pattern:** Wrap operation results in `Result.Success<D, E>` or `Result.Error<D, E>` from `domain/result/`. Use `DataError.Network` or `DataError.Local` enums.

**DI Modules:** Use `single { }` for singleton repositories and services. Use `viewModel { }` for ViewModels. Pass dependencies via `get()`.

## Code Style

**Formatting:** Kotlin official code style (no auto-formatters configured). No unnecessary comments.

**Imports:** Group by library (androidx, com.dn0ne, org.koin, kotlinx, io.ktor, etc.) with blank lines between groups. Wildcard imports allowed for Compose utilities.

**File Organization:** One public type per file. Private helper functions at bottom of file.

**File Structure:** Follow feature-based organization under app/core modules. Each feature has data/domain/presentation subpackages.

**Functions:** Prefer single-expression functions when concise. Extension functions in separate files or with clear grouping. Use extension functions for domain logic (e.g., `List<Track>.sortedBy()`).

**Extension Functions:** Group related extension functions by receiver type. Place in domain layer for business logic transformations.

**Performance:** Use Compose UI's fast operators for list operations: `fastFilter`, `fastMap`, `fastForEach`, `fastFirstOrNull` instead of standard Kotlin list functions.

**Coroutines:** Use `viewModelScope` for coroutines in ViewModels. Use `Dispatchers.IO` for disk/network operations. Launch coroutines with `viewModelScope.launch` for async operations.

## Naming Conventions

**Packages:** `com.dn0ne.player.[module].[layer]` (app/core) → `[feature]` (data/domain/presentation)
**Classes:** PascalCase (data classes, regular classes, interfaces)
**Functions:** camelCase, verb-noun for actions (`getTracks()`, `updateState()`)
**Variables:** camelCase, private state prefixed with underscore (`_trackList`)
**Constants:** UPPER_SNAKE_CASE
**Composables:** PascalCase, descriptive (`PlayerScreen`, `TrackList`, `MainPlayerScreen`)
**Events:** sealed interface with `OnXxx` pattern (`OnTrackClick`, `OnPlayClick`, `OnSettingsClick`)
**ViewModels:** `FeatureViewModel` (PlayerViewModel, SetupViewModel)
**State Classes:** `FeatureState` (PlaybackState, SettingsSheetState, TrackInfoSheetState)
**Enums:** PascalCase with descriptive values (SortOrder.Ascending, PlaybackMode.Repeat)

## Type System

**Nullability:** Explicit nullable types (`String?`) with null-safe calls (`?.`) and elvis operator (`?:`).
**Sealed Interfaces:** Use for type-safe hierarchies (PlayerScreenEvent, Error, Result).
**Data Classes:** For immutable data holders. Use `copy()` for updates. Use default values for optional fields.
**@Serializable:** Mark with kotlinx.serialization for network/storage (Track, Lyrics, Playlist). Use custom serializers when needed (TrackSerializer).
**ByteArray:** Prefer ByteArray over List<Byte> for binary data.

**Error Handling:**

- Never throw exceptions across layers - wrap in Result.Error
- Use SnackbarController to show user-facing error messages via string resources
- Log errors with Log.d() for debugging
- Check for specific error types: `when (result.error) { DataError.Network.BadRequest -> ... }`

**Logging:** Use Log.d() for debug information. Tag with appropriate class/package name for filtering.

**Resources:** Use R.string for all user-facing messages in SnackbarController and composables. Never hardcode strings.

## Testing

**Unit Tests:** Place in `app/src/test/`. Use JUnit 4 with `@Test` annotation. Test pure business logic, data transformations, repository methods.

**Instrumented Tests:** Place in `app/src/androidTest/`. Use AndroidX Test with `androidx.test.runner.AndroidJUnitRunner`. Test UI components, database operations, MediaStore queries.

**Running Single Test:**

```bash
# Unit test class
./gradlew test --tests "*PlayerViewModelTest"

# Specific test method
./gradlew test --tests "*PlayerViewModelTest.testOnPlayClick"

# Instrumented test on device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dn0ne.player.ExampleInstrumentedTest
```

## UI/Compose Guidelines

**Composables:** Prefer stateless composables with parameters. Hoist state to parent or ViewModel.
**Modifiers:** Apply modifiers last, after all other parameters. Use Modifier chaining.
**Preview:** Use `@Preview` annotation for simple composables.
**Navigation:** Type-safe navigation with @Serializable route objects in sealed interfaces.
**Theme:** Use Material3 components. Follow DynamicMaterialTheme for app-wide theming.
**Lists:** Use LazyColumn/LazyGrid with rememberLazyListState/rememberLazyGridState.
**State Classes:** Use data classes for UI state with default values (PlaybackState, SettingsSheetState, TrackInfoSheetState).

**Common Patterns:**

- Use `when` expressions for type-safe handling of sealed classes/interfaces (Events, Results, Enums)
- Use `copy()` on data classes to update immutable state
- Use Channel for one-shot events between ViewModels and UI
- Use `receiveAsFlow()` on Channels to collect as Flow in coroutines

## User notes

this app currently hasn't been released. there is no need to take migration from previous versions into consideration.
