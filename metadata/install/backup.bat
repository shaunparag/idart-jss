@echo off
:: --------------------------------------------------------------
:: backup.bat
::
:: Backs up the iDART database to a date-stamped file in the
:: iDART-backups folder of your user profile, for example
:: C:\Users\YourName\iDART-backups. That folder is outside the
:: iDART install folder, so uninstalling iDART can't delete it.
::
:: Copy the backup files off this PC regularly, for example to a
:: USB drive or a network folder. To restore a backup, see
:: "Backing up and restoring" in WINDOWS11_INSTALL.md.
::
:: The database settings below are filled in by the iDART
:: installer. Each time iDART starts, it sets them to the database
:: it opened, so don't change them here: change the database in
:: iDART's connection settings instead.
::
:: Originally written by Nico Gevers, Cell-Life, September 2006.
:: --------------------------------------------------------------

setlocal

set "dbHost=$dbAddress"
set "dbPort=5432"
set "dbName=$dbName"
set "dbUser=$dbUser"

:: Leave postgresDir blank to use the pg_dump.exe found under
:: %ProgramFiles%\PostgreSQL or on the PATH. Set it to a PostgreSQL
:: bin folder to use that one instead.
set "postgresDir="

set "PG_DUMP="
if defined postgresDir set "PG_DUMP=%postgresDir%\pg_dump.exe"
if not defined PG_DUMP for /d %%D in ("%ProgramFiles%\PostgreSQL\*") do if exist "%%D\bin\pg_dump.exe" set "PG_DUMP=%%D\bin\pg_dump.exe"
if not defined PG_DUMP for /f "delims=" %%P in ('where pg_dump.exe 2^>nul') do if not defined PG_DUMP set "PG_DUMP=%%P"
if not defined PG_DUMP (
	echo Could not find pg_dump.exe, which comes with PostgreSQL.
	echo Open this file in Notepad and set postgresDir to the PostgreSQL
	echo bin folder, for example C:\Program Files\PostgreSQL\18\bin
	echo.
	pause
	exit /b 1
)

:: Date and time for the file name. PowerShell gives the same format
:: on every PC whatever the Windows date settings are; the fallback
:: only runs if PowerShell is blocked.
set "STAMP="
for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm"') do set "STAMP=%%T"
if not defined STAMP set "STAMP=%date%_%time:~0,5%"
set "STAMP=%STAMP:/=-%"
set "STAMP=%STAMP::=%"
set "STAMP=%STAMP: =_%"
set "STAMP=%STAMP:.=-%"

set "BACKUP_DIR=%USERPROFILE%\iDART-backups"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
set "BACKUP_FILE=%BACKUP_DIR%\iDART-%dbName%-%STAMP%.backup"

echo Backing up database %dbName% on %dbHost% to
echo %BACKUP_FILE%
echo.
echo When asked, enter the PostgreSQL password for user %dbUser%.
echo.

"%PG_DUMP%" -h %dbHost% -p %dbPort% -U %dbUser% -F c -Z 7 -f "%BACKUP_FILE%" %dbName%
set "RESULT=%errorlevel%"

:: One-line ifs rather than ( ) blocks, so a user folder name that
:: contains brackets can't break the script.
echo.
if not "%RESULT%"=="0" if exist "%BACKUP_FILE%" del "%BACKUP_FILE%"
if not "%RESULT%"=="0" echo BACKUP FAILED. See the message above. No backup was saved.
if "%RESULT%"=="0" echo Backup saved: %BACKUP_FILE%
if "%RESULT%"=="0" echo Now copy it somewhere off this PC, for example a USB drive.
echo.
pause
endlocal
