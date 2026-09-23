iDART
======
iDART is a software solution designed to support the dispensing of ARV drugs in the public health care sector. It supports pharmacists in their important role of dispensing accurately to an increasing number of patients whilst still being able to engage and assist the patient.

iDART is a product of [Cell-Life](http://www.cell-life.org/).
This branch is a fork of the code to be able to manage general purpose pharmacies instead of only ARV clinics. This branch is a work-in-progress to be implemented at [JSS Bilaspur](http://jssbilaspur.org).

The main project source is hosted at [iDART-Sourceforge](http://sourceforge.net/projects/idart/) and the Wiki for the same is located at [iDART - Cell-Life](http://wiki.cell-life.org/display/IDART)

Windows 11 build (3.9.0)
--
This fork runs on Windows 11 with 64-bit Java 8 and PostgreSQL 14 or later.

* [WINDOWS11_INSTALL.md](WINDOWS11_INSTALL.md): installing, moving data from an older iDART, backups and troubleshooting.
* [CHANGELOG.md](CHANGELOG.md): what changed in 3.9.0.

To build the installer, run `ant generateInstaller` with JDK 8 and Ant. It writes `idart-install-3.9.0.jar`. To run the tests, create an empty PostgreSQL database called `idart-trunk-testing`, owned by the user `idart` with password `idart`, then run `ant test`.

What is iDART?
--
iDART is a software solution designed to support the dispensing of ARV drugs in the public health care sector. It supports pharmacists in their important role of dispensing accurately to an increasing number of patients whilst still being able to engage and assist the patient.
The intelligent Dispensing of ART software is used by the pharmacist to manage the supplies of ARV stocks, print reports and manage collection of drugs by patients. The software is also designed to address the reporting requirements of Government, international funders (such as PEPFAR) and internal clinical data such as identifying patients who are have not collected their medication for an extended period of time.

Why does it exist?
--
* to increase capacity of pharmacies in public health clinics by providing a software that is focused on the specific needs of ARV dispensing.
* to facilitate dispensing in remote public health clinics
* to support the so-called "down - referral process"
* to support clinic information management by providing information on the status quo of patients.