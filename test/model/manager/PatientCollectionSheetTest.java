package model.manager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRQueryChunk;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.celllife.idart.database.hibernate.IdentifierType;
import org.celllife.idart.database.hibernate.PackagedDrugs;
import org.celllife.idart.database.hibernate.Packages;
import org.celllife.idart.database.hibernate.Patient;
import org.celllife.idart.database.hibernate.PatientIdentifier;
import org.celllife.idart.database.hibernate.Prescription;
import org.celllife.idart.test.HibernateTest;
import org.testng.annotations.Test;

/**
 * Runs the Patient Collection Sheet's query on the current database
 * structure, as JasperReports does when the sheets are printed.
 */
public class PatientCollectionSheetTest extends HibernateTest {

	private static final String REPORT = "Reports/patientCollectionSheet.jrxml";

	private static final int NATIONAL_ID_NUMBER = 3;

	private Patient patientWithPackage(String patientId) {
		Patient patient = utils.createPatient(patientId);
		Prescription prescription = utils.createPrescription(patient,
				utils.getDrugs(1), "240703A-" + patientId);
		Packages pack = new Packages();
		pack.setPrescription(prescription);
		pack.setPackageId("240703A-" + patientId + "-1");
		pack.setWeekssupply(4);
		pack.setPackDate(new Date());
		pack.setModified('Y');
		pack.setPackagedDrugs(new ArrayList<PackagedDrugs>());
		getSession().save(pack);
		return patient;
	}

	private List<Map<String, Object>> runReportQuery(String patientId)
			throws Exception {
		// the query runs on the session's connection, which only sees flushed rows
		getSession().flush();

		JasperDesign design = JRXmlLoader.load(REPORT);
		StringBuilder sql = new StringBuilder();
		int parameters = 0;
		for (JRQueryChunk chunk : design.getQuery().getChunks()) {
			if (chunk.getType() == JRQueryChunk.TYPE_PARAMETER) {
				assertEquals(chunk.getText(), "patientid");
				sql.append('?');
				parameters++;
			} else {
				sql.append(chunk.getText());
			}
		}

		PreparedStatement query = getSession().connection().prepareStatement(
				sql.toString());
		for (int i = 1; i <= parameters; i++) {
			query.setString(i, patientId);
		}
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		ResultSet result = query.executeQuery();
		ResultSetMetaData columns = result.getMetaData();
		while (result.next()) {
			Map<String, Object> row = new HashMap<String, Object>();
			for (int i = 1; i <= columns.getColumnCount(); i++) {
				row.put(columns.getColumnLabel(i), result.getObject(i));
			}
			rows.add(row);
		}
		query.close();
		return rows;
	}

	@Test
	public void showsTheNationalIdNumberAndAddress() throws Exception {
		Patient patient = patientWithPackage("CSHEET1");
		PatientIdentifier idNumber = new PatientIdentifier();
		idNumber.setPatient(patient);
		idNumber.setType((IdentifierType) getSession().get(
				IdentifierType.class, NATIONAL_ID_NUMBER));
		idNumber.setValue("8001015009087");
		getSession().save(idNumber);

		List<Map<String, Object>> rows = runReportQuery("CSHEET1");
		assertEquals(rows.size(), 1);
		assertEquals(rows.get(0).get("patient_idnum"), "8001015009087");
		assertEquals(rows.get(0).get("patient_address"),
				"12 Gabriel Road, 7800, Cape Town");
		assertEquals(rows.get(0).get("packid"), "240703A-CSHEET1-1");
	}

	@Test
	public void leavesOutBlankAddressLines() throws Exception {
		Patient patient = patientWithPackage("CSHEET2");
		patient.setAddress2(null);
		patient.setAddress3(" ");

		List<Map<String, Object>> rows = runReportQuery("CSHEET2");
		assertEquals(rows.size(), 1);
		assertEquals(rows.get(0).get("patient_address"), "12 Gabriel Road");
		assertNull(rows.get(0).get("patient_idnum"));
	}
}
