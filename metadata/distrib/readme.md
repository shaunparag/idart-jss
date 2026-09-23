===================
iDART 3.9.0
===================

iDART is a software solution designed to support the dispensing of ARV drugs in the public health care sector. It supports pharmacists in their important role of dispensing accurately to an increasing number of patients whilst still being able to engage and assist the patient.

Requirements
============

* Windows 11, 64-bit
* 64-bit Java 8 runtime (Temurin 8)
* PostgreSQL 14 or later
* 200 MB hard disk space
* 512 MB RAM

Installation and Upgrade
========================

Full instructions on installing iDART on Windows 11, moving data from an
older installation and making backups are in WINDOWS11_INSTALL.md in the
iDART source repository.

If you have any difficulties the best way to get help is to send an
email to the implementers mailing list, idart-implementers@lists.sourceforge.net.
	
	If you are not already a member you can join the list by going 
	to this url: `<http://lists.sourceforge.net/lists/listinfo/idart-implementers>`_

ChangeLog
---------

3.9.0

* Runs on Windows 11 with 64-bit Java 8 and PostgreSQL 14 or later. The
  installer installs into the user's own folder without administrator
  rights.
* Databases from older iDART versions, including the 2012 builds, open
  directly.
* New "Backup iDART database" shortcut. The backup always covers the
  database iDART uses.
* Package numbers no longer repeat, and IDs that repeat in older data no
  longer break the Package Tracking report or Scan Out Packages to
  Patients.
* Fixes to Update Patient, cellphone numbers, facility details, patient
  import dates and several reports (Monthly Receipts and Issues, Cohorts,
  the report viewer).
* Drop-down boxes show their arrow on Windows 11.

The full list is in CHANGELOG.md in the iDART source repository.

Upgrade Notes
-------------

* Install 3.9.0 on every PC that uses the same database or restores its
  backups.
* No manual database changes are needed: iDART updates the database
  structure itself the first time it opens it.

.. This file uses reStructuredText markup (http://docutils.sourceforge.net/rst.html).