# Installing iDART-JSS on Windows 11

This is a step-by-step guide to a fresh install on a Windows 11 machine. It
assumes no existing iDART installation and no existing PostgreSQL server.

**Known-good file set**, if you just want exact versions to grab without
reading the reasoning below: Temurin **JDK 8** (any `8u5xx` build, Windows,
x64, `.msi`) and **PostgreSQL 18** (Windows x64 installer). As of this
writing, `OpenJDK8U-jdk_x64_windows_hotspot_8u504b01.msi` and
`postgresql-18.6-3-windows-x64.exe` are the specific files in current use
for new installs — if you already have those two files, skip straight to
section 2.

## 1. Prerequisites

### 1.1 Install a 64-bit Java 8 runtime

The app is built and tested against **Java 8** specifically (the vendored
libraries — Hibernate, JasperReports, and others — are all pre-2012 and a
newer Java version risks breaking them in ways this project deliberately
avoided; see the notes in `build.xml`). Do not install a newer JRE (11, 17,
21, ...) instead — it has not been tested against this app.

1. Go to [adoptium.net](https://adoptium.net/) to download **Temurin,
   version 8, Windows, x64, JDK (or JRE), `.msi`**.
   **Do not just click the front-page download button** — it defaults to
   whatever the latest release currently is (21, 25, ...), not 8. Use the
   version selector/dropdown on the page and explicitly change it to
   **8** before downloading. The correct file's name contains `8u` — e.g.
   `OpenJDK8U-jdk_x64_windows_hotspot_8u504b01.msi`. If the filename says
   `jdk-11`, `jdk-17`, `jdk-21`, `jdk-25`, or anything other than `8u...`,
   it's the wrong one. This exact mistake has already caused the app to
   silently quit on launch with no error dialog and no log file (see
   Troubleshooting) — it's an easy trap on that site, so it's worth
   double-checking the filename before installing.
2. Run the `.msi`, and during setup enable **"Set JAVA_HOME variable"**
   and **"JavaSoft (Oracle) registry keys"** if offered — this saves the
   manual step below.
3. If `JAVA_HOME` wasn't set automatically, set it yourself:
   - Search Windows for **"Edit the system environment variables"** → **Environment Variables**.
   - Under **System variables**, click **New**, set:
     - Variable name: `JAVA_HOME`
     - Variable value: the install path, e.g. `C:\Program Files\Eclipse Adoptium\jdk8u504-b01` (the folder name will contain `8u`, not a bare major version number)
   - Click OK on all dialogs.
4. Verify — this matters even if step 2 auto-set things, because it's the
   only step that catches "found *a* Java, just the wrong version" rather
   than "found no Java at all": open a **new** Command Prompt window (has
   to be new to see an updated variable) and run:
   - `echo %JAVA_HOME%` — should print a path containing `8u`, not
     `jdk-11`/`17`/`21`/`25`.
   - `java -version` — the first line should say `1.8.0_...`.

   If `launcher.bat` can't find Java at all, it falls back to PATH and,
   failing that, prints a clear error rather than failing silently — but
   a *wrong version* of Java being found is a different, silent failure
   that this verification step is what actually catches.

### 1.2 Install PostgreSQL

1. Download the Windows installer from
   [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
   — select the **Windows x86-64** installer. Any recent version (14+)
   works — verified in testing against both PostgreSQL 16 and 18 — and
   the team currently standardizes on **PostgreSQL 18**
   (`postgresql-18.6-3-windows-x64.exe` as of this writing) for new
   installs.
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

## 4. Migrating data from an older iDART installation

This covers moving real data from an existing iDART deployment (e.g. an
old PostgreSQL 9.x install) into a fresh install of this build — a
different task from the fresh install in sections 1–3, and one that
touches real patient data, so treat every step here as production work.

### 4.1 Get the old server's own pg_dump

Use the *old* server's own `pg_dump.exe`, not the new server's, even if
both are installed on the same machine — a newer client dumping an older
server works fine, but the executable's version still has to actually
exist at the path you point at.

```
set PGPASSWORD=<old-db-password>
"C:\Program Files\PostgreSQL\<old-version>\bin\pg_dump" --no-owner --no-privileges -h localhost -p <old-port> -U postgres <old-db-name> > old_full.sql
```
Find `<old-version>` via `dir "C:\Program Files\PostgreSQL"`, and confirm
`<old-port>` in that version's `data\postgresql.conf` if it isn't the
default 5432.

A few warnings are expected and harmless when this gets restored:
- `SET default_with_oids = ...` — removed from PostgreSQL in version 12;
  every line like this throws "unrecognized configuration parameter" on
  restore, but doesn't stop anything or lose data (this app never relied
  on table OIDs).
- Restoring a custom-format dump instead shows the same non-issue as
  `restoring tables WITH OIDS is not supported anymore`.
- `schema "public" already exists` (once, right at the start of a
  restore into a freshly created database) — every new Postgres database
  already has a `public` schema by default.

### 4.2 Restore into a fresh, separate database on the new server

Create a new, distinct database — don't reuse one from an earlier
partial attempt — and restore into it:
```
"C:\Program Files\PostgreSQL\<new-version>\bin\createdb" -h localhost -p <new-port> -U postgres pharm_migrated
"C:\Program Files\PostgreSQL\<new-version>\bin\psql" -h localhost -p <new-port> -U postgres -d pharm_migrated -f old_full.sql 2> restore_errors.log
```
pgAdmin4 works just as well (register a connection to the new server,
Create → Database, then Restore… on it) and avoids hunting down exact
paths/ports for this half — only the old-server dump in 4.1 needs the
old version's own binary specifically.

Check `restore_errors.log` (or pgAdmin4's process log) afterward — you
should see only the warnings from 4.1. Anything else is worth
investigating before continuing.

### 4.3 Point the app at the migrated database

There's no in-app "change database" menu — the connection settings
screen only appears automatically, when the app can't reach its
currently configured database at all. To reach it deliberately:
1. Stop the *new* PostgreSQL service (Services app →
   `postgresql-x64-<new-version>` → Stop).
2. Launch iDART — it should fail to connect and open the **Database
   Connection Settings** wizard page instead of the login screen.
3. Start the PostgreSQL service again (the wizard tests the connection
   live as you type, so it needs the server reachable — don't close the
   wizard while doing this).
4. Set **Database name** to your migrated database (e.g.
   `pharm_migrated`), confirm host/username, and enter the password.
5. Finish the wizard.

This same wizard also offers to *create* a fresh database if it finds
one empty — it checks for existing rows in the `users` table first, so
a populated migrated database is correctly detected as already set up
and this step is skipped automatically. It will not overwrite anything.

### 4.4 Liquibase checksum validation failure on a real migration

If you hit the `ValidationFailedException` error described in
Troubleshooting below, but you're certain the database is a legitimate
migration (not an accidental point-at-an-old-deployment mistake covered
by that entry), check `idart.log` in the install folder for the specific
changeset IDs — the error looks like:
```
Validation Failed:
     2 change sets check sum
          org/celllife/idart/database/changelog-3.8.xml::3.8.2::simon@cell-life.org is now: 3:...
```
This happens when a changeset's checksum was computed against an older,
byte-different-but-functionally-identical copy of a file it references
(changesets defined via `sqlFile`/`loadData` checksum the *referenced*
file's exact bytes, not just its effect) — in practice this has traced
back to formatting differences from this codebase's original SVN-to-git
import, not a real content change. Verify before dismissing it: compare
what the referenced file would actually do against what's already in the
target database (e.g. for a function-altering changeset, compare its SQL
against the live function definition).

Once confirmed benign, accept the current files as correct for just
those specific rows. This does not re-run them or touch any data — only
Liquibase's own bookkeeping table:
```sql
UPDATE databasechangelog
SET md5sum = NULL
WHERE filename = '<file from the error>'
  AND id IN ('<id1>', '<id2>');
```
Relaunch the app afterward.

### 4.5 Verify

Row-count a handful of clinically important tables on both the old and
new databases and compare:
```sql
SELECT 'patient' AS table_name, COUNT(*) FROM patient
UNION ALL SELECT 'prescription', COUNT(*) FROM prescription
UNION ALL SELECT 'package', COUNT(*) FROM package
UNION ALL SELECT 'stock', COUNT(*) FROM stock
UNION ALL SELECT 'clinic', COUNT(*) FROM clinic
UNION ALL SELECT 'users', COUNT(*) FROM users;
```
A small gap (new side lower) on high-churn tables like `prescription`/
`package` most often just means the old server kept taking real activity
after the dump was taken — re-run the old-side count to confirm it's
still climbing, rather than assuming data was lost in the restore.

**For the real cutover** (not a test/rehearsal run): repeat this whole
process with a dump taken at the actual switch-over moment, with
dispensing paused on the old system for the few minutes between taking
the dump and bringing the new install online — anything entered in that
gap won't be in the dump.

### 4.6 A note on this specific deployment's existing data

Worth knowing before assuming something's broken post-migration: this
deployment's data has exactly **one** clinic record, and both it and the
`nationalclinics` reference table are leftover South African seed data
from the original Cell-Life product (South African district/metro
municipality names; a South African trade union as the clinic name) —
not something the migration dropped, and not specific to any one
migration attempt. Adding real clinics is a normal **General Admin → Add
Clinic** task (only the clinic name is actually required — the
province/district/facility fields can be left blank). Making an added
clinic *selectable at login* additionally needs `downReferralMode` in
`idart.properties` set to `online` — which also enables a real
down-referral/distribution workflow (a main pharmacy scanning packages
out to satellite clinics) across the Stock Control and
Package-to-Patient screens, so confirm that operating model actually
fits before switching it just to unlock the login dropdown.

## 5. Troubleshooting

**"JAVA_HOME is not set and javaw.exe was not found on PATH"**
`launcher.bat` couldn't find a Java install. Recheck section 1.1 — either
set `JAVA_HOME` correctly or make sure `javaw.exe` (inside the JRE's `bin`
folder) is on your PATH.

**App window flashes briefly (or nothing visible happens at all) and then quits — no error dialog, no `idart.log` created**
This is a *different* failure from the one above: Java was found and the
app started launching, but died very early — before logging even
initialized. Every occurrence of this so far has traced back to the
**wrong major version of Java** being installed, most often JDK 21 or 25
instead of 8, from grabbing whatever adoptium.net's front-page button
defaults to instead of explicitly selecting version 8 (section 1.1). The
app always launches via windowless `javaw.exe`, so a Java-version
incompatibility has nowhere to display itself — it just silently dies.

To confirm and fix:
1. `dir "C:\Program Files\Eclipse Adoptium"` — if the folder name is
   `jdk-11...`, `jdk-17...`, `jdk-21...`, `jdk-25...`, etc. instead of
   `jdk8u...`, that's the cause. Install the correct version per
   section 1.1 (you don't need to uninstall the wrong one — just install
   8 alongside it and point `JAVA_HOME` at that folder instead).
2. Confirm `JAVA_HOME` was actually updated to the correct folder in a
   **new** Command Prompt window — `echo %JAVA_HOME%`.
3. If both check out and it's still happening, get a definitive error
   message instead of guessing further: copy `launcher.bat` to
   `launcher-debug.bat` in the install folder, change `javaw.exe` to
   `java.exe` in the `start` line near the bottom, then run
   `.\launcher-debug.bat org.celllife.idart.start.PharmacyApplication`
   from a Command Prompt window opened in that folder (not by
   double-clicking the shortcut). `java.exe` keeps a console attached, so
   whatever's actually failing prints there instead of vanishing.

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

**Database Connection Settings wizard "hangs"/does nothing on Finish, even after retyping a different database name/password**
On a jar built from a current checkout, this shouldn't happen anymore: two
contributing issues have been fixed. First, `JDBCUtil` used to cache a
database connection per-thread and never discard it when the connection
settings changed, so a stale connection could in principle survive a
retry; `JDBCUtil.rebuild()` now closes any cached connection whenever
settings are rebuilt. Second, and more likely the actual cause in
practice: clicking Finish writes `idart.properties` into the install
folder, which — before the install-folder permissions fix described
above — a non-elevated app process couldn't do, so Finish could silently
fail to do anything useful. If you still hit this on a current build,
check `idart.log` for the real error rather than assuming it's this same
issue recurring.

**Error dialog titled "iDART: Error", "Error while updateing the database:
liquibase.exception.ValidationFailedException"**
For a fresh install (this guide, sections 1–3): this means the database
you pointed the installer at is **not** a fresh, empty one — it already
has iDART migration history in it (`SELECT * FROM databasechangelog;`
will show existing rows if so), most likely because it's an existing
database from an older iDART deployment rather than a new one created
per section 1.2. **Use a genuinely fresh, empty database** (section 1.2
step 8) to get the app running.

If you're deliberately migrating an older deployment's real data, see
section 4 for the full procedure — this same error can show up
legitimately even on a correct migration; section 4.4 covers the actual
cause (a changeset checksum mismatch tied to how a couple of migration
files were imported into this repository years ago, not a data problem)
and the fix. Don't attempt a real migration by just pointing the
installer at an old database and working through wizard errors as they
come — section 4 exists because that approach doesn't give you a way to
tell a benign checksum mismatch apart from a real one.

**"This directory can not be written! Please choose another directory!" during install**
The install path (`C:\Program Files\...` by default) needs administrator
rights. The installer should prompt for this itself via a UAC dialog before
this screen even appears — see step 2.3 above. If you got here without
seeing that prompt, either go back and re-launch as administrator, or click
**Browse** here and pick a folder you already have write access to instead
(e.g. `C:\Users\<you>\iDART`).

**No `idart.log` is ever created / errors seem to vanish with no trace, especially when running as a normal (non-administrator) user**
The installer now grants regular user accounts write access to the install
folder automatically (an `icacls` step added to `metadata/install/process.xml`,
running while the installer itself is still elevated). If you're using a
jar built from a current checkout, this is already handled — the app can
write `idart.log`, its properties file, etc. under `C:\Program Files\iDART`
without needing "Run as administrator" every time. If you hit this on an
installer jar built before that fix, either rebuild with
`ant generateInstaller` from a current checkout, or work around it on the
affected machine: right-click the install folder (default
`C:\Program Files\iDART`) → **Properties** → **Security** tab → **Edit** →
select your user account → check **Modify** → **OK**.

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
