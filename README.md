# WinDroid Emulator

WinDroid is an Android compatibility environment focused on running MetaTrader 5 (MT5) with the smallest practical runtime footprint.

## Goals

- Prioritize installing and running the official MetaTrader 5 Windows desktop terminal.
- Avoid bundling games and unrelated desktop applications.
- Keep large ImageFS and Proton archives out of the APK; download and verify them on first launch.
- Retain the Wine/Proton, Box64/FEX, graphics, networking, and input components needed for Windows compatibility.
- Build a debug APK through GitHub Actions for physical-device testing.

## Features in progress

- Official MetaTrader 5 installer downloader in the app navigation.
- Background runtime service for minimized/screen-off operation.
- Problem notes and a log/lock-file viewer in the Android-side navigation.
- A clipboard-sharing toggle in the Windows display side menu.

## Source and licensing

Based on [Stredohori/Winlator-CMOD](https://github.com/Stredohori/Winlator-CMOD), branch `winlator_bionic`. Upstream notices and license files are retained. WinDroid is an independent project and is not affiliated with MetaQuotes.

## Current status

The initial full debug build succeeded. Large runtime archives have been moved to first-run downloads, and optional legacy graphics packages have been removed to reduce the APK. The optimized build has passed CI. MT5 installation, graphics compatibility, background persistence, and actual device behavior still require physical-device verification.

## Branding audit

The app label, user-facing storage folder, and X11 app-name string use WinDroid branding. Upstream source and license references remain where required for attribution. A complete branding audit must also cover embedded assets, native components, and bundled third-party notices; these must not be renamed blindly if doing so breaks compatibility or misrepresents upstream licensing.
