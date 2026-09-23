package org.celllife.idart.integration.mobilisr;

import static org.celllife.idart.integration.mobilisr.MobilisrManager.normaliseMsisdn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class MobilisrManagerTest {

	@Test
	public void convertsNumbersAsStaffTypeThem() {
		assertEquals(normaliseMsisdn("0821234567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("082 123 4567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("082-123-4567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("+27821234567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("+27 82 123 4567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("0612345678", "27"), "27612345678");
	}

	@Test
	public void leavesAnythingElseForTheValidator() {
		assertEquals(normaliseMsisdn("27821234567", "27"), "27821234567");
		assertEquals(normaliseMsisdn("", "27"), "");
		assertNull(normaliseMsisdn(null, "27"));
		assertEquals(normaliseMsisdn("0021234567", "27"), "0021234567");
		assertEquals(normaliseMsisdn("821234567", "27"), "821234567");
		assertEquals(normaliseMsisdn("0821234567", null), "0821234567");
		assertEquals(normaliseMsisdn("0821234567", ""), "0821234567");
	}
}
