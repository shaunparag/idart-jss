package model.manager;

import static org.testng.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.celllife.idart.database.hibernate.PatientVisit;
import org.celllife.idart.test.HibernateTest;
import org.hibernate.Hibernate;
import org.testng.annotations.Test;

public class PatientVisitsReportTest extends HibernateTest {

	private static final int REASON = 5;

	@Test
	public void visitsWithNoPatientAreLeftOut() throws Exception {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
		int patient = ((Number) getSession().createSQLQuery(
				"select min(id) from patient").uniqueResult()).intValue();
		// a visit for a patient, and two with no patient like those iDART
		// records when stock is destroyed: one with a date, one without
		addVisit(patient, f.parse("1990-06-15"));
		addVisit(null, f.parse("1990-06-16"));
		addVisit(null, null);

		Date start = f.parse("1990-06-01");
		Date end = f.parse("1990-06-30");
		List<PatientVisit> visits = PAVASManager.getVisitsforAllPatients(
				getSession(), start, end);
		assertEquals(visits.size(), 1);
		assertEquals(visits.get(0).getpatientid(), patient);
		assertEquals(PAVASManager.getTotalVisits(getSession(), start, end), 1);
		assertEquals(PAVASManager.getTotalPatients(getSession(), start, end), 1);
		assertEquals(PAVASManager.getTotalVisitsforReason(getSession(), REASON,
				start, end), 1);
		assertEquals(PAVASManager.getTotalPatientsforReason(getSession(),
				REASON, start, end), 1);
	}

	private void addVisit(Integer patient, Date date) {
		getSession().createSQLQuery(
				"insert into patientvisit (id, patient_id, dateofvisit, isscheduled, "
						+ "patientvisitreason_id, diagnosis, notes) values "
						+ "(nextval('hibernate_sequence'), :patient, :date, 'Y', "
						+ ":reason, '', 'Scheduled Visit to Receive Package')")
				.setParameter("patient", patient, Hibernate.INTEGER)
				.setParameter("date", date, Hibernate.DATE)
				.setInteger("reason", REASON).executeUpdate();
	}
}
