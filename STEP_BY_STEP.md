# Controlle.rs — Quick Setup Guide

## Overview
Controlle.rs lets you use your Android phone as a PlayStation 5 controller for your Linux desktop. The app sends input over WiFi via UDP, and a Rust server on your desktop creates a virtual gamepad.

---

## Step 1: Set Up the Desktop Server

### Prerequisites
- A Linux system with `uinput` support (most distros have this by default).
- Rust installed (use `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh` if you don’t have it).
- Access to `/dev/uinput` (see below).

### Permissions for `/dev/uinput`
To allow the server to create a virtual controller, you need access to `/dev/uinput`:

```bash
# Add your user to the input group
sudo usermod -aG input $USER
# Apply the group change (log out and back in, or run this):
newgrp input
```

### Build and Run

```bash
# Build the server
cd /home/user/controllers
cargo build --release

# Run the server
./target/release/controllers
```

To verify, check for the virtual controller:
```bash
cat /proc/bus/input/devices | grep -A 5 "PlayStation"
```

---

## Step 2: Install the Android App

The APK is located at:
```
android/app/build/outputs/apk/release/controllers-release.apk
```

### Install the APK

```bash
# Install via ADB (requires USB debugging enabled on your phone)
adb install android/app/build/outputs/apk/release/controllers-release.apk
```
Or transfer the APK to your phone manually and install it. You may need to enable "Install from unknown sources" in your phone settings.

---

## Step 3: Connect

1. Find your desktop’s IP address:
   ```bash
   ip addr show | grep 'inet ' | grep -v 127.0.0.1
   ```
2. Open the app on your phone.
3. Enter your desktop’s IP address and leave the port as `42069` (unless you changed it).
4. Tap **Connect**. The status indicator will turn green when connected.

---

## Step 4: Play

- **Sticks:** Drag your finger in the left/right stick zones to move them. The further you drag, the more the stick moves.
- **Triggers:** Tap L2/R2 for full press. Tap again to release.
- **Buttons:** Tap the on-screen buttons as you would on a real controller.

---

## Troubleshooting

### Permission Issues with `/dev/uinput`
If you see "Permission denied," ensure you’ve added your user to the `input` group:
```bash
sudo usermod -aG input $USER
newgrp input
```
Or run the server with `sudo` (not recommended long-term):
```bash
sudo ./target/release/controllers
```

### Connection Problems
- Ensure both devices are on the same WiFi network.
- Double-check the IP address and port.
- Check your firewall settings:
  ```bash
  sudo ufw allow 42069/udp
  ```

### High Latency
- Use a 5GHz WiFi network.
- Reduce the distance between devices.
- Close bandwidth-heavy apps on your network.

---

## Rebuilding the APK

If you need to rebuild the APK:

```bash
# Build the Rust JNI library for Android
cd controllers-core
cargo ndk -t arm64-v8a -o ../android/app/src/main/jniLibs build --release

# Build the APK
cd ../android
./gradlew assembleRelease
```

The APK will be at:
```
android/app/build/outputs/apk/release/controllers-release.apk
```
