# NDK Ports Guide

This repository was forked from [the official NDK Ports repository](https://android.googlesource.com/platform/tools/ndkports/+/refs/heads/main). It provides instructions and tools for building and publishing C++ library ports for use with the Android NDK and React Native.

## Project Goal

The goal of this repository is to streamline publishing NDK ports for C++ libraries, enabling seamless integration on Android and React Native. The builds are managed via GitHub Actions, ensuring transparency and traceability in each step.

## Currently Supported Libraries

The following libraries are currently set up to be built with this project:
- `blst`
- `curl`
- `gmp`
- `jsoncpp`
- `libpng`
- `openssl`
- `utf8proc`
- `zlib`

You can manage library inclusion by adjusting the `settings.gradle.kts` file.

## Building a Port

Follow these steps to build a port with `ndkports`.

### 1. Prepare Your Environment

Ensure you have all necessary dependencies, particularly:
- Android NDK path configured if running locally

### 2. Run the Build Script

#### Running with Docker

If you prefer to build within a Docker container, use the provided `Dockerfile` to ensure all dependencies are managed consistently. Inside Docker, the following command runs the main build script:

```bash
./scripts/build_release.sh
```

This script compiles all specified libraries in settings.gradle.kts.

#### Running Locally

To run the build locally, use the following Gradle command and set the NDK path:

```bash
./gradlew -PndkPath="/Users/username/Library/Android/sdk/ndk/26.1.10909125" -Prelease release
```

This command will build all libraries specified in settings.gradle.kts for the release configuration.

### 3. Building a Specific Library

If you wish to build only a specific library (e.g., libpng), you can target it directly with Gradle:

```bash
./gradlew :openssl:distZip -PndkPath="/Users/username/Library/Android/sdk/ndk/26.1.10909125"
```

Replace openssl with the desired library name to customize the build further.