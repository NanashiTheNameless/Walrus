#!/usr/bin/env bash

set -eu

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="$ROOT_DIR/.android-sdk"
LOCAL_PROPERTIES_FILE="$ROOT_DIR/local.properties"

find_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        echo "$JAVA_HOME"
        return 0
    fi

    for candidate in \
        /usr/lib/jvm/zing-jdk21 \
        /usr/lib/jvm/java-21-openjdk-amd64 \
        /usr/lib/jvm/temurin-21-jdk-amd64 \
        /usr/lib/jvm/msopenjdk-21-amd64 \
        /usr/lib/jvm/openjdk-21
    do
        if [ -x "${candidate}/bin/java" ]; then
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

find_sdkmanager() {
    if [ -x "${SDK_DIR}/cmdline-tools/latest/bin/sdkmanager" ]; then
        echo "${SDK_DIR}/cmdline-tools/latest/bin/sdkmanager"
        return 0
    fi

    if command -v sdkmanager >/dev/null 2>&1; then
        command -v sdkmanager
        return 0
    fi

    return 1
}

mkdir -p "$SDK_DIR"

if JAVA_HOME_VALUE="$(find_java_home)"; then
    export JAVA_HOME="$JAVA_HOME_VALUE"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using JAVA_HOME=$JAVA_HOME"
else
    echo "Warning: JDK 21 was not found automatically. Set JAVA_HOME to a JDK 21 install before building." >&2
fi

cat > "$LOCAL_PROPERTIES_FILE" <<EOF
sdk.dir=$SDK_DIR
EOF
echo "Wrote $LOCAL_PROPERTIES_FILE"

export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"

if ! SDKMANAGER="$(find_sdkmanager)"; then
    cat >&2 <<EOF
Unable to find sdkmanager.

Install Android command-line tools or Android Studio first, then re-run:
  $ROOT_DIR/setup.sh

If you already have the command-line tools zip, unpack it to:
  $SDK_DIR/cmdline-tools/latest
EOF
    exit 1
fi

echo "Using sdkmanager at $SDKMANAGER"

yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null
"$SDKMANAGER" --sdk_root="$SDK_DIR" \
    "cmdline-tools;latest" \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;36.0.0"

export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

echo
echo "Android SDK is set up in $SDK_DIR"
echo "Environment for this shell:"
echo "  export JAVA_HOME=${JAVA_HOME:-<set JDK 21 manually>}"
echo "  export ANDROID_SDK_ROOT=$SDK_DIR"
echo "  export ANDROID_HOME=$SDK_DIR"
