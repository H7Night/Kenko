param(
    [switch]$NoPause
)

$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Starting to build Debug APK..."
& "$projectRoot\gradlew.bat" assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build Successful!"
    Write-Host "APK location: app\build\outputs\apk\debug\app-debug.apk"
} else {
    Write-Host ""
    Write-Host "Build Failed!"
}

if (-not $NoPause) { Read-Host "Press Enter to exit" }
exit $LASTEXITCODE
