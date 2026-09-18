package org.celllife.idart.database;

import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;

// Liquibase 2.0.2's PostgreSQL support conflates catalog (the database
// name, e.g. "test") and schema (e.g. "public") when resolving what to
// search for its own tracking tables (databasechangelog,
// databasechangeloglock):
//
// - PostgresDatabase.getDefaultCatalogName() hardcodes the literal string
//   "public" instead of the actual database name.
// - AbstractDatabase.getDefaultSchemaName() returns an internal field that
//   is never populated for Postgres, so once a request DOES carry a schema
//   name (e.g. after fixing the above), convertRequestedSchemaToCatalog()
//   stops calling getDefaultCatalogName() at all and just echoes the
//   schema string back as the catalog too - "public" again either way.
//
// Both were harmless against the old bundled JDBC driver (which didn't
// strictly filter DatabaseMetaData.getTables() by catalog/schema), but a
// modern, spec-compliant PostgreSQL driver enforces them correctly, so the
// lookup searches a nonexistent catalog/schema combination and returns zero
// rows - making Liquibase think its own tracking tables don't exist and try
// to recreate them, failing with "relation already exists". Overriding the
// two convert*() methods directly (rather than the getDefault*Name() ones
// they're supposed to fall back to) sidesteps that fallback chain entirely.
// Registered with DatabaseFactory (see DatabaseTools) at a higher priority
// than the stock PostgresDatabase so it's picked instead.
public class FixedPostgresDatabase extends PostgresDatabase {

	@Override
	public int getPriority() {
		return super.getPriority() + 1;
	}

	@Override
	public String getDefaultCatalogName() throws DatabaseException {
		return getConnection().getCatalog();
	}

	@Override
	public String getDefaultSchemaName() {
		return "public";
	}

	@Override
	public String convertRequestedSchemaToCatalog(String requestedSchema) throws DatabaseException {
		return getConnection().getCatalog();
	}

	@Override
	public String convertRequestedSchemaToSchema(String requestedSchema) throws DatabaseException {
		return "public";
	}

}
