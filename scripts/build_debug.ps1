$projectRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $projectRoot "build"
$log = Join-Path $logDir "build_debug.log"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "Starting to build Debug APK..."
# Redirect Gradle output to a file so the long-lived Gradle daemon inherits the
# file handle instead of this shell's output pipe. Otherwise the pipeline keeps
# waiting for the daemon to release it after the build already finished.
cmd /c "`"$projectRoot\gradlew.bat`" assembleDebug > `"$log`" 2>&1"
$code = $LASTEXITCODE

Get-Content -LiteralPath $log

if ($code -eq 0) {
    Write-Host ""
    Write-Host "Build Successful!"
    Write-Host "APK location: app\build\outputs\apk\debug\app-debug.apk"
} else {
    Write-Host ""
    Write-Host "Build Failed!"
}

exit $code
