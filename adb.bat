@echo off
REM Helper script to make ADB easier to use
REM Usage: adb.bat command [args]

echo Running ADB command: %*
"D:\Sdk\platform-tools\adb.exe" %* 