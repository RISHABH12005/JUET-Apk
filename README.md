# JUET Student Portal — Native Android App

A native Android app that embeds the official JUET Student Portal for live, user-initiated testing, alongside public university information and academic PDF resources.

**Status:** Independent student project, not an official JUET app. The Live Portal screen loads the official student portal in an Android WebView; sign-in and private records are handled by the university website. Test only with your own authorized student account.

## Features in this prototype

- Dashboard with overview cards and timetable preview
- Subject-wise attendance screen
- Semester results and sample grades
- Weekly timetable
- Notices screen
- Live official student portal WebView with JavaScript, DOM storage, navigation history and reload

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

The app opens `https://studentportal.juet.ac.in/studentportal/#/` inside a WebView for live portal testing. It also uses a public-information feed generated from the official JUET website and mirrored in `juet_public_info.json`. GitHub Actions attempts to refresh this feed every five minutes, and the Android app checks the feed every minute while the Notices screen is open. Actual freshness depends on the source website and successful sync runs. The feed contains public links and announcements only; it is not an official JUET API.

The official student portal's private backend has not been connected because no authorized API documentation or endpoint has been verified. Attendance, marks, fee balances, registered courses and personal profile data still require an approved university API or backend integration. Request authorization and API documentation from JUET administrators before connecting these records.

## Security

- The Live Portal screen sends sign-in data only to the official portal through its own page; the native app does not scrape credentials from the page.
- Attendance, marks, fees and profile values are not parsed into native app screens; use the official portal's own views.
- Do not share credentials or test accounts. Report any portal login or WebView incompatibility before relying on the app.
- Never embed API secrets in the Android application.
