# Implementation Plan - Specialized Fajr Adhan Integration

This plan outlines the integration of specialized Fajr Adhan support (including the traditional Fajr phrase *"الصلاة خير من النوم" / "Assalatu khayrun minan nawm"* — Prayer is better than sleep) into Paisa's Adhan notification and audio player systems.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Fajr Adhan Specialization**: Adding a dedicated Fajr Adhan audio stream / URL and explicit notification text highlighting "Assalatu khayrun minan nawm" when Fajr prayer time is reached.

- **Confirmed Decision**: Dedicated Fajr Adhan audio and visual notification styling.
- **Open Choice**: Default Fajr audio source mapped to specialized Makkah/Madinah Fajr reciter file.

---

### 1. Overview & Core Concept

- **What It Does**: Enhances the Adhan notification and playback engine to detect Fajr prayer time and play/notify with the specialized Fajr Adhan audio containing the morning call addition ("Assalatu khayrun minan nawm").
- **Target Audience**: Users relying on Paisa for precise Islamic prayer times and Adhan notifications.
- **Key Value**: Accurate religious adherence and high audio-visual quality for the dawn prayer.

---

### 2. User Experience & Visual Design

- **Key User Flows**: 
  1. User receives Fajr notification or opens Adhan Player at Fajr time.
  2. App displays "ফজরের আযান — الصلاة خير من النوم (আস-সালাতু খাইরুম মিনান নাওম)" prominently on the notification and player screen.
  3. Specialized Fajr Adhan audio plays cleanly via `AdhanPlaybackService`.
- **Visual Identity & Theme**:
  - *Color Palette*: Deep dawn teal (`#0D9488`) and indigo/amber accents representing dawn twilight.
  - *Typography*: Clean M3 typography with Arabic support for Fajr call phrasing.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1**: Dedicated Fajr Adhan asset / URL mapping.
  - *Chosen Approach*: Maintain a dedicated Fajr Adhan audio source URL in `AdhanAudioProvider` and `AdhanPlaybackService`.
  - *Why*: Ensures the correct Adhan variant with "Assalatu khayrun minan nawm" is played specifically for Fajr.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────┐
│       AdhanPreferences          │
│   (Fajr Adhan URL & Settings)   │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│      AdhanAudioProvider         │
│  (Provides Fajr-specific URL)   │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│    AdhanPlaybackService         │
│ (Plays Fajr Adhan + Special text│
└─────────────────────────────────┘
```

- **Data Model & State**:
  - Add `FAJR_ADHAN_URL` and specialized Fajr notification strings in `strings.xml`.
- **Interactive Component Mapping**:
  - `AdhanPlaybackService.kt`: Detect prayer name == "Fajr" (or "ফজর") and load Fajr-specific Adhan audio stream.
  - `PrayerNotificationWorker.kt`: Include "Assalatu khayrun minan nawm" text in the Fajr notification channel and banner.
