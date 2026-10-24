<p align="center">
  <img src="banner.png" width="1000" alt="Project Logo">
</p>

# MindControl 🧠📱
  <img src="https://img.shields.io/github/v/release/Dinico414/MindControll?style=for-the-badge&color=orange&logo=github" alt="Latest Version">   <img src="https://img.shields.io/github/downloads/Dinico414/MindControll/total?style=for-the-badge&color=blue&logo=github" alt="Total Downloads">

[![Android](https://img.shields.io/badge/Platform-Android%2015%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Shizuku](https://img.shields.io/badge/Power-Shizuku%20Compatible-informational)](https://shizuku.rikka.app/)
[![Root](https://img.shields.io/badge/Power-Root%20%2F%20KernelSU%20%2F%20Magisk-crimson)](https://github.com/tiann/KernelSU)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-yellow?logo=buy-me-a-coffee)](https://www.buymeacoffee.com/xenonware)

**MindControl** is a deep-integration hardware customization toolkit, physical-keyboard / gamepad mapper, and Always-On Display (AOD) engine designed for Android devices and specifically optimized for the iKKO MindOne. It allows you to intercept and remap physical buttons and keys, run a full XBOX-Style controller layer, handle screen-off hardware events via privileged shell access (Shizuku or Root), dynamically adjust display aspects and DPI, and enjoy a rich, modular AOD experience.

---

## 📸 Screenshots
| Main Dashboard | Button Config | AOD |
| :---: | :---: | :---: |
| <img src="Screenshots/Screenshot_20260607-172011.png" width="200"><br><sub>Main Dashboard</sub> | <img src="Screenshots/Screenshot_20260607-172111.png" width="200"><br><sub>Buttonconfig Screen</sub> | <img src="Screenshots/Screenshot_20260607-172754.png" width="200"><br><sub>Concentric screen with Mediaplayer</sub> |
| <img src="Screenshots/Screenshot_20260607-172036.png" width="200"><br><sub>Theme selector and Keyboard button</sub> | <img src="Screenshots/Screenshot_20260607-172122.png" width="200"><br><sub>Action selector</sub> | <img src="Screenshots/Screenshot_20260607-172941.png" width="200"><br><sub>Nothing Dot Stacked</sub> |
| <img src="Screenshots/Screenshot_20260607-172054.png" width="200"><br><sub>Keyboard Dashboard</sub> | <img src="Screenshots/Screenshot_20260607-172145.png" width="200"><br><sub>AOD Select Action</sub> | <img src="Screenshots/Screenshot_20260607-173104.png" width="200"><br><sub>Digital AOD With Media action triggered and charging state</sub> |
| | <img src="Screenshots/Screenshot_20260607-172331.png" width="200"><br><sub>QR-Code dialog</sub> | |

---

## 🚀 Key Features

### 1. Dual Privileged Engine (Shizuku & Root)
MindControl v3.0 introduces native support for both **Shizuku** and **Root** (KernelSU, Magisk, APatch, and standard `su`):
*   **Zero UI Blocking:** Permission detection runs reactively via background Kotlin Coroutines / StateFlows, preventing any frame drops or ANRs.
*   **Screen-Off Remapping via Kernel Monitor:** When the screen is off, standard accessibility services cannot intercept input. MindControl spawns a privileged low-level kernel event monitor (`getevent -l`) that translates raw hardware scancodes (volume rocker, power, camera shutter, two-stage focus sensor, AI buttons) in real time.
*   **Secured System Controls:** Execute system-level tasks without cumbersome permission hoops, such as changing aspect ratios, display density, airplane/mobile data, location, and brightness.

### 2. Physical Button & Trigger Remapping
*   **Hardware Supported:** Volume Rockers, Camera Shutter, Half-press Focus Sensor, Dedicated AI / Assistant buttons, Physical Keyboards, Gamepads, and Camera orientation sensors.
*   **Multi-Trigger Gestures:**
    *   `Single`, `Double`, `Triple`, `Four` and `Five` press patterns.
    *   `Long Press` (Hold), `Press and Hold`, and `2x Press & Hold` detection.
*   **Contextual States:** Assign distinct behaviors depending on whether the screen is **On** or **Off**.
*   **Per-Button Override Toggle:** Enable or disable the custom remapping of each individual button, so an unbound button simply keeps its stock behavior.

### 3. Gamepad Mode (XBOX Controller Layer)
MindControl can present a **virtual XBOX controller** to the system, built on a native `uinput` helper — no game needs to know the physical keyboard exists:
*   **Per-App Activation:** Choose the apps whose keyboard switches to the gamepad layout in `Gamepad keyboard apps`; everywhere else the phone stays in normal keyboard mode.
*   **Manual Toggle:** Start or stop the controller on demand from the dashboard, in any app.
*   **Full Controller Layout:** Face buttons (`A / B / X / Y`), D-pad, bumpers, `L3` / `R3`, `Start` / `Menu` / `Select`, and analog stick directions.
*   **Three Switchable Profiles:** Keep three independent controller mappings and switch between them at runtime — either from the action picker (`Profile 1 / 2 / 3`) or with the preset `c` / `v` / `b` keys on the home row. Profile 1 ships a ready-to-use preset.
*   **Rumble Feedback:** The controller advertises force-feedback; the game's rumble requests are read back from the helper and played on the phone's real vibrator.
*   **Virtual D-pad for Navigation:** Arrow actions are routed through a synthetic gamepad device tagged `SOURCE_GAMEPAD`, so Android's Focus Navigation Engine moves focus exactly like a physical XBOX controller instead of the app swallowing the key.

### 4. Continuum: Continuous Fluid Execution
MindControl includes a **Continuum Engine** for fluid system interactions:
*   **How it Works:** Assign an action to the **Hold** or **Press and Hold** trigger.
*   **Continuous Repeat:** As long as the physical button remains depressed, the assigned action continuously and smoothly repeats (ideal for volume ramping, smooth page scrolling, and fine brightness adjustment).

### 5. Custom Always-On Display (AOD)
A fully custom, low-overhead AOD built with Jetpack Compose. Can be assigned as an action to any button trigger or combination.

**Standalone Watch Faces:**
*   `Concentric` (Pixel Watch concentric rings)
*   `Spinner` (Rotating abstract clock indicators)
*   `Blocks` & `Bars` (Bold geometric watchfaces)
*   `Track` (Circular progress-ring clock)
*   `Cube` (3D rotating cube clock)
*   `Digital Abstract` (Modern abstract digital segments)

**Analog Clock** — one watch face with three switchable substyles:
*   `Classic` (traditional analog hands with modern dial ticks)
*   `Planets` (dynamic orbital visualization)
*   `Abstract` (modern abstract hands)

**Text Clock** — one watch face with a 2×4 grid of layout / substyle combinations, plus full font control:

| | `Modern` | `Nothing Dot` | `Digital` | `Pixel` |
| :--- | :--- | :--- | :--- | :--- |
| **`Stacked`** | Stacked (Modern) | Nothing Dot (Stacked) | Digital (Stacked) | Pixel (Stacked) |
| **`Inline`** | Inline (Modern) | Nothing Dot (Inline) | Digital (Inline) | Pixel (Inline) |

**Text Clock Customization:** Pick the `Inline` or `Stacked` layout and one of the `Modern` / `Nothing Dot` / `Digital` / `Pixel` substyles, then fully customize the font — family (`Quicksand`, Sans-Serif, Serif, Monospace, Cursive), weight (`Light` → `Extra Bold`) and italic.

**Shared across all watch faces:**
*   **Dynamic Media Art:** Renders the currently playing album art as a subtle, blurred and vignetted ambient backdrop.
*   **Smart Indicators:** Real-time mirroring of notification tray icons, charging animations, and battery percentage.
*   **Simple Dismissal:** Press power or swipe up to immediately return to your lock screen or desktop.

### 6. Display, Aspect Ratio & Density Customization
Switch display geometry on the fly using physical button mappings:
*   **Aspect Ratios:** Full Screen, 4:3, 16:9, Cycle Aspect Ratio, or Step Up/Down.
*   **Screen Densities (DPI):** Density 200, 300, 400, Density Cycle, or apply custom DPI profiles.
*   **Custom Resolutions:** Quickly toggle between native resolution and custom aspect configurations, or open a dialog to enter an exact custom width, height and density (with a live calculated aspect ratio).

### 7. Universal Physical Keyboard & Input Mapping
*   **Regional Layouts:** Native layout switching for `QWERTY`, `QWERTY (Spanish)`, `QWERTZ` (German/Central European), and `AZERTY` (French).
*   **Visual Key Mapper:** Interactive on-screen keyboard map lets you visualize and bind custom actions to individual physical keys.
*   **Key Injection:** Send single key presses, letters with `Shift` / `Ctrl` / `Alt` modifiers, arrow keys, `Tab` and `Reverse Tab` directly to the focused app.
*   **Better Shift:** A dedicated Shift action that taps for `Shift` and holds for `Shift Lock`, reacting instantly and always overriding the keyboard.
*   **Emoji Keyboard:** Open the system emoji picker from any physical key. (requires Shizuku / Root)
*   **Zero-Latency Remapping:** Per-button mode where a single press fires the moment the key is released — no multi-press waiting window, ideal for fast typing. The `,` key ships preconfigured this way.
*   **Permanent Keyboard Overwrite:** Keep the remapped behavior active even while a text field is focused.

### 8. QR-Code Sharing
Generate a QR code on the fly from any button — great for sharing a link, phone number or block of text without typing it out. Rendered in-app with ZXing and themed to match your device palette.

### 9. Instant Shortcuts From the Lock Screen
A dedicated overlay activity lets a button trigger launch an app or shortcut directly from a locked / screen-off state, waking the screen, dismissing the keyguard and a black frame appears over the whole screen so there are no distractions for short actions.

### 10. Adaptive Theming & Dynamic Icons
*   **Adaptive App Icons:** The launcher icon dynamically adapts to match your device casing (Black, White, Pink, Blue).
*   **Device & Keyboard Palettes:** Theme both the main interface and physical keyboard mapper to match your hardware style.

---

## 🛠 Action Library (70+ Actions)
<details>
<summary><b>Click to expand the available actions</b></summary>

> **Note:** Actions marked with **(Continuum)** support continuous hold execution. Actions marked with **(Privileged)** require Shizuku or Root.

| Category | Actions |
| :--- | :--- |
| **Navigation** | Home, Back, Recents, Last App, Show Menu (Privileged) |
| **Media** | Play/Pause, Next, Previous, Stop, Fast Forward, Rewind, Step Forward/Back, **Volume Up/Down (Continuum)**, Volume Dialog, Mute Volume, Mute Microphone Toggle (Privileged) |
| **Connectivity** | WiFi Toggle (Privileged), Bluetooth Toggle (Privileged), Mobile Data Toggle (Privileged), NFC Toggle (Privileged), Location Toggle (Privileged), Do Not Disturb |
| **Display & Geometry** | Aspect Ratio (Full / 4:3 / 16:9 / Cycle / Up / Down / Full+4:3+16:9) (Privileged), Screen Size (Full / 4:3 / 16:9 / Custom) (Privileged), Display Density (200 / 300 / 400 / Cycle / Up / Down / Custom) (Privileged), **Brightness Up/Down (Continuum)**, Auto-Brightness Toggle (Privileged), Auto-Rotate Toggle, Rotate 360°, Toggle AOD |
| **System** | Flashlight, Screenshot, Lock Screen, Assistant, Power Dialog, Notifications Panel, Quick Settings Panel, Cycle Sound Mode, Vibrate Ringer |
| **Navigation & Scroll** | **Scroll Up/Down (Continuum)**, **Smooth Scroll Normal/Fast (Continuum)**, Copy, Cut, Paste, App Info, Google Search |
| **Keyboard & Input** | Key Press / Letter (with Shift / Ctrl / Alt modifiers), **Arrow Up/Down/Left/Right**, Shift Hold, Shift Lock, Tab, Reverse Tab, Open Emoji Keyboard |
| **Gamepad** | Profile 1, Profile 2, Profile 3 (see *Gamepad Mode*) |
| **Custom & Shortcuts** | Launch App, Launch Custom Shortcut, Speed Dial, Open Custom URL, Show QR Code Dialog |

</details>

---

## 🏗 Technical Architecture

1.  **Accessibility Key Filtering:** Intercepts key events using `AccessibilityService.FLAG_REQUEST_FILTER_KEY_EVENTS` when the screen is active.
2.  **Kernel Event Monitoring:** When the screen turns off, MindControl falls back to reading raw scancodes via `/system/bin/getevent -l` through Shizuku or Root, ensuring reliable triggers even under aggressive OEM sleep states.
3.  **Native `uinput` Gamepad Helper:** A small NDK-built executable (`virtual_dpad.c`) stages an executable copy to disk via privileged shell, then creates synthetic input devices through `/dev/uinput` — a D-pad device tagged `SOURCE_GAMEPAD` for Focus Navigation, and a resident rumble-capable XBOX-identified pad with axes. It speaks a line-based stdin/stdout protocol and reports back the app's force-feedback requests.
4.  **Device-Protected Storage (`Direct Boot`):** User configurations and root statuses are kept in encrypted device-protected storage (`createDeviceProtectedStorageContext()`), allowing MindControl to initialize and function immediately upon boot before the first user unlock.
5.  **Shortcut Overlay Handoff:** A dedicated `ShortcutOverlayActivity` wakes the screen, dismisses the keyguard and holds a black frame across the transition, finishing only once the launched app's window fully covers the screen — never flashing the launcher.
6.  **Local & Reactive Architecture:** State is driven by Kotlin Coroutines `StateFlow` and Jetpack Compose without polling loops or blocking threads.

---

## 📥 Installation & Setup

1.  **Install the APK** from the [Releases](https://github.com/Dinico414/MindControll/releases) page.
2.  **Enable Accessibility Service:** Open `Settings > Accessibility > MindControl` and toggle it ON. (If greyed out on Android 13+, tap the 3 dots in App Info and choose *"Allow restricted settings"*).
3.  **Privileged Access (Recommended):**
    *   **Root:** If your device is rooted (KernelSU / Magisk / APatch), grant root access when prompted or from your root manager.
    *   **Shizuku:** If unrooted, launch [Shizuku](https://shizuku.rikka.app/) and authorize MindControl.
4.  **Battery Optimization:** Exclude MindControl from battery optimization to keep the background monitoring service persistent.
5.  **Notification Listener (Optional):** Enable notification access to display media controls and notification badges on the AOD.
6.  **Gamepad Mode (Optional):** Open *Gamepad keyboard apps* and tick the apps that should switch to the controller layout, or start the controller manually from the dashboard. Requires Shizuku or Root for the native `uinput` helper.

---

## 🛡 Privacy & Security

*   **No Internet Access:** MindControl requests **zero** network permissions (`android.permission.INTERNET` is not included in the manifest). All data, actions, and key events stay strictly on your device.
*   **No Analytics or Tracking:** No telemetry, third-party trackers, or cloud dependencies.
*   **Open Source:** Full source code is open and verifiable.

---

## ☕ Support the Project

If you find MindControl helpful, consider supporting its development!

<p align="left">
  <a href="https://www.buymeacoffee.com/xenonware">
    <img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=☕&slug=xenonware&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" alt="Buy Me A Coffee" />
  </a>
</p>

---

## 👨‍💻 Developer & Credits
*   **Company:** Xenonware
*   **Lead:** Nico ([Dinico414](https://github.com/Dinico414))

---
*Disclaimer: This application uses Accessibility Services and privileged shell APIs. It is an independent project by Xenonware and is not affiliated with Nothing Technology Ltd., iKKO, Google LLC, or any OEM.*
