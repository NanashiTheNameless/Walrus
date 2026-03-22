# Walrus

> [!IMPORTANT]
> This repository is an unofficial fork of the original Walrus project.
> It is maintained separately and is not an official Team Walrus release.
> Behavior, UI, dependencies, and supported features may differ from upstream.
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

## Installing

There are two simple ways to install this fork.

### Option 1: Install a prebuilt APK

1. Download an APK from this fork's release pages:
   - Nightly builds: https://github.com/NanashiTheNameless/Walrus/releases/tag/nightly
2. Copy the APK to your Android device if you downloaded it elsewhere.
3. Open the APK on the device and allow installation from unknown apps if Android asks.
4. Finish the install and launch Walrus.

### Option 2: Build and install from source

1. Install Android Studio or a local Android SDK setup.
2. Use JDK 21.
3. Clone this repository.
4. Build an APK:

```bash
./gradlew assembleDebug
```

For a release build:

```bash
./gradlew assembleRelease
```

Generated APKs are written to `app/build/outputs/apk/`.

To install a debug build directly onto a connected device:

```bash
./gradlew installDebug
```

## Development

Walrus was developed by Daniel Underhay and Matthew Daley (a.k.a. [Team Walrus](<mailto:team@walrus.app>)!) and is Open Source

This fork is maintained by [@NanashiTheNameless](<https://github.com/NanashiTheNameless/Walrus>)

## Building

Walrus is a standard Android Studio project. Run the Gradle wrapper with JDK 21. This project currently uses Android Gradle Plugin 9.1.0 and Gradle 9.4.1. After selecting JDK 21, open the project in Android Studio or run the Gradle wrapper directly.

TODO: When we refresh and remove the current Google Maps API key from the repo, we'll need to point out that this needs to be generated and set manually if maps are needed.

For signed nightly builds and GitHub Actions keystore setup, see [docs/nightly-signing.md](docs/nightly-signing.md).

## Codebase

The current layout of Walrus's source code is as follows:

* `/app/src/main`

  * `/assets`: Any non-resource assets, like the open source license listing.

  * `/res`: Resource files.

  * `/java/dev/namelessnanashi/walrus`: Actual code lives here!

    * `/card`: Code to do with persistent data (i.e. the wallet). The `Card` class, the base `CardData` class and various card data type classes, database models and database helpers are here.

    * `/device`: Device-agnostic and device-specific driver code. The important `CardDeviceManager` lives here alongside the base `CardDevice` class and its child classes for various basic kinds of device (serial, line-based, etc.). Code to handle bulk reading is also located here.

      * `/proxmark3`: Proxmark3 driver code.

      * `/chameleonmini`: Surprise! Chameleon Mini driver code.

    * `/ui`: Code to do with other UI.

    * `/util`: Miscellaneous.

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

We welcome all kinds of contributions and bug reports, big or small! Development takes place at our [GitHub repository](https://github.com/NanashiTheNameless/Walrus). There you can file issues (both bugs and enhancement requests) and submit pull requests.

During the initial development of Walrus, changes to the codebase are likely to be frequent and wide-ranging, so if you want to work on a feature, it's wise to reach out first to ensure that your hard work won't be soon obsoleted. After our first full release we hope to gain stability and bring in some of the additional resources expected of a project today, such as a proper test suite and continuous integration.

## Note on contributions from AI

AI-generated pull requests are not accepted. All pull requests must be authored by a human, include a clear description of the changes, include tests or verification steps where applicable, and follow the project's contribution guidelines.
