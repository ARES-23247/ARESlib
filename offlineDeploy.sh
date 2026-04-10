#!/usr/bin/env bash
echo "=============================================="
echo "ARESLib Offline Rapid Deployment"
echo "=============================================="
echo "Bypassing internet resolution and locking daemon..."

./gradlew build deploy --offline --daemon

if [ $? -ne 0 ]; then
    echo "[!] Deployment failed. Check the logs above."
    exit 1
fi

echo "[OK] Deployment to Control Hub/RoboRIO complete!"
exit 0
