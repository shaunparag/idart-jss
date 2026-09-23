package model.manager.excel.reports.in;

import model.manager.excel.interfaces.ImportConverter;

import org.celllife.idart.integration.mobilisr.MobilisrManager;

/**
 * Stores imported cellphone numbers in the same form as the Add/Update
 * Patient screen, e.g. 0821234567 as 27821234567. The import doesn't
 * validate phone numbers.
 */
public class CellphoneConverter implements ImportConverter<String> {

	@Override
	public String convert(String rawValue) {
		return MobilisrManager.normaliseMsisdn(rawValue);
	}

	@Override
	public String getDescription() {
		return "A cellphone number";
	}
}
