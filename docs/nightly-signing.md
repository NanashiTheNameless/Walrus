# Nightly Signing Setup

This repo can build signed release APKs locally with `walrus-keystore.properties` or in
GitHub Actions with repository secrets.

## 1. Generate a release keystore

Keep using the same keystore for every future release. If you lose it, users will not be able
to install updates over older builds signed with that key.

```bash
keytool -genkeypair \
  -v \
  -keystore release-key.jks \
  -alias walrus-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

## 2. Configure local signing

For local release builds, create `../walrus-keystore.properties` next to the repo with:

```properties
storeFile=AndroidKeystore/release-key.jks
storePassword=your-keystore-password
keyAlias=walrus-release
keyPassword=your-key-password
```

Then place the keystore at `app/AndroidKeystore/release-key.jks`.

## 3. Add GitHub Actions secrets

Encode the keystore as a single-line base64 string:

```bash
base64 -w 0 release-key.jks > release-key.jks.b64
```

If your `base64` does not support `-w`, use:

```bash
base64 < release-key.jks | tr -d '\n' > release-key.jks.b64
```

Add these repository secrets:

```text
RELEASE_KEYSTORE
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Use these values:

- `RELEASE_KEYSTORE`: contents of `release-key.jks.b64`
- `RELEASE_KEYSTORE_PASSWORD`: keystore password
- `RELEASE_KEY_ALIAS`: alias used when creating the key, for example `walrus-release`
- `RELEASE_KEY_PASSWORD`: key password

## 4. Trigger a nightly build

Push to `main` or `master`, or run the `Nightly Build` workflow manually from the Actions tab.
