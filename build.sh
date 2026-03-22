#!/usr/bin/env bash

set -eu

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="$ROOT_DIR/.android-sdk"

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
            echo "${candidate}"
            return 0
        fi
    done

    return 1
}

cd "$ROOT_DIR"

"$ROOT_DIR/setup.sh"

if JAVA_HOME_VALUE="$(find_java_home)"; then
    export JAVA_HOME="$JAVA_HOME_VALUE"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

if [ "$#" -eq 0 ]; then
    set -- assembleDebug
fi

exec "$ROOT_DIR/gradlew" --build-cache "$@"
