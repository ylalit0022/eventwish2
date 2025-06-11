 @echo off
echo ===== RUNNING USER MODEL TESTS =====
echo Creating sample users...
node ./scripts/sample-users.js

echo.
echo Testing user model...
node ./scripts/test-user-model.js

echo.
echo Testing user routes...
node ./scripts/test-user-routes.js

echo.
echo ===== USER TESTS COMPLETED =====
pause