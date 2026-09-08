<div align="center">

# ⏳ Kairos

**A minimalist, offline-first daily task planner, Eisenhower Matrix, Pomodoro timer, and peer-to-peer sync engine for Android.**

[![Android SDK](https://img.shields.io/badge/API-26%20to%2035%20(Android%2015)-3DDC84?logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit%20Tests-15%20Passing-success?logo=checkmarx&logoColor=white)](#-testing)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Cloud-00C853?logo=shield&logoColor=white)](#%EF%B8%8F-privacy--permissions)

</div>

---

## 📖 Overview

**Kairos** (*καιρός* — the ancient Greek concept of the opportune, decisive moment) is crafted for individuals who want intentional focus without corporate surveillance, mandatory subscriptions, or cloud lock-in.

Built from the ground up with **100% Jetpack Compose** and **Modern Android Architecture**, Kairos merges three essential productivity paradigms into a seamless, distraction-free mobile workspace:
1. **The Eisenhower Matrix** for rapid priority sorting.
2. **The Pomodoro Technique** for sustained deep work.
3. **Local Peer-to-Peer Wi-Fi Sync** for syncing with companion devices (tablets, wearables, Galaxy Watch) without internet access.

---

## ✨ Key Features

### 📋 Intelligent Task Management & Planning
* **Multi-View Workflow**: Switch instantly between **Today**, **Upcoming**, and **History** to review achievements.
* **Recurring Schedules**: Flexible recurrence rules (Daily, Weekdays, Weekly, or Custom interval).
* **Smart Drag / Manual Reordering**: Easily reprioritize tasks with intuitive reordering controls.
* **Daily Inspiration**: Curated stoic and focus quotes refreshed each morning.

### 🎯 Eisenhower Decision Matrix
Categorize every task into one of four actionable quadrants:
* 🟢 **Q1: Do First** — Urgent & Important
* 🔵 **Q2: Schedule** — Important, Not Urgent
* 🟡 **Q3: Delegate** — Urgent, Not Important
* ⚪ **Q4: Don't Do** — Neither Urgent nor Important

### ⏱️ Pomodoro Focus Mode
* **Deep Work & Rest Cycles**: Configurable focus intervals (default 25 min) and restorative break intervals (default 5 min).
* **Direct Task Linking**: Tie focus intervals to specific tasks to measure actual execution time.
* **Smart Audio Cues**: Distinct auditory tones signal the transition between focus and break periods.
* **Battery-Optimized Engine**: State updates run strictly when the timer is active; zero unnecessary CPU cycles while idle.

### 🔒 Zero-Cloud Peer-to-Peer Local Sync
* **100% Local Wi-Fi Sync**: Sync task state directly across local network sockets (`:45873`) without third-party servers.
* **Brute-Force Protection**: Built-in `SyncSecurityGate` locks out unauthorized pairing attempts after 5 incorrect PIN entries.
* **Wearable & Watch Friendly**: Optimized for companion devices such as Wear OS / Galaxy Watch.
* **Android 15 Ready**: Conforms to Android 15 foreground service policies with timeout and inactivity auto-shutdown safeguards.

### ⏰ Exact Reminders & Alarms
* Uses Android's `SCHEDULE_EXACT_ALARM` with system boot receivers (`RECEIVE_BOOT_COMPLETED`) to ensure reminders survive reboots.
* Actionable notifications to complete or snooze tasks directly from the lock screen.

### 💾 Backup & Data Portability
* Full local JSON backup export and import. Your data is always yours to export, inspect, or transfer.


---

## 🏗️ Architecture & Technology Stack

Kairos follows **Clean Architecture** principles and **Unidirectional Data Flow (UDF)**:

```
                  ┌───────────────────────────────┐
                  │    Jetpack Compose UI         │
                  │ (Material 3, Edge-to-Edge)    │
                  └───────────────┬───────────────┘
                                  │ Observes StateFlow / Dispatches Intents
                                  ▼
                  ┌───────────────────────────────┐
                  │       KairosViewModel         │
                  │ (Reactive State Management)   │
                  └───────┬───────────────┬───────┘
                          │               │
            ┌─────────────┴─────┐   ┌─────┴──────────────┐
            ▼                   ▼   ▼                    ▼
   ┌─────────────────┐ ┌──────────────────┐ ┌──────────────────────┐
   │ TaskRepository  │ │FocusTimerRepo /  │ │  LocalSync Engine    │
   │ (Atomic SQLite) │ │ReminderScheduler │ │ (Encrypted Socket /  │
   │ SharedFlow Bus  │ │ (AlarmManager)   │ │  Rate-Limited PIN)   │
   └────────┬────────┘ └──────────────────┘ └──────────┬───────────┘
            │                                          │
            ▼                                          ▼
     [ kairos.db ]                              [ Peer Device ]
```

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 components and edge-to-edge support.
* **Language & Coroutines**: 100% [Kotlin](https://kotlinlang.org) 2.0.21, Kotlin Coroutines, `StateFlow`, and `SharedFlow`.
* **Persistence**: SQLite with atomic transaction batching, IO-dispatched asynchronous queries, and zero UI-thread blocking.
* **Foreground Services**: Android 15 compliant `dataSync` foreground service with strict timeout handling.
* **Testing**: JUnit 4 unit tests covering domain logic, Eisenhower calculations, focus state transitions, and security lockout gates.

---

## 📂 Repository Structure

```text
Kairos/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kairos/daily/
│   │   │   │   ├── MainActivity.kt           # Edge-to-edge activity & notification launcher
│   │   │   │   ├── data/
│   │   │   │   │   ├── Task.kt               # Domain model (Eisenhower, recurrences)
│   │   │   │   │   ├── TaskDatabase.kt       # SQLiteOpenHelper schema & migrations
│   │   │   │   │   ├── TaskRepository.kt     # Reactive data access with SharedFlow events
│   │   │   │   │   ├── TaskBackup.kt         # JSON backup import/export
│   │   │   │   │   └── UserPreferences.kt    # Pomodoro & theme configurations
│   │   │   │   ├── focus/
│   │   │   │   │   ├── FocusTimer.kt         # Countdown logic & repository
│   │   │   │   │   └── FocusCompleteReceiver.kt # Broadcast receiver for timer alarms
│   │   │   │   ├── reminders/
│   │   │   │   │   ├── ReminderScheduler.kt  # Exact alarm scheduling
│   │   │   │   │   ├── ReminderReceiver.kt   # System notification dispatcher
│   │   │   │   │   └── BootReceiver.kt       # Alarm restoration after phone reboot
│   │   │   │   ├── sync/
│   │   │   │   │   ├── LocalSync.kt          # P2P TCP socket server & client
│   │   │   │   │   │                         # Includes SyncSecurityGate rate-limiter
│   │   │   │   │   └── SyncReceiverService.kt # Foreground service (Android 15 safe)
│   │   │   │   └── ui/
│   │   │   │       ├── KairosApp.kt          # Main navigation scaffold, views, sheets
│   │   │   │       ├── KairosViewModel.kt    # MVVM reactive state coordinator
│   │   │   │       ├── DailyQuotes.kt        # Daily quote catalog
│   │   │   │       └── theme/                # Material 3 ColorScheme, Typography
│   │   │   └── res/                          # Vector drawables, mipmaps, theme styles
│   │   └── test/                             # Comprehensive unit test suites
│   └── build.gradle.kts                      # Module build configuration
├── build.gradle.kts                          # Root build configuration
└── settings.gradle.kts                       # Project settings
```

---

## 🚀 Getting Started

### Prerequisites
* **JDK 17** (or newer)
* **Android Studio Ladybug (2024.2+)** or **Android SDK Command-Line Tools**
* **Target Device / Emulator**: Android 8.0 (API 26) through Android 15 (API 35)

### Clone & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/kairos.git
   cd kairos
   ```

2. **Run unit tests**:
   ```bash
   # Windows
   .\gradlew.bat testDebugUnitTest

   # macOS / Linux
   ./gradlew testDebugUnitTest
   ```

3. **Build the Debug APK**:
   ```bash
   .\gradlew.bat assembleDebug
   ```
   *Output APK:* `app/build/outputs/apk/debug/app-debug.apk`

4. **Build the Optimized Release APK**:
   ```bash
   .\gradlew.bat assembleRelease
   ```
   *Output APK:* `app/build/outputs/apk/release/app-release.apk`

---

## 📲 Direct Installation (Sideloading)

To install the built release APK directly on an Android device:
1. Transfer `app-release.apk` to the device (via USB, email, Google Drive, or chat).
2. Tap the APK file to begin installation.
3. When prompted:
   > *"For your security, your phone is currently not allowed to install unknown apps from this source."*
4. Tap **Settings** &rarr; toggle **"Allow from this source"** &rarr; tap **Install**.

---

## 🛡️ Privacy & Permissions

Kairos is built with a strict **Privacy-First** philosophy:

| Permission | Purpose | Why It's Needed |
| :--- | :--- | :--- |
| `POST_NOTIFICATIONS` | Alerts | Required on Android 13+ for task reminders and focus alarms. |
| `SCHEDULE_EXACT_ALARM` | Reminders | Delivers reminders at the exact minute requested. |
| `FOREGROUND_SERVICE_DATA_SYNC` | P2P Sync | Keeps the local Wi-Fi listener active while syncing with companion devices. |
| `INTERNET` & `ACCESS_NETWORK_STATE` | Local Sync | Restricted to local subnet communication (`192.168.x.x` / `:45873`). No external traffic. |
| `RECEIVE_BOOT_COMPLETED` | Persistence | Restores active task alarms automatically after device restart. |

> **Note**: Kairos contains **no analytics SDKs, no trackers, and no external ad networks**. All task notes, priorities, and schedules remain encrypted in device storage.

---

## 🧪 Testing

The project includes unit tests covering business logic, data structures, and security controls:
* `TaskTest`: Verifies Eisenhower matrix sorting, recurring date calculation, and priority positions.
* `FocusTimerStateTest`: Tests countdown intervals and break state transitions.
* `PlanningFilterTest`: Tests date-based task query segregation.
* `SyncSecurityGateTest`: Validates the 5-attempt rate limiter and 30-second brute-force lockout.

Run the test suite with:
```bash
.\gradlew.bat test
```

---

## 🤝 Contributing

Contributions, feedback, and issue reports are warmly welcomed!
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.
