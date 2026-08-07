param(
    [switch]$NoPause
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$apk = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "Error: adb not found. Install Android SDK platform-tools and add it to PATH." -ForegroundColor Red
    if (-not $NoPause) { Read-Host "Press Enter to exit" }
    exit 1
}

if (-not (Test-Path $apk)) {
    Write-Host "Error: APK not found at $apk. Run build_debug.ps1 first." -ForegroundColor Red
    if (-not $NoPause) { Read-Host "Press Enter to exit" }
    exit 1
}

Write-Host "Installing Debug APK..."
adb install -r $apk

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Install Successful!"
} else {
    Write-Host ""
    Write-Host "Install Failed! Make sure your device is connected and USB debugging is enabled."
}

if (-not $NoPause) { Read-Host "Press Enter to exit" }
exit $LASTEXITCODE
