@echo off
echo Installing Debug APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Install Successful!
) else (
    echo.
    echo Install Failed! Make sure your device is connected and USB debugging is enabled.
)
pause