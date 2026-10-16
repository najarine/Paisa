# Paisa: Fullscreen Adhan Alert, Offline Audio Reliability & UI/UX Polish

Comprehensive architectural blueprint to introduce a dedicated incoming-call style Adhan screen, guarantee reliable offline Adhan playback, eliminate overlapping background audio and Adhan tester confusion, preserve exact scroll positions across navigation, and polish the application's overall Material 3 UI/UX.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following technical and UX choices have been confirmed and integrated into this master plan:
> - **Adhan Alert Presentation**: Fullscreen incoming call-style UI (`AdhanAlarmActivity`) with prominent Stop ("আজান বন্ধ করুন") and Mute ("মিউট করুন") controls, capable of turning on the screen and displaying over the lockscreen.
> - **Offline Playback & Alarm Concurrency**: Pre-cache selected Adhan MP3 files to persistent internal storage (`filesDir/adhans/`). Enforce a strict 15-minute staleness threshold so delayed alarms from device sleep or reboots are discarded. Enforce a single-instance audio playback mutex to eliminate multiple Adhans playing at once when returning online.
> - **Do Not Disturb (DND) & Silent Mode**: Follow standard Android Alarm best practices—respect system Ringer & DND policies appropriately while providing crystal-clear in-app settings.
> - **Scroll Position Preservation**: Hoist and persist scroll states across all screens and sub-screens in `PaisaViewModel` so returning from settings or sub-pages restores the exact previous scroll position.
> - **Adhan Tester Isolation**: Strictly decouple test audio previews from the actual prayer alarm engine so testing never activates alarm services, notification banners, or full-screen overlays.

---

## 1. Overview & Core Concept

### What It Does
1. **Fullscreen Call-Style Adhan Alert (`AdhanAlarmActivity`)**: When a scheduled prayer time arrives, an incoming call-style screen appears with pulsing visual waves, prayer name in Bangla and Arabic calligraphy, prayer time, and high-visibility action buttons to immediately stop or mute the Adhan. It turns the screen on and operates seamlessly over the device lockscreen.
2. **Reliable Offline Playback**: Pre-caches the user's chosen regular and Fajr Adhans in internal app storage. Even when completely disconnected from the internet, the alarm fires on time using hardware `AlarmClockInfo`.
3. **No More Audio Overlaps or Stale Alarms**: Eliminates concurrent Adhan playback when phones reconnect to the internet by strictly separating `WorkManager` (background data sync only) from `AlarmManager` (time-critical alarms only), discarding any alarm delayed by more than 15 minutes, and enforcing a synchronized playback mutex.
4. **Scroll State Retention**: Preserves scroll offsets across all screens (Home, Islamic, Wallets, Transactions, Settings, Adhan Prayer Times). Tapping a setting or sub-feature and pressing Back returns the user to the exact position on the page without jumping to the top.
5. **Unified Material 3 UI/UX Polish**: Standardizes card elevations, typography hierarchy (Bangla + English), padding grids, and interaction feedback across all finance and Islamic features.

---

## 2. User Experience & Visual Design

### Key User Flows

#### A. Scheduled Prayer Adhan (Call-Style Alert)
```
Phone in pocket / Locked screen / Inside another app
                     │
         [ Exact Prayer Time Arrives ]
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│               Paisa • ওয়াক্তের সালাত                   │
│                                                        │
│                     ( ( 🕌 ) )                         │
│               [ Pulsing Teal Rings ]                   │
│                                                        │
│                     আসরের নামাজ                         │
│                  حي على الصلاة                         │
│               সময়: ৪:২৫ PM • মক্কা আযান               │
│                                                        │
│        [ 🔇 মিউট ]                 [ ⛔ আজান বন্ধ ]       │
│    (Mute audio & dismiss)      (Stop playback & log)   │
└────────────────────────────────────────────────────────┘
```
- **Visuals**: Deep navy and dark teal gradient background (`PaisaBackground` to `PaisaTealSurface`), glowing circular mosque emblem, smooth breathing pulse animation.
- **Actions**:
  - **আজান বন্ধ করুন (Call End / Stop)**: Immediately stops audio, removes foreground service, closes alarm activity, and logs prayer.
  - **মিউট করুন (Mute)**: Silences audio while keeping prayer details visible.

#### B. In-Settings Adhan Preview (Tester Isolation)
- In `AdhanPrayerTimesScreen`, the "টেস্ট আজান" button only plays an in-place local preview with an inline Stop button.
- It **never** triggers `AdhanPlaybackService`, never shows the full-screen alarm screen, and never generates notifications.

#### C. Navigational Scroll Retention
- User scrolls down on `IslamicScreen` or `HomeScreen` to reach bottom cards.
- User taps "আযান ও নামাজের সময়" (Adhan Prayer Times).
- User adjusts settings and taps the TopBar Back arrow or system Back button.
- The previous screen immediately re-renders at the **exact item index and pixel offset**, preventing disruptive jump-to-top jumps.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Dedicated `AdhanAlarmActivity` vs In-App Compose Overlay
- **Decision**: Create a dedicated `AdhanAlarmActivity` in AndroidManifest with `android:showWhenLocked="true"` and `android:turnScreenOn="true"`, invoking `setShowWhenLocked(true)` and `setTurnScreenOn(true)` in Kotlin.
- **Why**: Android 10+ restricts starting background activities or displaying full-screen overlays from services unless dispatched via `NotificationCompat.Builder.setFullScreenIntent` with a dedicated, lockscreen-aware Activity. In-app composables only render if the app was already foregrounded and unlocked.

### 2. Internal Storage (`filesDir/adhans/`) vs Cache Directory (`cacheDir`)
- **Decision**: Store user-selected regular and Fajr Adhan audio files in `context.filesDir.resolve("adhans")`.
- **Why**: Android OS cleans `cacheDir` under low-memory conditions. Placing active Adhan audio in internal storage guarantees that alarms will sound even when the device is offline and storage is low.

### 3. Alarm Staleness Window (15 Minutes)
- **Decision**: Check `(System.currentTimeMillis() - scheduledPrayerMillis)` upon receiving an alarm intent. If the difference exceeds 15 minutes (e.g. phone was powered off or in deep Doze across multiple prayers), silently log and drop the alarm.
- **Why**: Prevents multiple past prayers from queueing up and blasting simultaneously the moment the device reconnects or powers on.

### 4. Audio Playback Mutex (`synchronized` lock & `AtomicReference`)
- **Decision**: Wrap MediaPlayer initialization in a synchronized block. Any incoming play request immediately stops, releases, and nullifies the prior MediaPlayer instance before creating a new one.
- **Why**: Eliminates audio overlap bugs caused by near-simultaneous worker or broadcast triggers.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
                 [ AlarmManager: setAlarmClock() ]
                                 │
                   (Time Arrival / Offline Wakeup)
                                 │
                                 ▼
                    [ PrayerAlarmReceiver ]
                                 │
                 ┌───────────────┴───────────────┐
                 │ Staleness Check (> 15 min)?   │
                 └───────────────┬───────────────┘
                                 │
                        No       │       Yes
                   ┌─────────────┴─────────────┐
                   │                           │
                   ▼                           ▼
        [ AdhanPlaybackService ]         [ Discard Event ]
         (Acquires WakeLock)
                   │
      ┌────────────┴───────────────────────────┐
      │                                        │
      ▼                                        ▼
[ FullScreenIntent ]               [ DefaultAdhanAudioProvider ]
      │                                        │
      ▼                                        ▼
[ AdhanAlarmActivity ]              [ Single-Player Mutex ]
- showWhenLocked=true               - Stop & release active player
- turnScreenOn=true                 - Check Ringer / DND mode
- Fullscreen Call UI                - Play from internal filesDir
- Stop / Mute buttons
```

### Component & State Mapping

| Component | Responsibility | Key Interactions / State |
| :--- | :--- | :--- |
| `AdhanAlarmActivity` | Dedicated fullscreen call-style alarm UI | Displays over lockscreen; provides Stop and Mute buttons; automatically finishes when audio finishes or user dismisses. |
| `PrayerAlarmReceiver` | Catches RTC_WAKEUP alarms from AlarmManager | Validates 15-minute staleness window; dispatches `AdhanPlaybackService` with FullScreenIntent to `AdhanAlarmActivity`. |
| `DefaultAdhanAudioProvider` | Central audio engine & cache manager | Holds synchronized lock; pre-caches active adhans in `filesDir/adhans/`; checks DND/silent settings; manages isolated preview vs alarm modes. |
| `ScrollStateHolder` / `PaisaViewModel` | Retains scroll positions across all views | Stores `LazyListState` and scroll offsets keyed by `AppScreen`; ensures Back navigation restores exact scroll position. |
| `PaisaApp` & Screens | M3 visual presentation & navigation | Connects screens with hoisted scroll states; unifies card typography, padding, and elevation. |

---

## 5. Execution Steps

1. **Step 1: Dedicated Fullscreen `AdhanAlarmActivity`**
   - Create `AdhanAlarmActivity.kt` with modern call-style Jetpack Compose UI (pulsing mosque emblem, prayer info, Stop & Mute buttons).
   - Configure manifest declarations: `showWhenLocked="true"`, `turnScreenOn="true"`, `screenOrientation="portrait"`, `USE_FULL_SCREEN_INTENT`.
2. **Step 2: Adhan Audio Provider & Offline Caching Hardening**
   - Refactor `DefaultAdhanAudioProvider.kt` with a synchronized playback mutex, persistent local storage (`filesDir/adhans`), and isolated preview player for settings.
   - Implement DND & Ringer mode checks (`AudioManager.RINGER_MODE_SILENT` / `VIBRATE`) following Android audio best practices.
3. **Step 3: Alarm Staleness & Concurrency Elimination**
   - Update `PrayerAlarmReceiver.kt` and `AdhanScheduler.kt` with 15-minute staleness validation to drop old alarms.
   - Configure high-priority FullScreenIntent pointing directly to `AdhanAlarmActivity`.
4. **Step 4: Hoisted Scroll State Manager**
   - Add scroll state retention in `PaisaViewModel` for `HomeScreen`, `IslamicScreen`, `AdhanPrayerTimesScreen`, `WalletsScreen`, and `MoreAndAnalyticsScreen`.
   - Update screen composables to bind to the retained list states so navigating back retains exact scroll offsets.
5. **Step 5: UI/UX & Card Hierarchy Refinement**
   - Polish spacing, elevation, and typography across all cards in `IslamicScreen` and `AdhanPrayerTimesScreen`.
   - Verify complete end-to-end compilation with `compile_applet`.
