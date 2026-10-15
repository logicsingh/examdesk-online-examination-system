@echo off
cd /d "%~dp0"
echo Building ExamDesk and running unit tests...
mvn clean test package
if errorlevel 1 (
  echo Build or tests failed. Read the errors above.
  pause
  exit /b 1
)
echo Build successful. WAR file: target\online-examination-system.war
pause
