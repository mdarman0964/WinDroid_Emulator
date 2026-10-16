# WinDroid Emulator

WinDroid is an Android compatibility environment focused on running MetaTrader 5 (MT5) with the smallest practical runtime footprint.

## Goals

- Prioritize installing and running the official MetaTrader 5 Windows desktop terminal.
- Keep the app focused: no bundled games or unrelated desktop applications.
- Retain only runtime components required for Windows compatibility, graphics, networking, and input.
- Provide a repeatable GitHub Actions debug APK build for physical-device testing.

## Source and licensing

Based on [Stredohori/Winlator-CMOD](https://github.com/Stredohori/Winlator-CMOD), branch `winlator_bionic`. Upstream notices and license files are retained. WinDroid is an independent project; it is not affiliated with MetaQuotes.

## Current status

The upstream source has been imported. The first full Android build is running. MT5 installation, graphics compatibility, background persistence, and device behavior must be verified before this build can be considered ready for daily use.
