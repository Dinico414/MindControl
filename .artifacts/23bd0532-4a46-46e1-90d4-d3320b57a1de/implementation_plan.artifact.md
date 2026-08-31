# Refactor Project Structure

Organize the project into a clean package structure by moving UI components, layouts, and resources into their respective directories under `ui/`.

## User Review Required

> [!IMPORTANT]
> This is a massive refactoring that involves moving almost all UI-related files and splitting `MainActivity.kt`. It will significantly change the project structure.

## Proposed Changes

### [Directory Structure]

#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/res/watchfaces`
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts`

---

### [Component: Watchfaces]

#### [MOVE] `app/src/main/java/com/xenonware/mindcontrol/ui/res/PixelWatchFace.kt` -> `app/src/main/java/com/xenonware/mindcontrol/ui/res/watchfaces/PixelWatchFace.kt`
#### [MOVE] `app/src/main/java/com/xenonware/mindcontrol/ui/res/AodStyles.kt` -> `app/src/main/java/com/xenonware/mindcontrol/ui/res/watchfaces/AodStyles.kt`

---

### [Component: Layouts]

#### [MOVE] `app/src/main/java/com/xenonware/mindcontrol/CustomKeyboardScreen.kt` -> `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/CustomKeyboardScreen.kt`
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/MainScreen.kt` (Extract from `MainActivity.kt`)
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/GridScreen.kt` (Extract from `MainActivity.kt`)
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/TogglesContainer.kt` (Extract from `MainActivity.kt`)
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/ButtonConfigScreen.kt` (Extract from `MainActivity.kt`)
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/layouts/ActionSelectionScreen.kt` (Extract from `MainActivity.kt`)

---

### [Component: Other Resources]

#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/res/ActionIcons.kt` (Extract `ActionIcon` from `MainActivity.kt`)
#### [NEW] `app/src/main/java/com/xenonware/mindcontrol/ui/res/Dialogs.kt` (Extract dialogs from `MainActivity.kt`)

---

### [Updates]

#### [MODIFY] `MainActivity.kt`
#### [MODIFY] `WatchActivity.kt`
#### [MODIFY] `QrCodeActivity.kt`

## Verification Plan

### Automated Tests
- `gradlew assembleDebug` to ensure everything compiles and links correctly.

### Manual Verification
- Deploy to device/emulator and verify all screens (Main, Keyboard, AOD, Config) still work as expected.
