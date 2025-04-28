@echo off
echo Setting up environment for building the Nether Gauntlet mod...

REM Try to find Java installation
for /d %%i in ("C:\Program Files\Java\*") do (
    if exist "%%i\bin\java.exe" (
        echo Found Java at: %%i
        set "JAVA_HOME=%%i"
        goto :found_java
    )
)

for /d %%i in ("C:\Program Files\Eclipse Adoptium\*") do (
    if exist "%%i\bin\java.exe" (
        echo Found Java at: %%i
        set "JAVA_HOME=%%i"
        goto :found_java
    )
)

echo Java not found in standard locations.
echo Please install JDK 17 and try again.
goto :end

:found_java
echo Setting JAVA_HOME to %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Building the mod...
call .\gradlew.bat build

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo The mod JAR file should be in the build\libs directory.
) else (
    echo Build failed with error code %ERRORLEVEL%
)

:end
pause
