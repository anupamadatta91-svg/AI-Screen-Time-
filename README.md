# AI Screen Time Tracker

An Android application for an academic Digital Industry & AI project.

## Project idea

The app measures how long a user spends in mobile applications, identifies the apps consuming the most time, compares total usage with a configurable daily limit, and generates an explainable AI-style wellbeing score.

### Main features

- Reads Android application usage statistics.
- Shows total screen/app usage for the current day.
- Shows the apps with the highest usage.
- Configurable daily limit (30 minutes–8 hours).
- AI-style risk score from 0–100.
- AI-generated/explainable recommendations based on usage behavior.
- Local-first: no account, server, or database is required.
- Android notification permission is included for future alert functionality.

## Important Android limitation

Android does not let a normal app silently read other apps' usage data without the user's explicit Usage Access approval.

The application therefore sends the user to:

**Settings → Apps → Special app access → Usage access → AI Screen Time → Allow**

The project uses `UsageStatsManager`, which is Android's official API for device usage history.

## AI design

The first version uses an explainable local scoring model:

- 40%: total usage compared with the user's daily limit
- 35%: concentration in the most-used app
- 25%: number of apps used for at least one hour

The result is classified as:

- Healthy: < 35
- Moderate: 35–64
- High: >= 65

This is intentionally transparent for an academic prototype. It is not a medical or psychological diagnosis.

## Alert design

The UI already supports configurable limits. A production version can add a background worker that checks the current day's total periodically and sends a notification when the limit is crossed.

Android 13+ requires notification permission for normal notifications.

## Technology

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- UsageStatsManager
- Explainable local AI/risk scoring

## How to open

1. Install Android Studio.
2. Open this folder as an existing Gradle project.
3. Let Gradle sync.
4. Connect an Android phone with USB debugging enabled or start an emulator.
5. Run the `app` configuration.
6. Open Usage Access when the app asks for it.
7. Allow AI Screen Time.
8. Return to the app and press Refresh.

## GitHub

Repository name:

`ai-screen-time-tracker`


8. Add export to CSV for academic analysis.

## Privacy

The application should clearly explain why Usage Access is required. Do not collect passwords, messages, screen contents, or personal communications. The project only needs aggregate app usage duration and package/application labels.
