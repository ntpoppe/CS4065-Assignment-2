# Server README

This server is implemented in **Java**.

## Prerequisites

Java JDK must be installed on your system.  
On Windows, you can download it [here](https://www.oracle.com/java/technologies/downloads/).  
On Unix/Linux/macOS, you can install it using your package manager.
For example, on Ubuntu, you can install it using:
```bash
sudo apt-get update
sudo apt-get install openjdk-21-jdk
```

## Compilation

### Unix/Linux/macOS

The server includes a `Makefile` located in the `server/` directory. To compile the server:

```bash
cd server
make
```

This will compile all Java source files from `src/` and generate the necessary `.class` files in `out/production/server/`.

### Windows

On Windows, use the provided batch file to compile and run:

```cmd
cd server
compile-and-run.bat [port]
```

This will compile to `out\production\server\server\` (IntelliJ defaults here...) and run the server. If no port is specified, the server defaults to port 8000.

Alternatively, compile manually (PowerShell):

```powershell
cd server
if (-not (Test-Path "out\production\server")) { New-Item -ItemType Directory -Path "out\production\server" -Force }
javac -d out\production\server src\server\*.java
```

## Running

### Unix/Linux/macOS

To run the server using the Makefile (after compiling):

```bash
cd server
make run
```

### Windows

On Windows, if you've already compiled, you can run:

```powershell
cd server
java -cp out\production\server\server server.Main [port]
```

Or use the batch file which compiles and runs:

```powershell
cd server
.\compile-and-run.bat [port]
```

If no port is specified, the server defaults to port 8000.
