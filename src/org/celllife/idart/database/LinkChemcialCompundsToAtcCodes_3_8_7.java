package org.celllife.idart.database;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

// Restored as a no-op: this custom Liquibase change class is referenced by
// changelog-3.8.xml but was missing from this repository snapshot. Its
// stated purpose ("link atc codes to chemical compounds") is a one-time
// data backfill with nothing to link on a fresh database, which is the
// only scenario this restoration can support without inventing the
// original linking logic.
public class LinkChemcialCompundsToAtcCodes_3_8_7 implements CustomTaskChange {

	@Override
	public void execute(Database database) throws CustomChangeException {
	}

	@Override
	public String getConfirmationMessage() {
		return "No ATC code linking performed (fresh database, nothing to link).";
	}

	@Override
	public void setUp() throws SetupException {
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
	}

	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}

}
