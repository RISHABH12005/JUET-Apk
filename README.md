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

The public portal URL does not by itself establish an API available to third-party apps. No authorized API documentation or endpoint has been verified. Request permission and API documentation from the university administrators before integration.

The API contracts included in the source are placeholders and are not verified JUET endpoints. Configure a verified HTTPS API, implement secure authentication and token storage, and replace the sample data only after integration has been authorized and tested.

## Security

- Do not enter your real JUET password in the current demo.
- No official university credentials are transmitted by the demo sign-in.
- No university records are fetched.
- Never embed API secrets in the Android application.
