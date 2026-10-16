# JUET Student Portal — Native Android App

A native Android application prototype for a modern, student-focused JUET portal interface.

**Status:** UI prototype with illustrative sample data. This is not an official JUET app and is not currently connected to university services. Do not enter real JUET credentials.

## Features in this prototype

- Dashboard with overview cards and timetable preview
- Subject-wise attendance screen
- Semester results and sample grades
- Weekly timetable
- Notices screen
- Student profile and demo sign-in

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM-style ViewModel state
- Retrofit API interface scaffold
- Gradle / Android Studio

## Build the APK

1. Download or clone this repository.
2. Open the project folder in Android Studio.
3. Install the Android SDK components requested by Android Studio and sync Gradle.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK, if compilation succeeds, is created at `app/build/outputs/apk/debug/app-debug.apk`.

## JUET backend integration

The app now uses a public-information feed generated from the official JUET website and mirrored in `juet_public_info.json`. GitHub Actions attempts to refresh this feed every five minutes, and the Android app checks the feed every minute while the Notices screen is open. Actual freshness depends on the source website and successful sync runs. The feed contains public links and announcements only; it is not an official JUET API.

The official student portal's private backend has not been connected because no authorized API documentation or endpoint has been verified. Attendance, marks, fee balances, registered courses and personal profile data still require an approved university API or backend integration. Request authorization and API documentation from JUET administrators before connecting these records.

## Security

- Do not enter your real JUET password in the current demo.
- No official university credentials are transmitted by the demo sign-in.
- No university records are fetched.
- Never embed API secrets in the Android application.
