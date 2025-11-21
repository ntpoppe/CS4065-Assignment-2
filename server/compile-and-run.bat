@echo off
REM Create output directory if it doesn't exist
if not exist "out\production\server" mkdir "out\production\server"

REM Compile Java files
javac -d out\production\server src\server\*.java

REM Check if compilation was successful
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

REM Run the server
java -cp out\production\server server.Main %*

