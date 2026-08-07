param(
    [switch]$NoPause
)

$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Building and Installing Debug APK..."
& "$projectRoot\gradlew.bat" installDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Success! The app has been installed and is ready to run."
} else {
    Write-Host ""
    Write-Host "Operation Failed! Please check the error messages above."
}

if (-not $NoPause) { Read-Host "Press Enter to exit" }
exit $LASTEXITCODE
