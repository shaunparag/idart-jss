package org.celllife.idart.database;

import static org.testng.Assert.assertTrue;

import java.util.List;

import org.celllife.idart.test.HibernateTest;
import org.testng.annotations.Test;

public class DatabaseIndexesTest extends HibernateTest {

	/**
	 * Changeset 3.9.2 indexes the columns that link the large tables; without
	 * them reports on a large database read whole tables.
	 */
	@Test
	public void linkColumnsAreIndexed() {
		List<?> indexes = getSession().createSQLQuery(
				"select indexname from pg_indexes where schemaname = 'public'")
				.list();
		String[] expected = { "idx_prescription_patient",
				"idx_prescribeddrugs_prescription", "idx_package_prescription",
				"idx_package_packageid", "idx_package_packdate",
				"idx_package_pickupdate", "idx_packageddrugs_parentpackage",
				"idx_packageddrugs_stock", "idx_packagedruginfotmp_packageddrug",
				"idx_appointment_patient", "idx_pillcount_previouspackage",
				"idx_accumulateddrugs_withpackage",
				"idx_accumulateddrugs_pillcount", "idx_episode_patient",
				"idx_patientidentifier_patient", "idx_patientattribute_patient",
				"idx_patientvisit_patient", "idx_stock_drug" };
		for (String index : expected) {
			assertTrue(indexes.contains(index), index);
		}
	}
}
