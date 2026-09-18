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
   Any recent version (14+) works — verified in testing against both
   PostgreSQL 16 and 18.
2. Run the installer. When prompted for a password for the `postgres`
   superuser, set one and **remember it exactly** — you'll need it again
   in step 4 below and in the iDART installer later, and there's no way
   to recover it if forgotten (only reset, which needs the recovery
   procedure in Troubleshooting below).
3. **Important — switch to `md5` authentication before going further.**
   The vendored PostgreSQL driver predates SCRAM-SHA-256 (the modern
   default) and cannot authenticate with it. Do the following steps
   **in this exact order** — doing the `pg_hba.conf` change (step 5)
   before re-storing the password (step 4) will lock you out (see
   Troubleshooting if that happens).
4. Open **pgAdmin**, connect to the server with the password from step 2
   (this still works normally at this point — nothing's changed yet),
   open a query tool, and run both of these, in order:
   ```sql
   ALTER SYSTEM SET password_encryption = 'md5';
   ALTER USER postgres WITH PASSWORD 'your-password-here';
   ```
   The first changes how *new* passwords get stored from now on; the
   second re-stores the current password using that new format, while
   you're still safely connected under the old one. Use the same
   password again, or a new one — either way, this is the point where
   it actually becomes `md5`-stored.
5. **Now** change how connections are authenticated. Open
   `C:\Program Files\PostgreSQL\<version>\data\pg_hba.conf` in a text
   editor **running as Administrator** (it's a protected folder — e.g.
   right-click Notepad → Run as administrator, then File → Open).
   Change every line ending in `scram-sha-256` to end in `md5`
   instead — there are normally a few `local`/`host` lines like this:
   ```
   host    all             all             127.0.0.1/32            md5
   host    all             all             ::1/128                 md5
   ```
   Save the file.
6. Restart the PostgreSQL service for the `pg_hba.conf` change to take
   effect: **Services** app (search Windows for "Services") → find
   `postgresql-x64-<version>` → Restart.
7. Reconnect in pgAdmin with the same password to confirm it still
   works — it should, on the first try, since the password was already
   re-stored as `md5` in step 4 before the auth method changed.
8. Create the database iDART will use:
   ```sql
   CREATE DATABASE pharm OWNER postgres;
   ```
   (`pharm` is the installer's default database name — see step 2.3 below.
   You can use a different name, just be consistent between here and the
   installer wizard.)

   **Use a genuinely fresh, empty PostgreSQL server for this** — either
   a brand new local install (as above) or an empty database on an
   existing server. Do not point this at a database that already has
   an older iDART installation's data in it; see the
   `ValidationFailedException` entry in Troubleshooting for why.

## 2. Run the iDART installer

1. Copy `idart-install-<version>.jar` (built via `ant generateInstaller`)
   to the Windows machine.
2. Double-click it. If nothing happens (some Windows setups don't
   associate `.jar` with `javaw` by default), open Command Prompt in that
   folder and run:
   ```
   java -jar idart-install-<version>.jar
   ```
3. The default install location is under `C:\Program Files`, which needs
   administrator rights to write to. The installer requests this itself —
   expect a Windows **User Account Control** prompt ("Do you want to allow
   this app to make changes to your device?") right after step 2. Click
   **Yes**. If you don't get this prompt and instead see an error saying
   the install directory can't be written to, the installer didn't manage
   to relaunch itself elevated (uncommon, but some locked-down/managed PCs
   block it) — right-click the `.jar` (or your Command Prompt shortcut) and
   choose **"Run as administrator"** manually, or use **Browse** on that
   screen to pick a folder you already have write access to, e.g. somewhere
   under your own user profile.
4. Work through the installer wizard:
   - **Install path**: default is fine, or pick your own.
   - **Database Server**: `localhost` if you installed PostgreSQL on this
     same machine per section 1.2 (the normal case). Only use a different
     hostname if you deliberately set up PostgreSQL elsewhere — and even
     then, it must point at a fresh, empty database; see the
     `ValidationFailedException` entry in Troubleshooting if you're
     tempted to point this at an existing/older iDART database instead.
   - **iDART Database Name**: `pharm` (must match what you created in 1.2.8).
   - **iDART Database Username**: `postgres`.
   - **iDART Database Password**: the password you set/confirmed in 1.2.2/1.2.4.
   - **Connecting to Ekapa?**: **No** (Ekapa is a South African health
     system integration, not relevant here).
   - The remaining panels are dispensing/label/workflow preferences
     (direct-dispensing vs. pre-packaging, label size, auto-logout time,
     etc.) — the defaults are reasonable to start with; all of them can be
     changed later by editing `idart.properties` in the install folder.
5. Finish the wizard. This installs the app and creates Start Menu /
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

**"Connection to localhost:5432 refused" (or similar) on the installer's Database Connection Settings screen**
- Most often: PostgreSQL isn't running, or is listening on a different
  port than 5432. Check the `postgresql-x64-<version>` service is
  started, and that nothing else on the machine is using port 5432.
- If you typed anything other than `localhost` as the server address:
  that only works if PostgreSQL is genuinely reachable at that
  address from this machine. If you meant to use the PostgreSQL you
  just installed on *this* machine per section 1.2, the address is
  `localhost`, not a remote hostname — this exact mix-up (accidentally
  pointing at a different, pre-existing server on the network) is what
  caused this error during testing.
- Authentication error instead of connection refused: password
  encryption is still `scram-sha-256`, or the password stored on the
  server doesn't match what you typed. Revisit section 1.2 steps 3–7.

**"password authentication failed for user \"postgres\"" when reconnecting in pgAdmin, after switching pg_hba.conf to md5 (locked out)**
This means the password actually stored on the server doesn't match
what you're typing — most likely because `pg_hba.conf` was switched to
`md5` (section 1.2 step 5) before the password was re-stored in `md5`
format (step 4), or the password was simply mistyped/forgotten along
the way. Recover it with PostgreSQL's standard "trust" trick:
1. Edit `pg_hba.conf` again (as Administrator) — change the `local
   all all`, `host all all 127.0.0.1/32`, and `host all all ::1/128`
   lines from `md5` to **`trust`** (leave the `replication` lines
   alone).
2. Restart the PostgreSQL service.
3. Reconnect in pgAdmin — it should now connect without checking any
   password.
4. Run `ALTER USER postgres WITH PASSWORD 'your-password-here';` to
   set a password you're sure of.
5. Edit `pg_hba.conf` a third time, changing those same lines from
   `trust` back to `md5`.
6. Restart the service once more, then reconnect with the new
   password to confirm.

   `trust` means anyone who can reach the server skips password
   checking entirely — don't leave it set that way; step 5 is not
   optional cleanup. If this feels like too much back-and-forth and
   nothing real is stored on the server yet, uninstalling and
   reinstalling PostgreSQL fresh is an equally valid shortcut.

**"Unable to create the database"**
Check the log file in the install folder for the underlying error. If it's
a permissions error, confirm the database user (`postgres` by default) has
rights to create tables in the target database.

**Error dialog titled "iDART: Error", "Error while updateing the database:
liquibase.exception.ValidationFailedException"**
This means the database you pointed the installer at is **not** a fresh,
empty one — it already has iDART migration history in it (`SELECT * FROM
databasechangelog;` will show existing rows if so), most likely because
it's an existing database from an older iDART deployment rather than a
new one created per section 1.2. Liquibase records a checksum for every
migration it's already run, and several of the migration files in this
build were fixed to work around bugs that only show up against a modern
PostgreSQL — so their content, and therefore their checksum, no longer
matches what an old database recorded originally. This is expected
behavior, not a bug to work around: **use a genuinely fresh, empty
database** (section 1.2 step 8) to get the app running.

If you actually need an existing iDART deployment's historical data
carried into this build, that's a real but separate task from a fresh
install — it needs a backup of that database taken first, and a careful
review of what the migration would actually do against that specific
(likely older and possibly manually modified) schema before running
anything against it. Don't attempt it by just pointing the installer at
it and working through wizard errors.

**"This directory can not be written! Please choose another directory!" during install**
The install path (`C:\Program Files\...` by default) needs administrator
rights. The installer should prompt for this itself via a UAC dialog before
this screen even appears — see step 2.3 above. If you got here without
seeing that prompt, either go back and re-launch as administrator, or click
**Browse** here and pick a folder you already have write access to instead
(e.g. `C:\Users\<you>\iDART`).

**Installer freezes/hangs right after "The target directory will be created", no error shown**
This was a real bug in earlier builds: the Windows shortcut-creation step
depends on a native DLL, and the installer was only bundling the 32-bit
version of it while requiring a 64-bit JVM — a 64-bit Java can't load a
32-bit DLL, and because the installer runs windowless (`javaw.exe`) by
that point, the failure had nowhere to display, so it just silently froze
at 0% CPU instead of erroring. This is fixed in `build.xml` (both
installer-generating targets now repack the 64-bit DLL under the name the
loader expects). If you hit this, you're most likely running an installer
jar built before that fix — rebuild with `ant generateInstaller` from a
current checkout, or get a freshly built jar.

**Installer or app won't start / GUI looks broken**
This build targets a modern 64-bit Java 8 runtime specifically — confirm
you installed Temurin 8 (not a 32-bit build, not a different major
version) and that `JAVA_HOME`/PATH point at it and not some other Java
install that might already be on the machine.
