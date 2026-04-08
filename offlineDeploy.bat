@echo off
echo ==============================================
echo ARESLib2 Offline Rapid Deployment
echo ==============================================
echo Bypassing internet resolution and locking daemon...

.\gradlew build deploy --offline --daemon

if %ERRORLEVEL% neq 0 (
    echo [!] Deployment failed. Check the logs above.
    pause
    exit /b %ERRORLEVEL%
)

echo [OK] Deployment to Control Hub/RoboRIO complete!
pause
