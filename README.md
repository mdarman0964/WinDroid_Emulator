# WinDroid Emulator

Android emulator focused on running MetaTrader 5 (`MT5`) with the minimum practical set of Windows compatibility components.

## Project goals

- Keep the app and repository as small as practical without removing runtime components required by MT5.
- Preserve the Wine/Box64, graphics, input, audio, and Android integration required for compatibility.
- Build a debug APK through GitHub Actions for device testing.

## Source

Based on [Stredohori/Winlator-CMOD](https://github.com/Stredohori/Winlator-CMOD), branch `winlator_bionic`. The upstream repository is archived; this project is maintained separately.

## Status

Bootstrap is in progress. The first workflow imports the upstream source into this repository and attempts a debug APK build. MT5 installation, graphics compatibility, and background operation must still be verified on a physical device.
