Importing patients into iDART
=============================

1. GET A TEMPLATE FROM IDART
----------------------------
In iDART go to General Admin -> Generate import template, and save the
file. Always start from a template made this way: its ID columns match
the patient ID types set up in your database.

If your iDART folder still has an import.xls from an older version,
don't use it. Its column names no longer match, and the import rejects
it with "Import is missing compulsory columns".

2. FILL IT IN
-------------
- Row 1 holds the column headings. Leave them as they are. Headings
  are not case sensitive, but they must be spelled the same.
- Rows 2 and 3 describe what each column needs. Delete them before
  importing. If you leave them in, the import reports them as 2 errors
  (the patients below them still import).
- Enter one patient per row.
- Each patient needs at least one of the ID columns and a Clinic.
  Also fill in First Name, Last Name, DOB and Sex.

3. SAVE AS .XLS
---------------
The import only reads the older Excel format:
- Excel: File -> Save As -> "Excel 97-2003 Workbook (*.xls)".
- LibreOffice or OpenOffice: Save As -> "Excel 97-2003 (.xls)".
An .xlsx file is rejected with "Unable to open the Excel sheet".

4. IMPORT
---------
General Admin -> Import patients. Choose the file, then enter the sheet
name when asked (a generated template's sheet is called Sheet1).
"Unable to open the Excel sheet" means the file isn't .xls or the sheet
name doesn't match.

When the import finishes, iDART says how many rows had errors and
offers to open the error file. Rows with errors are not imported. They
are copied, with a "Reason for Error" column, to a file named
idart-<number>-exportErrors.xls in the iDART install folder. Correct
the rows in that file and import it the same way; the Reason for Error
column is ignored.

More detail about errors may be in idart.log in the iDART install
folder.

DATES
-----
Type dates so that Excel recognises them as dates. Otherwise type them
as text in one of these forms, always with a 4-digit year:
15 Jun 1985, 15/06/1985, 15-06-1985, 1985/06/15 or 1985-06-15.
15/06/1985 is read as 15 June, never as a US-style month/day date.
A date in any other form (for example 06/15/1985 or 15/06/85) isn't
guessed: that row goes to the error file with the reason "Can't read
the date".

Episode Start Date and Episode Stop Date can't be in the future or
before 1990. If Episode Start Date is blank, the import uses today's
date.

OTHER COLUMNS
-------------
- ID columns: can't contain the characters ' ` or ^, and can't already
  belong to another patient.
- Sex: F, Female, M, Male, U or Unknown. Blank means Unknown.
- Clinic: capitals don't matter, but otherwise the name must match an
  existing clinic exactly. A name that doesn't match creates a new
  clinic, so check the spelling.
- Province: blank, or one of the provinces iDART lists.
- Episode Start Reason: blank (means New Patient), or one of the
  reasons iDART lists, such as New Patient, Transferred In or
  Restart ART.
- Episode Stop Date / Episode Stop Reason: only for patients who are no
  longer on treatment at this pharmacy.
- Next of kin name / Next of kin contact number: saved as the
  patient's treatment supporter.
- (ARV Start Date): the date the patient started ARVs.
- Address 1, 2 and 3 can each hold a full address if you need more
  than one (for example postal in Address 1, residential in Address 2).
  Address 3 and Episode Start Notes can also hold other information
  that has no column of its own.
