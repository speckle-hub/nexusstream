@echo off
setlocal
cd /d "%~dp0\.."

echo [1/3] Fetching libmpv (skipped when already present)...
call gradlew.bat fetchLibmpv
if errorlevel 1 goto :error

echo [2/3] Building portable app image...
call gradlew.bat portableZip
if errorlevel 1 goto :error

echo [3/3] Building MSI installer...
call gradlew.bat packageMsi
if errorlevel 1 goto :error

echo.
echo Build complete:
echo   build\distributions\Moovio-1.0.0-windows-x64.zip
echo   build\compose\binaries\main\msi\Moovio-1.0.0.msi
exit /b 0

:error
echo Build failed.
exit /b 1
