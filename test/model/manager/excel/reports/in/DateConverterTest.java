package model.manager.excel.reports.in;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import model.manager.excel.conversion.exceptions.PatientException;

import org.celllife.idart.commonobjects.iDartProperties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DateConverterTest {

	private String savedImportDateFormat;

	@BeforeMethod
	public void setImportDateFormat() {
		savedImportDateFormat = iDartProperties.importDateFormat;
		// the value the installer's idart.properties template sets
		iDartProperties.importDateFormat = "dd MM yyyy hh:mm:ss";
	}

	@AfterMethod
	public void restoreImportDateFormat() {
		iDartProperties.importDateFormat = savedImportDateFormat;
	}

	@Test
	public void acceptsSupportedForms() throws PatientException {
		Date expected = date(1985, Calendar.JUNE, 15);
		// what a real Excel date cell is read as
		String excelCell = new SimpleDateFormat("dd MMM yyyy").format(expected);
		String[] values = { excelCell, "15/06/1985", "15-06-1985",
				"1985/06/15", "1985-06-15", "15 06 1985 00:00:00",
				"  15/06/1985  " };
		for (String value : values) {
			assertEquals(new DateConverter().convert(value), expected, value);
		}
		assertEquals(new DateConverter().convert("5/6/1985"),
				date(1985, Calendar.JUNE, 5));

		// importDateFormat's hh is read as a 24-hour clock
		Calendar afternoon = Calendar.getInstance();
		afternoon.setTime(expected);
		afternoon.set(Calendar.HOUR_OF_DAY, 13);
		afternoon.set(Calendar.MINUTE, 30);
		assertEquals(new DateConverter().convert("15 06 1985 13:30:00"),
				afternoon.getTime());
	}

	@Test
	public void rejectsDatesItCannotReadInsteadOfGuessing() {
		// Each of these used to import as a wrong date, or as no date
		String[] values = { "06/15/1985", "31/02/1985", "15/06/85",
				"15.06.1985", "1985/06/15 extra", "June 15 1985", "abc" };
		for (String value : values) {
			try {
				Date d = new DateConverter().convert(value);
				fail("'" + value + "' was read as " + d);
			} catch (PatientException expected) {
				// the row goes to the import's error file
			}
		}
	}

	@Test
	public void blankIsNotAnError() throws PatientException {
		assertNull(new DateConverter().convert(null));
		assertNull(new DateConverter().convert(""));
		assertNull(new DateConverter().convert("   "));
	}

	private static Date date(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month, day);
		return cal.getTime();
	}
}
