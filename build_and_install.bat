@echo off
echo Building and Installing Debug APK...
call gradlew.bat installDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Success! The app has been installed and is ready to run.
) else (
    echo.
    echo Operation Failed! Please check the error messages above.
)
pause