package org.celllife.idart.database;

import static org.celllife.idart.database.BackupScript.withSettings;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

public class BackupScriptTest {

	/** backup.bat as the installer writes it. */
	private String installedScript() throws Exception {
		String template = new String(Files.readAllBytes(Paths.get("metadata/install/backup.bat")),
				StandardCharsets.ISO_8859_1);
		return template.replace("$dbAddress", "localhost").replace("$dbName", "pharm")
				.replace("$dbUser", "postgres");
	}

	@Test
	public void pointsTheScriptAtANewDatabase() throws Exception {
		String installed = installedScript();
		assertEquals(withSettings(installed, "localhost", "5432", "pharm_test", "postgres"),
				installed.replace("set \"dbName=pharm\"", "set \"dbName=pharm_test\""));
	}

	@Test
	public void updatesEveryConnectionSettingWhateverTheLineEndings() throws Exception {
		String crlf = installedScript().replace("\n", "\r\n");
		String updated = withSettings(crlf, "192.168.1.20", "5433", "pharm_migrated", "idart");
		assertTrue(updated.contains("\r\nset \"dbHost=192.168.1.20\"\r\n"));
		assertTrue(updated.contains("\r\nset \"dbPort=5433\"\r\n"));
		assertTrue(updated.contains("\r\nset \"dbName=pharm_migrated\"\r\n"));
		assertTrue(updated.contains("\r\nset \"dbUser=idart\"\r\n"));
		assertTrue(updated.contains("\r\nset \"postgresDir=\"\r\n"));
	}

	@Test
	public void leavesAMatchingScriptAsItIs() throws Exception {
		String installed = installedScript();
		assertEquals(withSettings(installed, "localhost", "5432", "pharm", "postgres"), installed);
	}

	@Test
	public void refusesValuesThatWouldBreakTheScript() throws Exception {
		String installed = installedScript();
		assertNull(withSettings(installed, "localhost", "5432", "pharm test", "postgres"));
		assertNull(withSettings(installed, "localhost", "5432", "pharm&del", "postgres"));
		assertNull(withSettings(installed, "localhost", "5432", "pharm%x%", "postgres"));
		assertNull(withSettings(installed, "localhost", "5432", "pharm\"", "postgres"));
		assertNull(withSettings(installed, "localhost", "5432", "pharm", null));
	}
}
