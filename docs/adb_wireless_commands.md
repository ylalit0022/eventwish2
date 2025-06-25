# ADB Wireless Debugging Commands Guide

This guide provides a comprehensive list of ADB (Android Debug Bridge) commands for wireless debugging. These commands are particularly useful when using your Android device's hotspot for development.

## Table of Contents
- [Initial Setup](#initial-setup)
- [Device Information](#device-information)
- [App Installation](#app-installation)
- [App Management](#app-management)
- [File Transfer](#file-transfer)
- [Debugging](#debugging)
- [Network Commands](#network-commands)
- [System Commands](#system-commands)
- [Multiple Device Handling](#multiple-device-handling)
- [Troubleshooting](#troubleshooting)

## Initial Setup

### Enable Wireless Debugging
```bash
# Step 1: Enable TCP/IP mode (run while USB connected)
adb tcpip 5555

# Step 2: Connect wirelessly (replace with your hotspot IP)
adb connect 192.168.25.42:5555

# Step 3: Verify connection
adb devices

# Step 4: Disconnect when needed
adb disconnect 192.168.25.42:5555
```

**Note**: Always replace `192.168.25.42` with your actual hotspot IP address.

## Device Information

### Get Device IP and Network Info
```bash
# Get hotspot (ap0) IP address
adb shell ip addr show ap0

# Get all network routes
adb shell ip route

# Get device model
adb shell getprop ro.product.model

# Get Android version
adb shell getprop ro.build.version.release
```

## App Installation

### Basic Installation Commands
```bash
# Install new APK
adb install path/to/app.apk

# Install with options
adb install -r path/to/app.apk    # Replace existing app
adb install -d path/to/app.apk    # Allow version downgrade
adb install -g path/to/app.apk    # Grant all permissions

# Uninstall app
adb uninstall com.package.name
adb uninstall -k com.package.name  # Keep data and cache
```

## App Management

### Package Management
```bash
# List all installed packages
adb shell pm list packages

# List third-party packages only
adb shell pm list packages -3

# Clear app data
adb shell pm clear com.package.name

# Force stop app
adb shell am force-stop com.package.name

# Launch app
adb shell monkey -p com.package.name 1
```

## File Transfer

### File Operations
```bash
# Copy file to device
adb push local/path/file.txt /sdcard/

# Copy file from device
adb pull /sdcard/file.txt local/path/

# List directory contents
adb shell ls /sdcard/

# Remove file from device
adb shell rm /sdcard/file.txt
```

## Debugging

### Logging and Screenshots
```bash
# View real-time logs
adb logcat

# Filter logs by package
adb logcat | grep "com.package.name"

# Clear log buffer
adb logcat -c

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .

# Record screen (press Ctrl+C to stop)
adb shell screenrecord /sdcard/video.mp4
```

## Network Commands

### Network Diagnostics
```bash
# Check network status
adb shell netstat

# View network interfaces
adb shell ifconfig

# Test internet connectivity
adb shell ping -c 4 google.com

# Get WiFi information
adb shell dumpsys wifi
```

## System Commands

### Device Control
```bash
# Reboot device
adb reboot

# Enter recovery mode
adb reboot recovery

# Enter bootloader
adb reboot bootloader

# Get battery information
adb shell dumpsys battery

# Get memory usage
adb shell dumpsys meminfo
```

## Multiple Device Handling

### Managing Multiple Connections
```bash
# List all connected devices
adb devices -l

# Target specific device
adb -s 192.168.25.42:5555 <command>

# Example: Install on specific device
adb -s 192.168.25.42:5555 install path/to/app.apk
```

## Troubleshooting

### Common Issues and Solutions
```bash
# Reset ADB server
adb kill-server
adb start-server

# Check ADB version
adb version

# Reset TCP/IP connection
adb tcpip 5555
```

### Connection Issues
If you experience connection problems:
1. Ensure phone and PC are on the same network
2. Check if hotspot IP hasn't changed
3. Try killing and restarting ADB server
4. Reconnect USB and re-enable TCP/IP mode

### Common Error Solutions
- **"device unauthorized"**: Kill and restart ADB server
- **"device offline"**: Disconnect, reset TCP/IP, and reconnect
- **"multiple devices"**: Use `-s` flag with specific device ID

## Best Practices

1. **Hotspot Usage**:
   - Keep phone and PC close for stable connection
   - Note that IP might change when restarting hotspot
   - USB connection needed only for initial setup

2. **Connection Management**:
   - Always verify connection with `adb devices`
   - Disconnect properly when done
   - Keep track of your hotspot IP address

3. **Development Workflow**:
   - Use `adb logcat` for real-time debugging
   - Keep USB cable handy for reconnection
   - Regular backup of important data

## Quick Reference

### Most Used Commands
```bash
adb tcpip 5555                    # Enable wireless
adb connect 192.168.25.42:5555    # Connect wireless
adb devices                       # List devices
adb install path/to/app.apk       # Install app
adb logcat                        # View logs
adb shell                         # Enter shell
```

Remember to replace:
- `192.168.25.42` with your actual hotspot IP
- `com.package.name` with your app's package name
- `path/to/app.apk` with your actual APK path 