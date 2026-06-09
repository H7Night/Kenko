$projectRoot = Split-Path -Parent $PSScriptRoot
$apk = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Installing Debug APK..."
adb install -r $apk

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Install Successful!"
} else {
    Write-Host ""
    Write-Host "Install Failed! Make sure your device is connected and USB debugging is enabled."
}

Read-Host "Press Enter to exit"