package org.celllife.idart.database;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.celllife.idart.commonobjects.iDartProperties;

/**
 * Keeps backup.bat in the install folder pointed at the database iDART
 * uses. The installer writes the connection settings into it, but the
 * database connection wizard only saves them to idart.properties.
 */
public class BackupScript {

	private static final Logger log = Logger.getLogger(BackupScript.class);

	public static final String FILE = "backup.bat";

	// the script uses these values unquoted on the pg_dump command line
	private static final Pattern SAFE_VALUE = Pattern.compile("[\\w.-]+");

	/**
	 * Rewrites the connection settings in backup.bat to match the current
	 * connection, if they differ. Never throws: a failure is only logged.
	 */
	public static void pointAtCurrentDatabase() {
		File script = new File(FILE);
		if (!script.isFile()) {
			return;
		}
		try {
			Map<String, String> url = DatabaseTools._().decomposeConnectionURL();
			String current = new String(Files.readAllBytes(script.toPath()),
					StandardCharsets.ISO_8859_1);
			String updated = withSettings(current, url.get(DatabaseTools.DBHOST),
					url.get(DatabaseTools.DBPORT), url.get(DatabaseTools.DBNAME),
					iDartProperties.hibernateUsername);
			if (updated == null) {
				log.warn("Not updating " + FILE + ": the database connection settings"
						+ " contain characters it can't use. Check them in " + FILE + " by hand.");
			} else if (!updated.equals(current)) {
				Files.write(script.toPath(), updated.getBytes(StandardCharsets.ISO_8859_1));
				log.info("Pointed " + FILE + " at database " + url.get(DatabaseTools.DBNAME)
						+ " on " + url.get(DatabaseTools.DBHOST) + ".");
			}
		} catch (Exception e) {
			log.warn("Unable to update " + FILE + ". It may back up a different"
					+ " database from the one iDART uses.", e);
		}
	}

	/**
	 * Returns the script with its dbHost, dbPort, dbName and dbUser lines set
	 * to the given values, or null if a value would break the script.
	 */
	static String withSettings(String script, String host, String port, String name,
			String user) {
		String[][] settings = { { "dbHost", host }, { "dbPort", port },
				{ "dbName", name }, { "dbUser", user } };
		for (String[] setting : settings) {
			if (setting[1] == null || !SAFE_VALUE.matcher(setting[1]).matches()) {
				return null;
			}
			script = script.replaceAll("(?m)^(set \"" + setting[0] + "=)[^\"\\r\\n]*\"",
					"$1" + Matcher.quoteReplacement(setting[1]) + "\"");
		}
		return script;
	}
}
