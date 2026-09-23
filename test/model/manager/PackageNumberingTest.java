package model.manager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import java.util.ArrayList;
import java.util.Date;

import org.celllife.idart.database.hibernate.PackagedDrugs;
import org.celllife.idart.database.hibernate.Packages;
import org.celllife.idart.database.hibernate.Patient;
import org.celllife.idart.database.hibernate.Prescription;
import org.celllife.idart.test.HibernateTest;
import org.testng.annotations.Test;

public class PackageNumberingTest extends HibernateTest {

	private static final long DAY = 24L * 60 * 60 * 1000;

	private Prescription newPrescription(String patientId) {
		Patient patient = utils.createPatient(patientId);
		Prescription prescription = utils.createPrescription(patient,
				utils.getDrugs(1), "240703A-" + patientId);
		getSession().flush();
		return prescription;
	}

	private Packages addPackage(Prescription prescription, String packageId,
			int weeksSupply, int packedDaysAgo, Date pickupDate) {
		Packages pack = new Packages();
		pack.setPrescription(prescription);
		pack.setPackageId(packageId);
		pack.setWeekssupply(weeksSupply);
		pack.setPackDate(new Date(System.currentTimeMillis() - packedDaysAgo * DAY));
		pack.setPickupDate(pickupDate);
		pack.setModified('Y');
		pack.setPackagedDrugs(new ArrayList<PackagedDrugs>());
		getSession().save(pack);
		// the app's sessions only flush on commit
		getSession().flush();
		return pack;
	}

	@Test
	public void numbersContinueFromTheHighestPackageOnThePrescription() {
		Prescription pre = newPrescription("NUMTEST1");
		assertEquals(PackageManager.getNextIssueNo(getSession(), pre), 1);

		addPackage(pre, "240703A-NUMTEST1-9", 4, 60, new Date());
		assertEquals(PackageManager.getNextIssueNo(getSession(), pre), 10);

		addPackage(pre, "240703A-NUMTEST1-10", 4, 30, new Date());
		assertEquals(PackageManager.getNextIssueNo(getSession(), pre), 11);

		// the old numbering went back to -1 after -10
		addPackage(pre, "240703A-NUMTEST1-1", 4, 1, new Date());
		assertEquals(PackageManager.getNextIssueNo(getSession(), pre), 11);

		// a package not collected yet still uses its number
		addPackage(pre, "240703A-NUMTEST1-11", 8, 0, null);
		assertEquals(PackageManager.getNextIssueNo(getSession(), pre), 13);
	}

	@Test
	public void findsThePackageWaitingToBeCollectedWhenAnIdRepeats() {
		Prescription pre = newPrescription("DUPTEST1");
		addPackage(pre, "240703A-DUPTEST1-9", 4, 60, new Date());
		Packages waiting = addPackage(pre, "240703A-DUPTEST1-9", 4, 2, null);
		addPackage(pre, "240703A-DUPTEST1-2", 4, 1, new Date());

		assertSame(PackageManager.getPackage(getSession(), "240703a-duptest1-9"), waiting);
	}

	@Test
	public void findsTheMostRecentPackageWhenAllWithTheIdWereCollected() {
		Prescription pre = newPrescription("DUPTEST2");
		addPackage(pre, "240703A-DUPTEST2-9", 4, 60, new Date());
		Packages recent = addPackage(pre, "240703A-DUPTEST2-9", 4, 2, new Date());

		assertSame(PackageManager.getPackage(getSession(), "240703A-DUPTEST2-9"), recent);
		assertNull(PackageManager.getPackage(getSession(), "240703A-DUPTEST2-99"));
	}
}
