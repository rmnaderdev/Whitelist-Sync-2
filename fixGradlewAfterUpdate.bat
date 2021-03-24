@echo off
:: This seemed to fix it: .\gradlew wrapper --gradle-version 5.0
.\gradlew clean
.\gradlew genIntelliJRuns --no-daemon
.\gradlew --refresh-dependencies
pause