package model.manager.excel.reports.in;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import model.manager.excel.conversion.exceptions.PatientException;
import model.manager.excel.interfaces.ImportConverter;

import org.celllife.idart.commonobjects.iDartProperties;

public class DateConverter implements ImportConverter<Date> {

	/*
	 * Real Excel date cells reach this converter as iDART's own "dd MMM yyyy"
	 * (XLReadManager.readCell formats them with iDARTUtil.format). Typed
	 * dates are accepted in the importDateFormat setting or in these forms;
	 * 15/06/1985 is day/month, never US month/day.
	 *
	 * Parsing is strict and has to use the whole value. Lenient parsing used
	 * to read 15/06/1985 as the year 20, and a value no pattern matched was
	 * returned as null, which left the date of birth empty or put today's
	 * date in Episode Start Date. A value that can't be read now fails the
	 * row, so it goes to the error file.
	 */
	private static final String[] TEXT_PATTERNS = { "yyyy/MM/dd",
			"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy" };

	@Override
	public Date convert(String rawValue) throws PatientException {
		if (rawValue == null || rawValue.trim().isEmpty())
			return null;
		String value = rawValue.trim();

		Date date = parse("dd MMM yyyy", value);
		if (date == null)
			date = parse(twentyFourHour(iDartProperties.importDateFormat), value);
		for (int i = 0; date == null && i < TEXT_PATTERNS.length; i++)
			date = parse(TEXT_PATTERNS[i], value);

		if (date == null)
			throw new PatientException("Can't read the date '" + value
					+ "'. Use a date Excel recognises, or type it like"
					+ " 15 Jun 1985, 15/06/1985, 15-06-1985, 1985/06/15"
					+ " or 1985-06-15.");
		return date;
	}

	/*
	 * The installer sets importDateFormat to "dd MM yyyy hh:mm:ss". With no
	 * am/pm marker, hh can only mean 24-hour time, and strict parsing would
	 * otherwise reject 00:00:00 (hh only allows 1 to 12).
	 */
	private static String twentyFourHour(String pattern) {
		if (pattern == null || pattern.indexOf('a') >= 0)
			return pattern;
		return pattern.replace('h', 'H');
	}

	private Date parse(String pattern, String value) {
		if (pattern == null)
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		sdf.setLenient(false);
		ParsePosition pos = new ParsePosition(0);
		Date date = sdf.parse(value, pos);
		if (date == null || pos.getIndex() != value.length())
			return null;

		// A two-digit year such as 15/06/85 parses as the year 85
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		if (cal.get(Calendar.YEAR) < 1900)
			return null;
		return date;
	}

	@Override
	public String getDescription() {
		return "A date";
	}
}
