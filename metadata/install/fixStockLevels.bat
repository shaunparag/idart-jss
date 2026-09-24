@echo off
set ROOT="%~dp0"

set TMP_CLASSPATH=%CLASSPATH%

set CLASSPATH=.\target\
rem Add all jars....

for %%i in (".\lib\*.jar") do call ".\cpappend.bat" %%i
for %%i in (".\bin\*.jar") do call ".\cpappend.bat" %%i

set PHARM_CLASSPATH=%CLASSPATH%;.
set CLASSPATH=%TMP_CLASSPATH%

if defined JAVA_HOME (
	set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
) else (
	where javaw.exe >nul 2>nul
	if errorlevel 1 (
		echo JAVA_HOME is not set and javaw.exe was not found on PATH. Please install a Java Runtime Environment and set JAVA_HOME.
		exit /b 1
	)
	set "JAVA_CMD=javaw.exe"
)

start  "iDART" "%JAVA_CMD%" -cp "%PHARM_CLASSPATH%" -Djava.library.path=%ROOT:~0,-2%" -Xms24m -Xmx2048m org.celllife.idart.start.FixStockLevels %*



