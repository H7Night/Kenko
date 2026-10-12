$projectRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $projectRoot "build"
$log = Join-Path $logDir "build_and_install.log"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "Building and Installing Debug APK..."
# Redirect Gradle output to a file so the long-lived Gradle daemon inherits the
# file handle instead of this shell's output pipe. Otherwise the pipeline keeps
# waiting for the daemon to release it after the build already finished.
cmd /c "`"$projectRoot\gradlew.bat`" installDebug > `"$log`" 2>&1"
$code = $LASTEXITCODE

Get-Content -LiteralPath $log

if ($code -eq 0) {
    Write-Host ""
    Write-Host "Success! The app has been installed and is ready to run."
} else {
    Write-Host ""
    Write-Host "Operation Failed! Please check the error messages above."
}

exit $code
