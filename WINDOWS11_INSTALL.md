# Installing iDART-JSS on Windows 11

This is a step-by-step guide to a fresh install on a Windows 11 machine. It
assumes no existing iDART installation and no existing PostgreSQL server.

## 1. Prerequisites

### 1.1 Install a 64-bit Java 8 runtime

The app is built and tested against **Java 8** specifically (the vendored
libraries — Hibernate, JasperReports, and others — are all pre-2012 and a
newer Java version risks breaking them in ways this project deliberately
avoided; see the notes in `build.xml`). Do not install a newer JRE (11, 17,
21, ...) instead — it has not been tested against this app.

1. Download and install **Eclipse Temurin 8 (JRE or JDK), Windows x64** from
   [adoptium.net](https://adoptium.net/). Pick the installer (`.msi`), and
   during setup enable **"Set JAVA_HOME variable"** and **"JavaSoft
   (Oracle) registry keys"** if offered — this saves the manual step below.
2. If `JAVA_HOME` wasn't set automatically, set it yourself:
   - Search Windows for **"Edit the system environment variables"** → **Environment Variables**.
   - Under **System variables**, click **New**, set:
     - Variable name: `JAVA_HOME`
     - Variable value: the install path, e.g. `C:\Program Files\Eclipse Adoptium\jdk-8.0.XXX-hotspot`
   - Click OK on all dialogs.
3. Verify: open a new Command Prompt and run `echo %JAVA_HOME%` — it should
   print the path. If `launcher.bat` can't find Java, it will now fall back
   to PATH and, failing that, print a clear error rather than failing
   silently.

### 1.2 Install PostgreSQL

1. Download the Windows installer from
   [postgresql.org/download/windows](https://www.postgresql.org/download/windows/).
   Any recent version (14+) works; this was verified against PostgreSQL 16.
2. Run the installer. When prompted for a password for the `postgres`
   superuser, set one and **remember it** — you'll enter it into the iDART
   installer later.
3. **Important — switch to `md5` authentication before creating any
   database users.** The vendored PostgreSQL driver predates
   SCRAM-SHA-256 (PostgreSQL's modern default) and cannot authenticate
   with it. This needs two changes:

   a. **Change how new passwords are stored.** Open **pgAdmin**, open a
      query tool on the `postgres` database, and run:
      ```sql
      ALTER SYSTEM SET password_encryption = 'md5';
      ```
      This only affects passwords set *after* this point, not the auth
      method connections actually use — step (b) is still required.

   b. **Change how connections are authenticated.** Open
      `C:\Program Files\PostgreSQL\<version>\data\pg_hba.conf` in a text
      editor **running as Administrator** (it's a protected folder — e.g.
      right-click Notepad → Run as administrator, then File → Open).
      Change every line ending in `scram-sha-256` to end in `md5`
      instead — there are normally a few `host ... 127.0.0.1/32` /
      `::1/128` lines like this:
      ```
      host    all             all             127.0.0.1/32            md5
      host    all             all             ::1/128                 md5
      ```
      Save the file.

   c. Restart the PostgreSQL service for both changes to take effect:
      **Services** app (search Windows for "Services") → find
      `postgresql-x64-<version>` → Restart.
4. The `postgres` user's password was set during PostgreSQL installation
   (step 2), *before* step 3a's encryption change — so it's still stored
   in the old format. Reset it once more (same password is fine) so it's
   stored as `md5`:
   ```sql
   ALTER USER postgres WITH PASSWORD 'your-password-here';
   ```
5. Create the database iDART will use. Open a query tool (as the
   `postgres` user) and run:
   ```sql
   CREATE DATABASE pharm OWNER postgres;
   ```
   (`pharm` is the installer's default database name — see step 2.3 below.
   You can use a different name, just be consistent between here and the
   installer wizard.)

## 2. Run the iDART installer

1. Copy `idart-install-<version>.jar` (built via `ant generateInstaller`)
   to the Windows machine.
2. Double-click it. If nothing happens (some Windows setups don't
   associate `.jar` with `javaw` by default), open Command Prompt in that
   folder and run:
   ```
   java -jar idart-install-<version>.jar
   ```
3. Work through the installer wizard:
   - **Install path**: default is fine, or pick your own.
   - **Database Server**: `localhost` (unless PostgreSQL is on another machine).
   - **iDART Database Name**: `pharm` (must match what you created in 1.2.5).
   - **iDART Database Username**: `postgres`.
   - **iDART Database Password**: the password you set in 1.2.2.
   - **Connecting to Ekapa?**: **No** (Ekapa is a South African health
     system integration, not relevant here).
   - The remaining panels are dispensing/label/workflow preferences
     (direct-dispensing vs. pre-packaging, label size, auto-logout time,
     etc.) — the defaults are reasonable to start with; all of them can be
     changed later by editing `idart.properties` in the install folder.
4. Finish the wizard. This installs the app and creates Start Menu /
   desktop shortcuts.

## 3. First launch

1. Use the desktop shortcut ("iDART - Pharmacy"), or run `go.bat` from the
   install folder.
2. On first launch, iDART detects the database is empty and opens a
   **Create Database** wizard. Choose **"Don't include test data"** (unless
   you specifically want sample data) and click **Finish**. This creates
   the full schema — it takes a little while and there's no progress bar
   for parts of it, so let it run.
3. You should land on the **login screen**. Default seed credentials are:
   - Username: `admin`
   - Password: `123`

   **Change this password immediately** — this is a real pharmacy system
   and the default credential is publicly documented (in this file).
   Go to **General Admin** after logging in to manage users.
4. After logging in you should see the main dashboard (General Admin,
   Patient Admin, Stock & Dispensing, Reports).

## 4. Troubleshooting

**"JAVA_HOME is not set and javaw.exe was not found on PATH"**
`launcher.bat` couldn't find a Java install. Recheck section 1.1 — either
set `JAVA_HOME` correctly or make sure `javaw.exe` (inside the JRE's `bin`
folder) is on your PATH.

**A database/authentication error on first launch, mentioning "authentication" or connection refused**
- Connection refused: PostgreSQL isn't running, or is listening on a
  different port than 5432. Check the `postgresql-x64-<version>` service
  is started, and that nothing else on the machine is using port 5432.
- Authentication error: password encryption is still set to
  `scram-sha-256`. Revisit step 1.2.3–1.2.4 — the `ALTER SYSTEM` change
  only applies to *newly set* passwords, so if you set the postgres
  password before making that change, reset it again afterward.

**"Unable to create the database"**
Check the log file in the install folder for the underlying error. If it's
a permissions error, confirm the database user (`postgres` by default) has
rights to create tables in the target database.

**Installer or app won't start / GUI looks broken**
This build targets a modern 64-bit Java 8 runtime specifically — confirm
you installed Temurin 8 (not a 32-bit build, not a different major
version) and that `JAVA_HOME`/PATH point at it and not some other Java
install that might already be on the machine.
