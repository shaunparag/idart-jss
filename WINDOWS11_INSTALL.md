# Installing iDART-JSS on Windows 11

This is a step-by-step guide to a fresh install on a Windows 11 machine. It
assumes no existing iDART installation and no existing PostgreSQL server.
It is for iDART 3.9.0, the version shown on the login screen;
[CHANGELOG.md](CHANGELOG.md) lists what changed from 3.8.1.

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
   than "found no Java at all": open a **new** terminal window (has
   to be new to see an updated variable) and run:
   - `echo %JAVA_HOME%` in Command Prompt, or `$env:JAVA_HOME` in
     PowerShell (Windows 11's default — don't mix them up: `%JAVA_HOME%`
     typed into PowerShell just echoes that literal text back and looks
     like the variable isn't set even when it is) — should print a path
     containing `8u`, not `jdk-11`/`17`/`21`/`25`.
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
   in step 3 below and in the iDART installer later, and there's no way
   to recover it if forgotten (only reset, which needs the recovery
   procedure in Troubleshooting below). Leave PostgreSQL's password
   settings at their defaults: the database driver bundled with iDART
   supports the default `scram-sha-256` method.

   Earlier versions of this guide had extra steps here to switch
   PostgreSQL to `md5` passwords. They're no longer needed, and machines
   already set up that way keep working as they are.
3. Create the database iDART will use: open **pgAdmin**, connect to the
   server with the password from step 2, open a Query Tool, and run:
   ```sql
   CREATE DATABASE pharm OWNER postgres;
   ```
   (`pharm` is the installer's default database name — see section 2,
   step 4. You can use a different name, just be consistent between here
   and the installer wizard.)

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
3. The install-path screen defaults to a folder under your own profile
   (`C:\Users\<you>\iDART`) — no admin rights needed, and **the installer
   never requests elevation** (it always runs as a normal, unelevated
   process, by design — see Troubleshooting if you're wondering why there's
   no UAC prompt). Just accept the default and continue, or Browse to pick
   somewhere else you own.

   If you specifically want it under `C:\Program Files` instead, right-click
   Command Prompt/PowerShell, choose **"Run as administrator"**, `cd` to the
   folder with the jar, and run `java -jar idart-install-<version>.jar` from
   that elevated prompt — then type or Browse to that path instead of the
   default; it'll be writable this time. Doing this from a normal,
   non-elevated launch instead will fail with a "directory can not be
   written" error — see Troubleshooting if you hit that.
4. Work through the installer wizard:
   - **Install path**: default is fine, or pick your own.
   - **Database Server**: `localhost` if you installed PostgreSQL on this
     same machine per section 1.2 (the normal case). Only use a different
     hostname if you deliberately set up PostgreSQL elsewhere — and even
     then, it must point at a fresh, empty database; see the
     `ValidationFailedException` entry in Troubleshooting if you're
     tempted to point this at an existing/older iDART database instead.
   - **iDART Database Name**: `pharm` (must match what you created in
     section 1.2, step 3).
   - **iDART Database Username**: `postgres`.
   - **iDART Database Password**: the `postgres` password you set in
     section 1.2, step 2.
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
5. Enter your pharmacy's details: **General Admin → Manage Pharmacies**,
   choose **Update Facility Details**, and fill in the facility name,
   street address, city, telephone number, head pharmacist and pharmacy
   assistant. These print on every label and on reports. A new database
   starts with placeholders ("Facility Name", "Demo Pharmacist, B.Pharm"),
   so labels look wrong until this is done.
6. Before entering real patients, set up daily backups — see section 5.

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

iDART opens the database named in its settings: the name typed into the
installer, unless it was later changed in the connection settings
wizard. To check which one that is, look near the top of `idart.log` in
the install folder for a line like `Opening JDBC connection to
jdbc:postgresql://localhost:5432/pharm`.

**Simplest: rename the databases.** Give the migrated database the name
iDART already uses, and keep the old one under a new name. Close iDART
and pgAdmin first (PostgreSQL won't rename a database that has open
connections), then in Command Prompt, where `pharm` is the name iDART
uses:
```
"C:\Program Files\PostgreSQL\<new-version>\bin\psql" -h localhost -p <new-port> -U postgres -d postgres -c "ALTER DATABASE pharm RENAME TO pharm_old;" -c "ALTER DATABASE pharm_migrated RENAME TO pharm;"
```
Then start iDART. To go back, run the same two renames the other way
round.

**Or change iDART's settings.** There's no in-app "change database"
menu — the connection settings screen only appears automatically, when
the app can't reach its currently configured database at all. To reach
it deliberately:
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

Either way, the **Backup iDART database** shortcut backs up the
migrated database too: each time iDART starts, it sets the database
settings in `backup.bat` to the database it opened.

This same wizard also offers to *create* a fresh database if it finds
one empty — it checks for existing rows in the `users` table first, so
a populated migrated database is correctly detected as already set up
and this step is skipped automatically. It will not overwrite anything.

### 4.4 Liquibase checksum validation failure on a real migration

Databases from the 2012 iDART builds hold different checksums for
changesets 3.8.2 and 3.8.4 of `changelog-3.8.xml` than this build
computes. The files are the same; the 2012 builds shipped them with
Windows line endings, and changesets defined via `sqlFile`/`loadData`
checksum the *referenced* file's exact bytes. Current builds accept those
two automatically, so a migrated database opens without a manual step.

If you still hit the `ValidationFailedException` error described in
Troubleshooting below, but you're certain the database is a legitimate
migration (not an accidental point-at-an-old-deployment mistake covered
by that entry), check `idart.log` in the install folder for the specific
changeset IDs — the error looks like:
```
Validation Failed:
     2 change sets check sum
          org/celllife/idart/database/changelog-3.8.xml::3.8.2::simon@cell-life.org is now: 3:...
```
- If it lists only 3.8.2 and 3.8.4, that PC has an iDART older than
  3.9.0 (the version is on the login screen). Install the current one,
  or use the fix below.
- Anything else is a real difference between the code and the database.
  Verify it before dismissing it: compare what the referenced file would
  actually do against what's already in the target database (e.g. for a
  function-altering changeset, compare its SQL against the live function
  definition).

Once confirmed benign, accept the current files as correct for just
those specific rows. This does not re-run them or touch any data — only
Liquibase's own bookkeeping table, which iDART refills with the current
checksums on its next start:
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

**Repeated package IDs are normal in older data.** Older versions of
iDART could give two packages the same ID: the package number went back
to 1 after package 10, and a package made to replace a returned one
reused its number. iDART 3.9.0 handles these in its reports and when
scanning packages, and numbers new packages after the highest number
already used on each prescription, so there is nothing to fix. To see
how many IDs repeat:
```sql
SELECT COUNT(*) AS repeated_package_ids
FROM (SELECT packageid FROM package GROUP BY packageid HAVING COUNT(*) > 1) r;
```

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

## 5. Backing up and restoring

All patient, prescription, dispensing and stock records live in the
PostgreSQL database, not in the iDART install folder — reinstalling iDART
or copying its folder does not back them up. Back up at least once a day,
and keep copies somewhere other than this PC.

### 5.1 Making a backup

1. Start menu → **All apps** → **iDart** → **Backup iDART database** (or
   double-click `backup.bat` in the install folder).
2. When asked, enter the PostgreSQL password — the one you entered on the
   iDART installer's database screen.
3. The window shows where the backup was saved:
   `C:\Users\<you>\iDART-backups\iDART-<database>-<date>_<time>.backup`.
   That folder is outside the install folder, so uninstalling iDART
   doesn't delete your backups.
4. Copy the file off this PC (USB drive or network folder) — a backup on
   the same disk doesn't survive a disk failure or a stolen PC.

The backup is always of the database iDART uses: each time iDART starts,
it updates the database settings in `backup.bat` to match its own, so
don't edit those lines by hand. When it changes them, `idart.log` records
a line such as `Pointed backup.bat at database pharm on localhost.`

If the window says it couldn't find `pg_dump.exe`, PostgreSQL is installed
somewhere other than `C:\Program Files\PostgreSQL`: open `backup.bat` in
Notepad and set `postgresDir` to the folder containing `pg_dump.exe`.

pgAdmin can make the same kind of backup: right-click the database →
**Backup…**, with Format set to **Custom**.

### 5.2 Restoring a backup

Restore into a new, empty database — never over the one in use — then
point iDART at it. In Command Prompt:
```
"C:\Program Files\PostgreSQL\<version>\bin\createdb" -h localhost -U postgres pharm_restored
"C:\Program Files\PostgreSQL\<version>\bin\pg_restore" -h localhost -U postgres -d pharm_restored "C:\Users\<you>\iDART-backups\<backup file>"
```
Then follow section 4.3 to point iDART at `pharm_restored`, and section
4.5 to check the row counts.

Try one restore soon after setting up backups, so you know the backup
files are good before you need them.

## 6. Troubleshooting

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
1. `dir "C:\Program Files\Eclipse Adoptium"` — if there's a folder other
   than `jdk-8...` in there (e.g. `jdk-21...`, `jdk-25...`), that's the
   cause.
2. Confirm what `JAVA_HOME` actually resolves to in a **new** terminal
   window (has to be new to see a just-changed value) — in PowerShell
   (the default on Windows 11) that's `$env:JAVA_HOME`, **not**
   `echo %JAVA_HOME%` (that's `cmd.exe` syntax; run in PowerShell it just
   echoes the literal text back and looks like JAVA_HOME is unset even
   when it isn't). `where.exe java` is also worth checking — it lists
   every `java.exe` on PATH in resolution order, which matters because
   some launch paths (a desktop shortcut, double-clicking a `.jar`) can
   go through Windows' file association instead of `JAVA_HOME`.
3. Point `JAVA_HOME` at the `jdk-8...` folder (section 1.1). If more than
   one JDK is installed, this is sometimes not enough by itself — confirmed
   on real hardware, a machine with both 25 and 8 installed kept launching
   under 25 even after correcting `JAVA_HOME`, most likely via a `.jar`
   file association or shortcut that a newer JDK's installer had claimed
   independently of `JAVA_HOME`. If retargeting `JAVA_HOME` doesn't fix it,
   the reliable fallback is to uninstall every installed JDK and install
   only Temurin 8 — no ambiguity left for anything to resolve to the wrong
   one.
4. If it's still happening after that, get a definitive error message
   instead of guessing further: copy `launcher.bat` to
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
- Authentication error instead of connection refused: the password you
  typed doesn't match the one stored on the server for that user. If it's
  been forgotten, see the next entry.

**Forgot the `postgres` password, or "password authentication failed for user \"postgres\""**
This means the password stored on the server doesn't match what you're
typing. Reset it with PostgreSQL's standard "trust" trick:
1. Open `C:\Program Files\PostgreSQL\<version>\data\pg_hba.conf` in a
   text editor **running as Administrator** (it's a protected folder —
   e.g. right-click Notepad → Run as administrator, then File → Open).
   On the `local all all`, `host all all 127.0.0.1/32`, and
   `host all all ::1/128` lines, note the method at the end of the line
   (normally `scram-sha-256`; `md5` on machines set up with an older
   version of this guide) and change it to **`trust`**. Leave the
   `replication` lines alone.
2. Restart the PostgreSQL service (**Services** app →
   `postgresql-x64-<version>` → Restart).
3. Reconnect in pgAdmin — it should now connect without checking any
   password.
4. Run `ALTER USER postgres WITH PASSWORD 'your-password-here';` to
   set a password you're sure of.
5. Edit `pg_hba.conf` again, changing those same lines from `trust` back
   to the method you noted in step 1.
6. Restart the service once more, then reconnect with the new
   password to confirm.

   `trust` means anyone who can reach the server skips password
   checking entirely — don't leave it set that way; step 5 is not
   optional cleanup. If this feels like too much back-and-forth and
   nothing real is stored on the server yet, uninstalling and
   reinstalling PostgreSQL fresh is an equally valid shortcut.

iDART still has the old password saved, so on its next launch it can't
connect and opens the **Database Connection Settings** screen: enter the
new password there and click Finish.

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
step 3) to get the app running.

If you're deliberately migrating an older deployment's real data, see
section 4 for the full procedure — this same error can show up
legitimately even on a correct migration; section 4.4 covers the actual
cause (a checksum mismatch on two changesets whose files the 2012 builds
shipped with Windows line endings, not a data problem), which current
builds accept automatically, and the fix for older builds. Don't attempt a real migration by just pointing the
installer at an old database and working through wizard errors as they
come — section 4 exists because that approach doesn't give you a way to
tell a benign checksum mismatch apart from a real one.

**Windows Script Host error: "There is no script engine for file extension '.js'"**, and/or **"The installer could not launch itself with administrator permissions"**
Both of these were the installer attempting to self-elevate via UAC — its
elevation mechanism runs a bundled JScript file (`elevate.js`) through
Windows Script Host, and on a machine where WSH's JScript engine is
disabled or unregistered (a common lockdown on managed/enterprise PCs),
that throws the native WSH ".js" error first, which then makes the
installer treat its own relaunch as failed and show the second dialog
right behind it — same underlying cause, confirmed by reading the
installer's actual compiled elevation logic directly. This reproduced
even launching with `java -jar` directly, since the self-elevation
attempt happens from inside the already-running installer process, not
from however it was originally started.

**Fixed as of this build: the installer no longer requests self-elevation
at all**, so neither dialog should appear anymore — if you see either
one, you're running an old installer jar; get a current one built with
`ant generateInstaller`. Also as of this build, the install-path screen's
default is a folder under your own profile rather than `C:\Program
Files`, so the next entry's error shouldn't come up in normal use either
— it now only applies if you deliberately navigate to an admin-owned
location.

**"This directory can not be written! Please choose another directory!" during install**
The installer never self-elevates (see above), so it can only write to a
folder you already own. You'll only see this if you've deliberately typed
or Browsed to an admin-owned location like `C:\Program Files\...` instead
of accepting the default. Two ways forward: click **Browse** on this
screen and pick a folder you already own instead, e.g.
`C:\Users\<you>\iDART` (simplest — or just accept the default, which is
already this) — or restart the installer from an elevated Command
Prompt/PowerShell (right-click → **"Run as administrator"**, `cd` to the
folder with the jar, `java -jar idart-install-<version>.jar`) if you
specifically want it under `C:\Program Files`.

**No `idart.log` is ever created / errors seem to vanish with no trace, especially when running as a normal (non-administrator) user**
Only relevant if you deliberately installed under an admin-owned location
like `C:\Program Files` (no longer the default — see above). The
installer grants regular user accounts write access to the install
folder automatically (an `icacls` step in `metadata/install/process.xml`),
but only if the installer itself was running elevated at install time. If
you installed under your own user profile instead (the default, or via
Browse), this doesn't apply — you already own that folder. If you hit this on an
installer jar built before the `icacls` fix, either rebuild with
`ant generateInstaller` from a current checkout, or work around it on
the affected machine: right-click the install folder → **Properties** →
**Security** tab → **Edit** → select your user account → check
**Modify** → **OK**.

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

**Patient import: "Import is missing compulsory columns" or "Unable to open the Excel sheet"**
- "Missing compulsory columns": the spreadsheet wasn't made from this
  database's template — most often it's the old `import.xls` that earlier
  versions put in the install folder. Use **General Admin → Generate
  import template** instead, and follow `Import_README.txt` in the
  install folder.
- "Unable to open the Excel sheet": the file isn't in the older `.xls`
  format (for example it's an `.xlsx`), or the sheet name you typed
  doesn't match. If `idart.log` has an "Error opening Excel file" entry,
  it's the format: save it as "Excel 97-2003 Workbook (*.xls)". If not,
  check the sheet name — a generated template's sheet is called `Sheet1`.

**iDART shows old or unexpected data after a migration or restore**
It is most likely opening a different database from the one you
restored into. Section 4.3 shows how to check which database iDART opens
and how to switch it.

**"An error has occurred in iDART that requires it to restart" on the Package Tracking report, or "Cannot Save Scanned Out Packages"**
Both were caused by two packages sharing a package ID, which older
versions of iDART created (see section 4.5), and both are fixed in
3.9.0. On an older build, the Package Tracking report crashes for such a
patient, and Scan Out Packages to Patients can't save the collection,
shows the wrong next collection date, or crashes when the collection
date is changed. Install 3.9.0.

**Drop-down boxes show an empty grey square instead of an arrow**
Seen on Windows 11 with builds older than 3.9.0. The box still opens
when you click the square; 3.9.0 draws the arrow.

**Installer or app won't start / GUI looks broken**
This build targets a modern 64-bit Java 8 runtime specifically — confirm
you installed Temurin 8 (not a 32-bit build, not a different major
version) and that `JAVA_HOME`/PATH point at it and not some other Java
install that might already be on the machine.
