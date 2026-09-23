package model.manager;

import static org.testng.Assert.assertEquals;
import model.nonPersistent.PharmacyDetails;

import org.celllife.idart.test.HibernateTest;
import org.testng.annotations.Test;

public class PharmacyDetailsTest extends HibernateTest {

	@Test
	public void savesDetailsContainingApostrophes() {
		PharmacyDetails details = AdministrationManager.getPharmacyDetails(getSession());
		details.setPharmacyName("St Mary's Pharmacy");
		details.setPharmacist("O'Brien, B.Pharm");
		details.setAssistantPharmacist("D'Souza, B.Pharm");
		details.setStreet("1 King's Road");
		details.setCity("Cape Town");
		details.setContactNo("021 555 0000");

		AdministrationManager.savePharmacyDetails(getSession(), details);
		// read back from the database, not the session's cached rows
		getSession().clear();

		PharmacyDetails saved = AdministrationManager.getPharmacyDetails(getSession());
		assertEquals(saved.getPharmacyName(), "St Mary's Pharmacy");
		assertEquals(saved.getPharmacist(), "O'Brien, B.Pharm");
		assertEquals(saved.getAssistantPharmacist(), "D'Souza, B.Pharm");
		assertEquals(saved.getStreet(), "1 King's Road");
		assertEquals(saved.getCity(), "Cape Town");
		assertEquals(saved.getContactNo(), "021 555 0000");
	}
}
