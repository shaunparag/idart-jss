# Changelog

## 3.9.0 (September 2026)

This release moves iDART onto Windows 11 and fixes the problems found while
testing it with real pharmacy data. Data from older versions of iDART opens
directly: the only database change, a column that lets drugs be made
inactive, is added by iDART itself the first time it opens the database. See
"Upgrading" below and [WINDOWS11_INSTALL.md](WINDOWS11_INSTALL.md).

### Windows 11

- Runs on 64-bit Java 8 with a 64-bit SWT (the library that draws the
  screens), and on current PostgreSQL (tested with 16 and 18) using its
  default password method.
- Screens are the right size on high-resolution displays, without changing
  compatibility settings on each PC.
- Drop-down boxes show their arrow. On Windows 11 they showed an empty grey
  square.
- The installer:
  - installs into your own user folder by default and doesn't ask for
    administrator rights;
  - no longer hangs after the folder is chosen;
  - gives all users write access to the install folder when it is
    installed under Program Files;
  - no longer copies the source code by default.
- The Documentation shortcut opens the iDART User Guide, whose chapters are
  now included. It used to start the uninstaller.

### Upgrading from an older iDART

- Databases created by the 2012 iDART builds open without the
  `ValidationFailedException` checksum error.
- Package IDs that repeat in older data no longer break reports or package
  scanning (see Dispensing).
- The install guide covers moving data from an older installation,
  checking it, and backups.

### Backups

- New Start Menu shortcut, **Backup iDART database**, saves a date-stamped
  backup in the iDART-backups folder of your user profile.
- The backup always covers the database iDART uses, including after you
  switch to another database.

### Dispensing

- Package numbers no longer repeat. The old numbering went back to 1 after
  package 10, and a package that replaced a returned one reused its number.
  New packages now continue from the highest number used on the
  prescription.
- Scan Out Packages to Patients handles a package ID shared by more than one
  package: it records the collection on the one waiting to be collected.
  Before, saving failed, the next collection date was wrong, and changing
  the collection date crashed iDART.
- Opening the history of a patient with old packages whose stock batch has
  since been deleted no longer crashes.

### Drugs

- A drug that is no longer used can be made inactive, instead of deleted,
  in **Update an existing Drug's details** (Status: Active / Inactive).
  An inactive drug:
  - can't be added to prescriptions or drug groups, or received as stock;
  - is left out when a drug group is added to a prescription, and iDART
    says so;
  - is pointed out when a patient's prescription that includes it is
    renewed, so the pharmacist decides whether to keep it;
  - keeps its stock, prescriptions, packages and reports, and its stock
    can still be destroyed or counted in a stock take.

  The drug search shows "Inactive" instead of the pack size for such
  drugs, and typing "inactive" lists them. Existing drugs start active.

### Patients

- Update Patient no longer blanks the patient's details when the
  Down-Refer button is switched off.
- Cellphone numbers can be typed as 0821234567, 082 123 4567 or
  +27821234567 (all stored as 27821234567), and 06x numbers are accepted.
- Adding a patient confirms that the patient "has been saved", and the
  Province list shows each province once.

### Reports

- Package Tracking no longer crashes when a patient's packages share an ID,
  and a report error on that screen now shows a message instead of
  restarting iDART.
- Print Collection Sheets works. It is on the Scan Out Packages to Down
  Referral Clinic screen, which sites installed with the Online down
  referral mode have. Since iDART 3.7 it had looked for the patient's ID
  number where older versions kept it, so no sheet was made and the
  "Please wait" box never closed. Blank details now print blank instead of
  "null", and iDART names any patient it couldn't make a sheet for.
- Monthly Receipts and Issues no longer fails with "column d.nsncode does
  not exist".
- The Patient Visits report (Patient Visits and Stats Module) leaves out
  visits that belong to no patient. The database records one each time
  stock is destroyed, and older versions of iDART left some with a date,
  which made the report fail and restart iDART.
- The Cohorts report runs (its query had a syntax error). Its Excel Report
  button, which did nothing, is gone: View Report saves the report as a CSV
  file, which opens in Excel.
- The report viewer opens maximised, and neither viewing a report with data
  nor closing the viewer crashes any more.
- View Report and Close are no longer cut off on date-range reports.
- Run Data Quality Checks with nothing selected shows a message instead of
  crashing.

### Patient import

- The template comes from **General Admin → Generate import template**, so
  its columns match the database. The outdated import.xls is no longer
  installed, and Import_README.txt explains each step.
- Typed dates are read day-first and strictly. A date that can't be read
  sends the row to the error file instead of being guessed.
- Cellphone numbers are stored the same way as on the patient screens.
- Import problems are written to idart.log.

### General Admin and labels

- Facility details containing an apostrophe, such as "St Mary's Pharmacy",
  can be saved.
- Saving in "Update a Pharmacy" mode before choosing a pharmacy shows a
  message instead of crashing.
- Patient labels no longer print past the label's right edge, and a failed
  label print is recorded in idart.log.
- The welcome screen says "Welcome to the iDART Pharmacy".

### Other

- idart.log keeps more history: five files of 1 MB.
- The database connection wizard shows a message when it can't connect,
  and uses the new settings when you try again.
- Data submission to eKapa works again, for sites that use it.

### Upgrading

- Install 3.9.0 on every PC that uses the same database or restores its
  backups. The login screen shows the version.
- No manual database changes are needed. iDART updates the database
  structure itself the first time it opens it.
- Needs 64-bit Java 8 (Temurin) and PostgreSQL 14 or later.
