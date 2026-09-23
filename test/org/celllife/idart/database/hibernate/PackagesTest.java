package org.celllife.idart.database.hibernate;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class PackagesTest {

	private static int nextAfter(String packageId, int weeksSupply) {
		Packages pack = new Packages();
		pack.setPackageId(packageId);
		pack.setWeekssupply(weeksSupply);
		return pack.getNextIssueNo();
	}

	@Test
	public void followsTheWholePackageNumber() {
		assertEquals(nextAfter("240703A-00013-9", 4), 10);
		assertEquals(nextAfter("240703A-00013-10", 4), 11);
		assertEquals(nextAfter("240703A-00013-19", 4), 20);
		assertEquals(nextAfter("070203B-NM-2774-3", 4), 4);
	}

	@Test
	public void countsOneNumberPerMonthSupplied() {
		assertEquals(nextAfter("240703A-00013-12", 8), 14);
		assertEquals(nextAfter("240703A-00013-12", 2), 13);
	}

	@Test
	public void startsAtOneWithoutAPackageNumber() {
		assertEquals(nextAfter(null, 4), 1);
		assertEquals(nextAfter("", 4), 1);
		assertEquals(nextAfter("destroyedStock", 4), 1);
	}
}
