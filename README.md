# Walrus

> [!IMPORTANT]
> This repository is an unofficial fork of the original Walrus project.
> It is maintained separately and is not an official Team Walrus release.
> Behavior, UI, dependencies, and supported features **WILL** differ from upstream.
>
> Original upstream project:
> https://github.com/TeamWalrus/Walrus
>
> This fork:
> https://github.com/NanashiTheNameless/Walrus
>
> If you are reporting a problem with this build or this repository, please open the issue on this
> fork rather than on the upstream Team Walrus repository.

## Intro

Walrus is an Android app for contactless card cloning devices such as the Proxmark3 and Chameleon Mini. Using a simple interface in the style of Google Pay, access control cards can be read into a wallet to be written or emulated later.

Designed for physical security assessors during red team engagements, Walrus supports basic tasks such as card reading, writing and emulation, as well as device-specific functionality such as antenna tuning and device configuration. More advanced functionality such as location tagging makes handling multiple targets easy, while bulk reading allows the stealthy capture of multiple cards while “war-walking” a target.

This fork currently supports Android 10 (API 29) and newer.

## Translations

English: 
Non-English translations in this fork are currently machine-translated first passes.
Corrections, improvements, and native-speaker reviews are welcomed and appreciated.

Deutsch: 
Die nicht-englischen Übersetzungen in diesem Fork sind derzeit maschinell erstellte Erstfassungen.
Korrekturen, Verbesserungen und Überprüfungen durch Muttersprachler sind willkommen und werden sehr geschätzt.

Español: 
Las traducciones no inglesas de este fork son actualmente borradores iniciales traducidos automáticamente.
Las correcciones, mejoras y revisiones por hablantes nativos son bienvenidas y muy agradecidas.

Français:
Les traductions non anglaises de ce fork sont actuellement des premières versions traduites automatiquement.
Les corrections, améliorations et relectures par des locuteurs natifs sont les bienvenues et très appréciées.

Português (Brasil): 
As traduções não inglesas deste fork são atualmente versões iniciais traduzidas por máquina.
Correções, melhorias e revisões por falantes nativos são bem-vindas e muito apreciadas.

日本語: 
このフォークの英語以外の翻訳は、現在のところ機械翻訳による初稿です。
修正、改善、ネイティブスピーカーによるレビューを歓迎しており、とてもありがたく思います。

## Installing

There are two simple ways to install this fork.

### Option 1: Install a prebuilt APK

1. Download an APK from this fork's release pages:
   - Nightly builds: https://github.com/NanashiTheNameless/Walrus/releases/tag/nightly
2. Copy the APK to your Android device if you downloaded it elsewhere.
3. Open the APK on the device and allow installation from unknown apps if Android asks.
4. Finish the install and launch Walrus.

### Option 2: Build and install from source

1. Install Android Studio or Android command-line tools so `sdkmanager` is available.
2. Use JDK 21.
3. Clone this repository.
4. Run the setup script:

```bash
./setup.sh
```

This will:
- create a repo-local Android SDK in `.android-sdk`
- write `local.properties`
- install `cmdline-tools;latest`, `platform-tools`, `platforms;android-34`, and `build-tools;36.0.0`

5. Build the app:

```bash
./build.sh
```

That defaults to `assembleDebug`.

For a release build:

```bash
./build.sh assembleRelease
```

To install a debug build directly onto a connected device:

```bash
./build.sh installDebug
```

Generated APKs are written to `app/build/outputs/apk/`.

## Development

Walrus was originally developed by Daniel Underhay and Matthew Daley (a.k.a. [Team Walrus](<mailto:team@walrus.app>)!) and is Open Source

This fork is maintained by [@NanashiTheNameless](<https://github.com/NanashiTheNameless/Walrus>)

## Building

Walrus is a standard Android Studio project. This fork currently uses Android Gradle Plugin 9.1.0 and Gradle 9.4.1, and it expects JDK 21.

For local command-line builds, the simplest flow is:

```bash
./setup.sh
./build.sh
```

`setup.sh` prepares the repo-local SDK and `local.properties`. `build.sh` runs setup first and then invokes the Gradle wrapper with the repo-local SDK environment.

For signed nightly builds and GitHub Actions keystore setup, see [docs/nightly-signing.md](docs/nightly-signing.md).

## Codebase

The repo is still centered around a single Android app module, but this fork has a few extra moving pieces worth calling out:

- `/app/src/main/java/dev/namelessnanashi/walrus`: Main application code.
  - `/card`: Wallet models, persistence, card-data types, and card editing/view flows.
  - `/device`: Device discovery, shared device abstractions, bulk-read logic, and device-specific implementations.
  - `/device/proxmark3`: Proxmark3 support.
  - `/device/chameleonmini`: Chameleon Mini support.
  - `/ui`: Shared activities like settings, web views, and the built-in map screen.
  - `/util`: Utility and compatibility helpers.
- `/app/src/main/java/com/afollestad/materialdialogs`: Local compatibility shim for the old Material Dialogs API used by the app.
- `/app/src/main/res`: Android resources, including layouts, drawables, strings, preferences, and bundled fonts.
- `/app/src/main/assets`: Bundled static assets, including the in-app open source acknowledgements page.
- `/app/src/test`: JVM unit tests.
- `/.github/workflows`: CI and nightly build automation.
- `/docs`: Project-specific maintenance and release docs.
- `/setup.sh` and `/build.sh`: Local SDK/bootstrap/build helpers for command-line development.

## Open Source Acknowledgements

This fork includes upstream Walrus code, vendored compatibility code, bundled fonts, third-party libraries, and map/data attributions.

The authoritative shipped acknowledgements page is:

- `/app/src/main/assets/open_source.html`

That page is also exposed inside the app through `Settings -> Open source acknowledgements`.

The main externally sourced pieces currently called out there are:

- Team Walrus / upstream Walrus
- 0xType 0xProto
- AndroidX and Material Components
- MapLibre, OpenFreeMap, and OpenStreetMap
- the remaining direct runtime libraries bundled with the app

## Device Support

Here’s a table of the current devices / card type pairs we support and in what manner.

**Key**: R = reading, W = writing, U = upload

|                   | Proxmark3 Original | Pm3 Evo | Pm3 RDV4 | Pm3 Iceman Fork | Chameleon Mini Rev.G | C.M Rev.E Rebooted |
|-----------------------|:------------------:|:-------:|:--------:|:---------------:|:--------------------:|:--------------:|
| **HID Prox**          | R / W              | R / W   | R / W    | R / W           | -                    | -              |
| **ISO14443A - UID**   | -                  | -       | -        |  -              | R / U                | U              |
| **Mifare Ultralight** | -                  | -       | -        |  -              | -                    | -              |
| **Mifare Classic 1K** | R / W              | R / W   | R / W    | R / W           | U                    | U              |
| **Mifare Classic 4K** | ?                  | ?       | ?        | ?               | ?                    | ?              |
| **Mifare Classic 4B** | ?                  | ?       | ?        | ?               | ?                    | ?              |
| **Mifare Classic 7B** | ?                  | ?       | ?        | ?               | ?                    | ?              |
| **Mifare DESFire**    | ?                  | ?       | ?        | ?               | ?                    | ?              |


## Contributing

This fork welcomes all kinds of contributions and bug reports, big or small! Development takes place at the fork's [GitHub repository](https://github.com/NanashiTheNameless/Walrus). There you can file issues (both bugs and enhancement requests) and submit pull requests.

## Note on contributions from AI

AI-generated pull requests are not accepted. All pull requests must be authored by a human, include a clear description of the changes, include tests or verification steps where applicable, and follow the project's contribution guidelines.
