package org.celllife.idart.database;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import liquibase.change.CheckSum;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.testng.annotations.Test;

public class ChangelogChecksumTest {

	private static final String CHANGELOG = "org/celllife/idart/database/changelog-3.8.xml";

	/**
	 * A database migrated from a 2012 iDART holds these checksums, taken of
	 * the same files with Windows line endings. It must still open.
	 */
	@Test
	public void acceptsChecksumsFromThe2012Builds() throws Exception {
		assertTrue(changeSet("3.8.2").isCheckSumValid(CheckSum.parse("3:c1e8e01e3d81b17d8ef21b393f207c25")));
		assertTrue(changeSet("3.8.4").isCheckSumValid(CheckSum.parse("3:5054cfc4ca0a4e0d0a00b867e34e6c84")));
	}

	@Test
	public void otherChangeSetsStillCheckTheirChecksum() throws Exception {
		assertFalse(changeSet("3.8.3").isCheckSumValid(CheckSum.parse("3:0123456789abcdef0123456789abcdef")));
	}

	private ChangeSet changeSet(String id) throws Exception {
		ResourceAccessor accessor = new ClassLoaderResourceAccessor();
		DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
				.getParser(CHANGELOG, accessor)
				.parse(CHANGELOG, new ChangeLogParameters(), accessor);
		for (ChangeSet changeSet : changeLog.getChangeSets()) {
			if (changeSet.getId().equals(id)) {
				return changeSet;
			}
		}
		fail("No changeset " + id + " in " + CHANGELOG);
		return null;
	}
}
