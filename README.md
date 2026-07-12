<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/fb8e41a8-5e31-4af3-8457-a4677e4e17b9

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Build APK Versi Release

### 1. Build Otomatis via GitHub Actions
Repositori ini telah dikonfigurasi dengan alur kerja (workflow) GitHub Actions pada file `.github/workflows/build-release.yml`:
1. Setiap kali melakukan *push* ke branch `main`/`master` atau menjalankan workflow secara manual via tab **Actions** (*Run workflow*).
2. GitHub Actions otomatis membuild file APK Release (`assembleRelease`).
3. File APK hasil build dapat diunduh langsung pada bagian **Artifacts** (`iuran-release-apk`) di halaman hasil workflow run.

### 2. Build Manual Secara Lokal
Untuk membuild APK Release secara lokal di terminal / Android Studio:
```bash
./gradlew assembleRelease
```
File APK akan berada di dalam direktori: `app/build/outputs/apk/release/`

