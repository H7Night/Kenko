@echo off
setlocal
set "ROOT=%~dp0.."
set "LOGDIR=%ROOT%\build"
set "LOG=%LOGDIR%\build_debug.log"

if not exist "%LOGDIR%" mkdir "%LOGDIR%"

echo Starting to build Debug APK...
rem Redirect Gradle output to a file so the long-lived Gradle daemon inherits the
rem file handle instead of the console pipe, which would otherwise keep waiting.
call "%ROOT%\gradlew.bat" assembleDebug > "%LOG%" 2>&1
set "CODE=%ERRORLEVEL%"

type "%LOG%"

if "%CODE%"=="0" (
    echo.
    echo Build Successful!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo Build Failed!
)
exit /b %CODE%
