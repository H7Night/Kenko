@echo off
setlocal
set "ROOT=%~dp0.."
set "LOGDIR=%ROOT%\build"
set "LOG=%LOGDIR%\build_and_install.log"

if not exist "%LOGDIR%" mkdir "%LOGDIR%"

echo Building and Installing Debug APK...
rem Redirect Gradle output to a file so the long-lived Gradle daemon inherits the
rem file handle instead of the console pipe, which would otherwise keep waiting.
call "%ROOT%\gradlew.bat" installDebug > "%LOG%" 2>&1
set "CODE=%ERRORLEVEL%"

type "%LOG%"

if "%CODE%"=="0" (
    echo.
    echo Success! The app has been installed and is ready to run.
) else (
    echo.
    echo Operation Failed! Please check the error messages above.
)
exit /b %CODE%
