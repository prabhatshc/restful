/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.postgresql;

import java.sql.SQLException;

import org.restsql.core.impl.AbstractSqlResourceMetaData;
import org.restsql.core.impl.ColumnMetaDataImpl;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * Implements SqlResourceMetaData for PostgreSQL.
 * 
 * @author Mark Sawers
 */
public class PostgreSqlSqlResourceMetaData extends AbstractSqlResourceMetaData {
	private static final String SQL_COLUMNS_QUERY = "select column_name, data_type, column_default from information_schema.columns where table_catalog = ? and table_name = ?";
	private static final String SQL_PK_QUERY = "select column_name from information_schema.table_constraints tc, information_schema.key_column_usage kcu"
			+ " where tc.constraint_catalog = ? and tc.table_name = ?"
			+ " and tc.constraint_type = 'PRIMARY KEY'"
			+ " and tc.constraint_schema = kcu.constraint_schema and tc.table_name = kcu.table_name"
			+ " and tc.constraint_name = kcu.constraint_name";
	private static final String SQL_TABLE_SCHEMA_QUERY = "select table_schema from information_schema.tables where table_catalog = ? and table_name = ?";

	/**
	 * Retrieves database name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	@Override
	protected String getColumnDatabaseName(
			final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		return SqlResourceDefinitionUtils.getDefaultDatabase(definition);
	}

	/**
	 * Retrieves actual column name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	@Override
	protected String getColumnName(final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		// return ((PGResultSetMetaData)
		// resultSetMetaData).getBaseColumnName(colNumber);
		return resultSetMetaData.getColumnName(colNumber);
	}

	/**
	 * Retrieves table name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	@Override
	protected String getColumnTableName(final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		// return ((PGResultSetMetaData)
		// resultSetMetaData).getBaseTableName(colNumber);
		return resultSetMetaData.getTableName(colNumber);
	}

	/**
	 * Retrieves sql for querying columns. Hook method for
	 * buildInvisibleForeignKeys() and buildJoinTableMetadata() allows
	 * database-specific overrides.
	 */
	@Override
	protected String getSqlColumnsQuery() {
		return SQL_COLUMNS_QUERY;
	}

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys
	 * allows database-specific overrides.
	 */
	@Override
	protected String getSqlPkQuery() {
		return SQL_PK_QUERY;
	}

	/** Retrieves database-specific table name used in SQL statements. */
	@Override
	protected String getQualifiedTableName(
			final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		// final PGResultSetMetaData pgMetaData = (PGResultSetMetaData)
		// resultSetMetaData;
		// return SqlResourceDefinitionUtils.getDefaultDatabase(definition) +
		// "."
		// + pgMetaData.getBaseSchemaName(colNumber) + "." +
		// pgMetaData.getBaseTableName(colNumber);
		return SqlResourceDefinitionUtils.getDefaultDatabase(definition) + "."
				+ resultSetMetaData.getSchemaName(colNumber) + "."
				+ resultSetMetaData.getTableName(colNumber);
	}

	/**
	 * Retrieves database-specific table name used in SQL statements. Used to
	 * build join table meta data.
	 */
	@Override
	protected String getQualifiedTableName(String databaseName, String tableName) {

		SqlRowSet resultSet = null;

		String schemaName = "unknown";

		resultSet = this.jdbcTemplate.queryForRowSet(SQL_TABLE_SCHEMA_QUERY,
				databaseName, tableName);
		if (resultSet.next()) {
			schemaName = resultSet.getString(1);
		}
		return databaseName + "." + schemaName + "." + tableName;

	}

	/**
	 * Sets sequence metadata for a column with the columns query result set.
	 * The column_default column will contain a string in the format
	 * nextval('sequence-name'::regclass), where sequence-name is the sequence
	 * name.
	 * 
	 * @throws SQLException
	 *             when a database error occurs
	 */
	@Override
	protected void setSequenceMetaData(ColumnMetaDataImpl column,
			SqlRowSet resultSet) {
		final String columnDefault = resultSet.getString(3);
		if (columnDefault != null && columnDefault.startsWith("nextval")) {
			column.setSequence(true);
			column.setSequenceName(columnDefault.substring(9,
					columnDefault.indexOf('\'', 10)));
		}
	}
}
