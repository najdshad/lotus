# Feature Removal Plan for Lotus Music Player

## PROGRESS TRACKING

**Overall Completion:** 2/12 sections completed (~17%)

### Completed Sections:
- ✅ **Section 1: Lyrics Feature** (100%)
- ✅ **Section 2: Metadata Editing Feature** (100%)

### Remaining Sections:
- ⏳ Section 3: Remove Genres and Folders tabs
- ⏳ Section 4: Remove shuffle playback mode
- ⏳ Section 5: Remove equalizer feature
- ⏳ Section 6: Remove sleep timer feature
- ⏳ Section 7: Remove tab reordering & default tab setting
- ⏳ Section 8: Remove grid/list view toggle
- ⏳ Section 9: Remove ignore short tracks setting
- ⏳ Section 10: Remove multi-language support
- ⏳ Section 11: Remove album art color theming
- ⏳ Section 12: Clean up build dependencies

### Summary of Completed Work:
- **Files Deleted:** 24 files (lyrics + metadata)
- **Files Modified:** 17 files
- **Lines Removed:** ~1,200+ lines
- **Code Cleanup:** Removed 2 complete feature subsystems from domain, data, and presentation layers

---

## SUMMARY OF CHANGES

### To Remove:
1. ✅ Lyrics feature (entirely) - COMPLETED
2. ✅ Metadata editing feature (entirely) - COMPLETED
3. ✗ Genres and Folders tabs
4. ✗ Shuffle playback mode
5. ✗ Equalizer feature
6. ✗ Sleep timer feature
7. ✗ Tab reordering & default tab setting
8. ✗ Grid/List view toggle
9. ✗ Ignore short tracks setting
10. ✗ Multi-language support (RU, UK)
11. ✗ Album art color theming

### To Keep:
- Basic playback (play/pause/seek/next/prev)
- Queue management (play next, add to queue, reorder queue)
- Background playback
- Playback modes: Repeat, RepeatOne (NO shuffle)
- Scroll to top button
- Locate current track button
- Refresh on app launch
- Filter vs search mode toggle
- Custom playlists (create/rename/delete)
- Playlist import (M3U)
- Fixed tab order: [Album, Artist, Track, Playlists]
- Layouts: Grid (Albums/Artists), List (Playlists/Tracks)
- Audio focus handling
- Scan folder configuration
- Dynamic colors (Material You)
- AMOLED dark theme
- System/Light/Dark theme options
- English language only

---

## SECTION 1: REMOVE ENTIRE LYRICS FEATURE

### Files to DELETE (13 files):
```
Domain Layer:
- app/domain/lyrics/Lyrics.kt

Data Layer:
- app/data/LyricsReader.kt
- app/data/LyricsReaderImpl.kt
- app/data/repository/LyricsRepository.kt
- app/data/repository/RealmLyricsRepository.kt
- app/data/remote/lyrics/LyricsProvider.kt
- app/data/remote/lyrics/LrclibLyricsProvider.kt

Presentation Layer:
- app/presentation/components/playback/LyricsSheet.kt
- app/presentation/components/trackinfo/LyricsControlSheet.kt
- app/presentation/components/trackinfo/LyricsControlSheetState.kt
- app/presentation/components/settings/LyricsSettings.kt
- app/presentation/components/trackinfo/TrackInfoRoutes.kt (remove LyricsControl route)
```

### Files to MODIFY (8 files):

1. **PlayerViewModel.kt** (~50 lines to remove)
   - Remove imports: LyricsRepository, LyricsProvider, LyricsReader, Lyrics
   - Remove constructor parameters: lyricsProvider, lyricsReader, lyricsRepository
   - Remove state: `_lyrics`, `_isLoadingLyrics`, `_isLyricsSheetExpanded`
   - Remove methods: `fetchLyrics()`, `readLyricsFromTag()`, `loadLyrics()`
   - Remove event handlers: 8 lyrics-related events

2. **PlayerScreenEvent.kt**
   - Remove events: OnLyricsClick, OnLyricsSheetExpandedChange, OnLyricsControlClick, OnDeleteLyricsClick, OnCopyLyricsFromTagClick, OnWriteLyricsToTagClick, OnFetchLyricsFromRemoteClick, OnPublishLyricsOnRemoteClick

3. **PlaybackState.kt**
   - Remove fields: `lyrics: Lyrics?`, `isLoadingLyrics: Boolean`, `isLyricsSheetExpanded: Boolean`

4. **TrackInfoSheetState.kt**
   - Remove: `lyricsControlSheetState: LyricsControlSheetState`

5. **PlayerScreen.kt**
   - Remove lyrics button from PlayerSheet (2 locations)
   - Remove lyrics sheet rendering

6. **TrackInfoSheet.kt**
   - Remove LyricsControl navigation route

7. **PlayerModule.kt**
   - Remove providers: LyricsRepository, LyricsProvider, LyricsReader
   - Remove LyricsJson from Realm schema

8. **Settings.kt**
   - Remove settings keys: `lyricsFontSizeKey`, `lyricsFontWeightKey`, `lyricsLineHeightKey`, `lyricsLetterSpacingKey`, `lyricsAlignmentKey`, `useDarkPaletteOnLyricsSheetKey`
   - Remove StateFlow and update functions for these settings

---

## SECTION 2: REMOVE ENTIRE METADATA EDITING FEATURE

### Files to DELETE (11 files):
```
Domain Layer:
- app/domain/metadata/Metadata.kt
- app/domain/metadata/MetadataSearchResult.kt

Data Layer:
- app/data/MetadataWriter.kt
- app/data/MetadataWriterImpl.kt
- app/data/remote/metadata/MetadataProvider.kt
- app/data/remote/metadata/MusicBrainzMetadataProvider.kt

Presentation Layer:
- app/presentation/components/trackinfo/RisksOfMetadataEditingDialog.kt
- app/presentation/components/trackinfo/EditDropdownMenu.kt
- app/presentation/components/trackinfo/InfoSearchSheet.kt
- app/presentation/components/trackinfo/InfoSearchSheetState.kt
- app/presentation/components/trackinfo/ManualInfoEditSheet.kt
- app/presentation/components/trackinfo/ManualInfoEditSheetState.kt
- app/presentation/components/trackinfo/ChangesSheet.kt
- app/presentation/components/trackinfo/ChangesSheetState.kt
```

### Files to MODIFY (6 files):

1. **PlayerViewModel.kt** (~80 lines to remove)
   - Remove imports: MetadataProvider, MetadataWriter, Metadata, MetadataSearchResult
   - Remove constructor parameters: metadataProvider, metadataWriter
   - Remove state: `pendingMetadata` Channel
   - Remove event handlers: 5 metadata events
   - Remove all metadata-related logic

2. **PlayerScreenEvent.kt**
   - Remove events: OnAcceptingRisksOfMetadataEditing, OnMatchDurationWhenSearchMetadataClick, OnMetadataSearchResultPick, OnOverwriteMetadataClick, OnConfirmMetadataEditClick

3. **TrackInfoSheet.kt**
   - Remove EditDropdownMenu
   - Remove navigation routes: InfoSearch, ManualEditing
   - Keep only TrackInfo display (read-only)

4. **TrackInfoSheetState.kt**
   - Remove: `showRisksOfMetadataEditingDialog`, `infoSearchSheetState`, `manualInfoEditSheetState`, `changesSheetState`

5. **PlayerModule.kt**
   - Remove providers: MetadataProvider, MetadataWriter

6. **MainActivity.kt**
   - Remove: `checkMetadataWriteResult()` function
   - Remove: pendingMetadata Channel observer

7. **Settings.kt**
   - Remove settings keys: `areRisksOfMetadataEditingAcceptedKey`, `matchDurationWhenSearchMetadataKey`
   - Remove StateFlow and update functions

### Files to MODIFY - TrackInfoRoutes.kt:
- Remove InfoSearch and ManualEditing routes
- Keep only TrackInfo route

---

## SECTION 3: REMOVE GENRES AND FOLDERS TABS

### Files to MODIFY (4 files):

1. **Tab.kt**
   - Remove: `Genres(R.string.genres)`, `Folders(R.string.folders)` from enum

2. **PlayerViewModel.kt** (~30 lines to remove)
   - Remove: `genrePlaylists` StateFlow
   - Remove: `folderPlaylists` StateFlow

3. **PlayerScreen.kt** (~80 lines to remove)
   - Remove: Genres tab content
   - Remove: Folders tab content
   - Remove: Genre and Folder selection callbacks

4. **Settings.kt** (~20 lines to remove)
   - Modify `_tabs` initialization to filter out Genres and Folders:
   ```kotlin
   private val _tabs = MutableStateFlow(
       sharedPreferences.getString(tabOrderKey, null)?.let {
           it.split(";").map { Tab.valueOf(it) }
       } ?: listOf(Tab.Albums, Tab.Artists, Tab.Tracks, Tab.Playlists)
   )
   ```

### Files to MODIFY - TrackRepositoryImpl.kt:
- Keep `getGenres()` and `getFoldersWithAudio()` for potential future use, but they won't be used in UI

### Keep Track.kt:
- Keep `genre: String?` field (still used for track info display)

---

## SECTION 4: REMOVE SHUFFLE PLAYBACK MODE

### Files to MODIFY (3 files):

1. **PlaybackMode.kt**
   - Remove: `Shuffle` from enum
   - Result: enum class PlaybackMode { Repeat, RepeatOne }

2. **PlayerViewModel.kt** (~15 lines to remove)
   - Remove: `player?.shuffleModeEnabled = true/false` logic
   - Update playback mode cycling to only cycle between Repeat and RepeatOne

3. **PlayerSheet.kt** (~40 lines to remove)
   - Remove: shuffle button from UI (2 locations: portrait and landscape)
   - Remove: shuffle icon import

### String resources to remove (from all 3 language files):
- `shuffle_mode` (if exists)

---

## SECTION 5: REMOVE EQUALIZER FEATURE

### Files to DELETE (0 files - all in existing files)

### Files to MODIFY (4 files):

1. **PlaybackService.kt** (~180 lines to remove)
   - Remove: `EqualizerSettings` class
   - Remove: `EqualizerController` class
   - Remove: EqualizerController initialization and usage

2. **PlayerModule.kt** (~5 lines to remove)
   - Remove: EqualizerController provider

3. **PlayerViewModel.kt** (~3 lines to remove)
   - Remove: `equalizerController` constructor parameter
   - Remove from settingsSheetState

4. **SettingsSheetState.kt** (~2 lines to remove)
   - Remove: `equalizerController: EqualizerController` from data class

5. **PlaybackSettings.kt** (~60 lines to remove)
   - Remove: `EqualizerSettings` composable
   - Remove: Equalizer toggle from PlaybackSettings

6. **SettingsSheet.kt** (~2 lines to remove)
   - Remove: equalizerController parameter to PlaybackSettings

### String resources to remove (from all 3 language files):
- `equalizer`
- `equalizer_explain`
- Update: `playback_supporting_text` to remove "equalizer" mention

---

## SECTION 6: REMOVE SLEEP TIMER FEATURE

### Files to DELETE (1 file):
```
- app/presentation/components/playback/SleepTimerBottomSheet.kt
```

### Files to MODIFY (2 files):

1. **PlaybackService.kt** (~40 lines to remove)
   - Remove: `SleepTimer` object
   - Remove: SleepTimer initialization and callback registration

2. **PlayerSheet.kt** (~30 lines to remove)
   - Remove: sleep timer button from UI (2 locations: portrait and landscape)
   - Remove: sleep timer sheet state and display logic

### String resources to remove (from all 3 language files):
- `set_sleep_timer`
- `sleep_timer`
- `start_timer`
- `stop_timer`

---

## SECTION 7: REMOVE TAB REORDERING & DEFAULT TAB SETTING

### Files to MODIFY (3 files):

1. **Settings.kt** (~30 lines to remove, ~10 lines to add)
   - Remove: `defaultTabKey`
   - Remove: `_defaultTab` StateFlow and update function
   - Modify `_tabs` initialization to hardcode fixed order

2. **TabsSettings.kt** (~80 lines to remove)
   - Remove: draggable tab list UI
   - Remove: default tab selection UI
   - Simplify to show only tab order info (read-only)

### String resources to remove (from all 3 language files):
- `default_tab_and_order`
- `default_tab`
- Any tab reordering strings

---

## SECTION 8: REMOVE GRID/LIST VIEW TOGGLE

### Files to MODIFY (6 files):

1. **Settings.kt** (~30 lines to remove)
   - Remove: playlistUseListViewKey, tracksUseListViewKey
   - Remove: StateFlow and update functions for view modes

2. **PlayerScreen.kt** (~40 lines to remove)
   - Remove: grid/list toggle button from playlists UI
   - Remove: grid/list toggle button from tracks UI
   - Hardcode layouts:
     - Custom playlists → always use PlaylistRows (list view)
     - Tracks → always use TrackRows (list view)
     - Albums → always use PlaylistCards (grid view)
     - Artists → always use PlaylistCards (grid view)

3. **PlaylistRows.kt** (~20 lines to remove)
   - Remove: grid view conditionals
   - Simplify to always render list view

4. **PlaylistCards.kt** (~20 lines to remove)
   - Remove: list view conditionals
   - Simplify to always render grid view

5. **TrackRows.kt** (~20 lines to remove)
   - Remove: grid view conditionals
   - Simplify to always render list view

### String resources to remove (from all 3 language files):
- `enable_list_view`
- `enable_grid_view`

---

## SECTION 9: REMOVE IGNORE SHORT TRACKS SETTING

### Files to MODIFY (2 files):

1. **Settings.kt** (~10 lines to remove)
   - Remove: `ignoreShortTracksKey`
   - Remove: `_ignoreShortTracks` StateFlow and update function

2. **MusicScanSettings.kt** (~10 lines to remove)
   - Remove: ignore short tracks toggle UI

3. **TrackRepositoryImpl.kt** (~10 lines to remove)
   - Remove: ignore short tracks filtering logic

### String resources to remove (from all 3 language files):
- `ignore_short_tracks`
- `ignore_short_tracks_explain` (if exists)

---

## SECTION 10: REMOVE MULTI-LANGUAGE SUPPORT (KEEP ENGLISH ONLY)

### Files to DELETE (2 directories):
```
- app/src/main/res/values-ru/
- app/src/main/res/values-uk/
```

### No code changes needed - app will fallback to English (values/strings.xml)

---

## SECTION 11: REMOVE ALBUM ART COLOR THEMING

### Files to DELETE (0 files - all in existing files)

### Files to MODIFY (6 files):

1. **Settings.kt** (~30 lines to remove)
   - Remove: `useAlbumArtColorKey`, `paletteStyleKey`
   - Remove: `_useAlbumArtColor`, `_paletteStyle` StateFlow
   - Remove: update functions for these settings
   - Keep: `useDynamicColor` and `amoledDarkTheme` (Material You and AMOLED)

2. **PlayerScreen.kt** (~50 lines to remove)
   - Remove: imports for `kmpalette` and `materialkolor`
   - Remove: `dominantColorState` extraction logic
   - Remove: `colorToApply` logic
   - Remove: `DynamicMaterialTheme` wrapping with album art color
   - Replace with: `DynamicMaterialTheme(seedColor = defaultSeedColor, ...)`

3. **ThemeSettings.kt** (~150 lines to remove)
   - Remove: palette style options UI
   - Remove: palette style selection logic
   - Remove: "Use album art color" toggle
   - Keep: Appearance options, AMOLED toggle, Dynamic colors toggle

4. **PlaylistRows.kt** (~30 lines to remove)
   - Remove: `DominantColorState` usage
   - Remove: dominant color application to TrackCountBubble

5. **PlaylistCards.kt** (~30 lines to remove)
   - Remove: `DominantColorState` usage
   - Remove: dominant color application to card containers

6. **SelectionRows.kt** (~30 lines to remove)
   - Remove: `DominantColorState` usage
   - Remove: dominant color application

7. **SelectionCards.kt** (~30 lines to remove)
   - Remove: `DominantColorState` usage
   - Remove: dominant color application

### String resources to remove (from all 3 language files):
- `palette_style`
- `palette_tonal_spot`, `palette_neutral`, `palette_vibrant`, `palette_expressive`
- `palette_rainbow`, `palette_fruit_salad`, `palette_monochrome`
- `palette_fidelity`, `palette_content`
- `use_album_art_color`
- `use_album_art_color_explain`

### Keep:
- `black_theme`, `black_theme_explain` (AMOLED)
- `use_system_key_colors`, `use_system_key_colors_explain` (Dynamic colors)

---

## SECTION 12: CLEAN UP BUILD DEPENDENCIES

### File to MODIFY:
- **gradle/libs.versions.toml**
  - Remove: `kmpalette` and `materialkolor` entries
  - Remove: `jaudiotagger` entry
  - Remove: `ktor-core` and `ktor-client-okhttp` entries

- **app/build.gradle.kts**
  - Remove: `implementation(libs.kmpalette)`
  - Remove: `implementation(libs.materialkolor)`
  - Remove: `implementation(libs.jaudiotagger)`
  - Remove: ktor HTTP client libraries

### File to MODIFY:
- **app/proguard-rules.pro**
  - Remove: jaudiotagger ProGuard rules

---

## VERIFICATION & TESTING PLAN

After implementing all changes:

1. **Build verification:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **Functional testing:**
   - Test basic playback (play/pause/seek/next/prev)
   - Test queue management (play next, add to queue, reorder)
   - Test repeat and repeat-one modes (NO shuffle)
   - Test background playback
   - Test custom playlists (create/rename/delete)
   - Test playlist import (M3U)
   - Test all tabs: Albums, Artists, Tracks, Playlists (fixed order)
   - Verify layouts: Grid for Albums/Artists, List for Tracks/Playlists
   - Test scroll to top button
   - Test locate current track button
   - Test filter vs search mode toggle
   - Test refresh on app launch
   - Test scan folder configuration
   - Test audio focus handling
   - Test dynamic colors (Material You)
   - Test AMOLED dark theme
   - Test System/Light/Dark theme options

3. **Verify removed features are NOT present:**
   - No lyrics button or sheet
   - No metadata editing options
   - No Genres or Folders tabs
   - No shuffle button or mode
   - No equalizer in settings
   - No sleep timer
   - No tab reordering
   - No default tab setting
   - No grid/list toggle for playlists/tracks
   - No ignore short tracks setting
   - No language options (English only)
   - No album art color theming options

4. **Check for compilation errors:**
   - Fix any remaining imports
   - Fix any remaining references to removed classes
   - Run linting: `./gradlew lint`

---

## ESTIMATED IMPACT (UPDATED)

### Completed:
**Files Deleted:** 24 files
**Files Modified:** 17 files
**Lines Removed:** ~1,200+ lines

### Remaining:
**Files to DELETE:** ~5 files
**Files to MODIFY:** ~25 files
**Lines to remove:** ~500-800 lines
**Dependencies to remove:** 5-7 libraries

### Total Impact:
**Total Files to DELETE:** ~29 files (24 completed, 5 remaining)
**Total Files to MODIFY:** ~42 files (17 completed, 25 remaining)
**Total Lines Removed:** ~1,700-2,000 lines (1,200 completed, 500-800 remaining)
**Total Dependencies to Remove:** 5-7 libraries

**Result:** A leaner, simpler music player tailored to your workflow with only the features you actually use.
