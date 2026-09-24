package model.manager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.celllife.idart.database.hibernate.Drug;
import org.celllife.idart.test.HibernateTest;
import org.testng.annotations.Test;

public class InactiveDrugTest extends HibernateTest {

	@Test
	public void existingDrugsAreActive() {
		// the drugs in the test data were inserted without the new column
		List<Drug> drugs = DrugManager.getAllDrugs(getSession());
		assertFalse(drugs.isEmpty());
		for (Drug drug : drugs) {
			assertTrue(drug.isActive(), drug.getName());
		}
		assertEquals(DrugManager.getActiveDrugs(getSession()).size(), drugs.size());
	}

	@Test
	public void inactiveDrugsAreLeftOutOfTheActiveList() {
		Drug drug = DrugManager.getAllDrugs(getSession()).get(0);
		drug.setActive(false);
		getSession().flush();
		getSession().clear();

		assertFalse(DrugManager.getDrug(getSession(), drug.getName()).isActive());
		assertFalse(contains(DrugManager.getActiveDrugs(getSession()), drug));
		assertTrue(contains(DrugManager.getAllDrugs(getSession()), drug));
	}

	@Test
	public void aNewDrugIsActive() {
		assertTrue(new Drug().isActive());
	}

	private static boolean contains(List<Drug> drugs, Drug drug) {
		for (Drug d : drugs) {
			if (d.getId() == drug.getId()) {
				return true;
			}
		}
		return false;
	}
}
