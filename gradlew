#!/usr/bin/env sh
set -eu
GRADLE_VERSION="9.6.1"
EXPECTED_SHA256="9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
BOOTSTRAP_DIR="$GRADLE_USER_HOME/bootstrap"
GRADLE_HOME="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
ZIP_PATH="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION-bin.zip"

verify_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL="$(sha256sum "$1" | awk '{print $1}')"
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL="$(shasum -a 256 "$1" | awk '{print $1}')"
  else
    echo "sha256sum or shasum is required to verify the Gradle distribution." >&2
    exit 1
  fi
  if [ "$ACTUAL" != "$EXPECTED_SHA256" ]; then
    echo "Gradle distribution SHA-256 mismatch. Refusing to execute it." >&2
    rm -f "$1"
    exit 1
  fi
}

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$BOOTSTRAP_DIR"
  if [ ! -f "$ZIP_PATH" ]; then
    if ! command -v curl >/dev/null 2>&1; then
      echo "curl is required to download Gradle $GRADLE_VERSION." >&2
      exit 1
    fi
    curl --fail --location --retry 3 \
      "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
      --output "$ZIP_PATH"
  fi
  verify_sha256 "$ZIP_PATH"
  if ! command -v unzip >/dev/null 2>&1; then
    echo "unzip is required to extract Gradle." >&2
    exit 1
  fi
  unzip -q -o "$ZIP_PATH" -d "$BOOTSTRAP_DIR"
fi

exec "$GRADLE_BIN" "$@"
