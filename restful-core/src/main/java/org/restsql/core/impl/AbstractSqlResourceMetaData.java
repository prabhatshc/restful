/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.TableMetaData.TableRole;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;
import org.restsql.core.sqlresource.Table;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * Represents meta data for sql resource. Queries database for table and column
 * meta data and primary and foreign keys.
 * 
 * @author Mark Sawers
 */
@XmlRootElement(name = "sqlResourceMetaData", namespace = "http://restsql.org/schema")
@XmlType(name = "SqlResourceMetaData", namespace = "http://restsql.org/schema", propOrder = {
		"resName", "hierarchical", "multipleDatabases", "tables",
		"parentTableName", "childTableName", "joinTableName",
		"parentPlusExtTableNames", "childPlusExtTableNames", "joinTableNames",
		"allReadColumnNames", "parentReadColumnNames", "childReadColumnNames" })
public abstract class AbstractSqlResourceMetaData implements
		SqlResourceMetaData {
	private static final int DEFAULT_NUMBER_DATABASES = 5;
	private static final int DEFAULT_NUMBER_TABLES = 10;

	@XmlElementWrapper(name = "allReadColumns", required = true)
	@XmlElement(name = "column", required = true)
	private List<String> allReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> allReadColumns;

	@XmlElementWrapper(name = "childPlusExtTables", required = true)
	@XmlElement(name = "table")
	private List<String> childPlusExtTableNames;

	@XmlTransient
	private List<TableMetaData> childPlusExtTables;

	@XmlElementWrapper(name = "childReadColumns", required = true)
	@XmlElement(name = "column")
	private List<String> childReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> childReadColumns;

	@XmlTransient
	private TableMetaData childTable;

	@XmlElement(name = "childTable")
	private String childTableName;

	@XmlTransient
	private SqlResourceDefinition definition;

	@XmlTransient
	private boolean extendedMetadataIsBuilt;

	@XmlAttribute
	private boolean hierarchical;

	@XmlTransient
	private List<TableMetaData> joinList;

	@XmlTransient
	private TableMetaData joinTable;

	@XmlElement(name = "joinTable")
	private String joinTableName;

	@XmlElementWrapper(name = "joinTables")
	@XmlElement(name = "table")
	private List<String> joinTableNames;

	@XmlAttribute
	private boolean multipleDatabases;

	@XmlElementWrapper(name = "parentPlusExtTables", required = true)
	@XmlElement(name = "table", required = true)
	private List<String> parentPlusExtTableNames;

	@XmlTransient
	private List<TableMetaData> parentPlusExtTables;

	@XmlElementWrapper(name = "parentReadColumns", required = true)
	@XmlElement(name = "column", required = true)
	private List<String> parentReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> parentReadColumns;

	@XmlTransient
	private TableMetaData parentTable;

	@XmlElement(name = "parentTable", required = true)
	private String parentTableName;

	@XmlAttribute(required = true)
	private String resName;

	/** Map<database.table, TableMetaData> */
	@XmlTransient
	private Map<String, TableMetaData> tableMap;

	@XmlElementWrapper(name = "tables", required = true)
	@XmlElement(name = "table", type = TableMetaDataImpl.class, required = true)
	private List<TableMetaData> tables;

	// Public methods to retrieve metadata

	@Override
	public List<ColumnMetaData> getAllReadColumns() {
		return allReadColumns;
	}

	@Override
	public TableMetaData getChild() {
		return childTable;
	}

	@Override
	public List<TableMetaData> getChildPlusExtTables() {
		return childPlusExtTables;
	}

	@Override
	public List<ColumnMetaData> getChildReadColumns() {
		return childReadColumns;
	}

	@Override
	public TableMetaData getJoin() {
		return joinTable;
	}

	@Override
	public List<TableMetaData> getJoinList() {
		return joinList;
	}

	@Override
	public int getNumberTables() {
		return tables.size();
	}

	@Override
	public TableMetaData getParent() {
		return parentTable;
	}

	@Override
	public List<TableMetaData> getParentPlusExtTables() {
		return parentPlusExtTables;
	}

	@Override
	public List<ColumnMetaData> getParentReadColumns() {
		return parentReadColumns;
	}

	@Override
	public Map<String, TableMetaData> getTableMap() {
		return tableMap;
	}

	@Override
	public List<TableMetaData> getTables() {
		return tables;
	}

	@Override
	public boolean hasJoinTable() {
		return joinTable != null;
	}

	@Override
	public boolean hasMultipleDatabases() {
		return multipleDatabases;
	}

	@Override
	public boolean isHierarchical() {
		return hierarchical;
	}

	/** Populates metadata using definition. */
	@Override
	public void init(final String resName,
			final SqlResourceDefinition definition, DataSource dataSource)
			throws SqlResourceException {
		this.resName = resName;
		this.definition = definition;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		String sql = null;
		SqlResourceDefinitionUtils.validate(definition);

		sql = getSqlMainQuery(definition);
		final SqlRowSet resultSet = this.jdbcTemplate.queryForRowSet(sql);
		resultSet.next();
		buildTablesAndColumns(resultSet);

		buildPrimaryKeys();
		buildInvisibleForeignKeys();
		buildJoinTableMetadata();
		buildSequenceMetaData();

		hierarchical = getChild() != null;
	}

	/** Returns XML representation. */
	@Override
	public String toXml() {
		// Build extended metadata for serialization if first time through
		if (!extendedMetadataIsBuilt) {
			parentTableName = getQualifiedTableName(parentTable);
			childTableName = getQualifiedTableName(childTable);
			joinTableName = getQualifiedTableName(joinTable);
			parentPlusExtTableNames = getQualifiedTableNames(parentPlusExtTables);
			childPlusExtTableNames = getQualifiedTableNames(childPlusExtTables);
			allReadColumnNames = getQualifiedColumnNames(allReadColumns);
			childReadColumnNames = getQualifiedColumnNames(childReadColumns);
			parentReadColumnNames = getQualifiedColumnNames(parentReadColumns);
			extendedMetadataIsBuilt = true;
		}

		try {
			final JAXBContext context = JAXBContext
					.newInstance(AbstractSqlResourceMetaData.class);
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter writer = new StringWriter();
			marshaller.marshal(this, writer);
			return writer.toString();
		} catch (final JAXBException exception) {
			return exception.toString();
		}
	}

	// Protected methods for database-specific implementation

	/**
	 * Retrieves database name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	protected String getColumnDatabaseName(
			final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		return resultSetMetaData.getCatalogName(colNumber);
	}

	/**
	 * Retrieves actual column name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	protected String getColumnName(final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		return resultSetMetaData.getColumnName(colNumber);
	}

	/**
	 * Retrieves table name from result set meta data. Hook method for
	 * buildTablesAndColumns() allows database-specific overrides.
	 */
	protected String getColumnTableName(final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber) {
		return resultSetMetaData.getTableName(colNumber);
	}

	/**
	 * Retrieves database-specific table name used in SQL statements. Used to
	 * build join table meta data.
	 */
	protected abstract String getQualifiedTableName(String databaseName,
			String tableName);

	/** Retrieves database-specific table name used in SQL statements. */
	protected abstract String getQualifiedTableName(
			final SqlResourceDefinition definition,
			final SqlRowSetMetaData resultSetMetaData, final int colNumber);

	/**
	 * Retrieves sql for querying columns. Hook method for
	 * buildInvisibleForeignKeys() and buildJoinTableMetadata() allows
	 * database-specific overrides.
	 */
	protected abstract String getSqlColumnsQuery();

	/**
	 * Retrieves sql for the main query based on the definition. Optimized to
	 * retrieve only one row. Hook method for constructor allows
	 * database-specific overrides.
	 */
	protected String getSqlMainQuery(final SqlResourceDefinition definition) {
		return definition.getQuery().getValue() + " LIMIT 1 OFFSET 0";
	}

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys
	 * allows database-specific overrides.
	 */
	protected abstract String getSqlPkQuery();

	/**
	 * Sets sequence metadata for a column with the columns query result set.
	 * 
	 * @throws SQLException
	 *             when a database error occurs
	 */
	protected abstract void setSequenceMetaData(ColumnMetaDataImpl column,
			SqlRowSet resultSet);

	// Private methods

	private void buildInvisibleForeignKeys() {

		SqlRowSet resultSet = null;

		for (final TableMetaData table : tables) {
			if (!table.isParent()) {
				// statement.setString(1, table.getDatabaseName());
				// statement.setString(2, table.getTableName());
				resultSet = this.jdbcTemplate.queryForRowSet(
						getSqlColumnsQuery(), table.getDatabaseName(),
						table.getTableName());
				while (resultSet.next()) {
					final String columnName = resultSet.getString(1);
					if (!table.getColumns().containsKey(columnName)) {
						TableMetaData mainTable;
						switch (table.getTableRole()) {
						case ChildExtension:
							mainTable = childTable;
							break;
						default: // Child, ParentExtension, Unknown
							mainTable = parentTable;
						}
						// Look for a pk on the main table with the same
						// name
						for (final ColumnMetaData pk : mainTable
								.getPrimaryKeys()) {
							if (columnName.equals(pk.getColumnName())) {
								final ColumnMetaDataImpl fkColumn = new ColumnMetaDataImpl(
										table.getDatabaseName(),
										table.getQualifiedTableName(),
										table.getTableName(),
										table.getTableRole(), columnName,
										pk.getColumnLabel(),
										resultSet.getString(2), this);
								((TableMetaDataImpl) table).addColumn(fkColumn);
							}
						}
					}
				}
			}
		}

	}

	private void buildJoinTableMetadata() {
		// Join table could have been identified in buildTablesAndColumns(), but
		// not always
		final Table joinDef = SqlResourceDefinitionUtils.getTable(definition,
				TableRole.Join);
		if (joinDef != null && joinTable == null) {
			// Determine table and database name
			String tableName, databaseName;
			final String possiblyQualifiedTableName = joinDef.getName();
			final int dotIndex = possiblyQualifiedTableName.indexOf('.');
			if (dotIndex > 0) {
				tableName = possiblyQualifiedTableName.substring(0, dotIndex);
				databaseName = possiblyQualifiedTableName
						.substring(dotIndex + 1);
			} else {
				tableName = possiblyQualifiedTableName;
				databaseName = SqlResourceDefinitionUtils
						.getDefaultDatabase(definition);
			}

			final String qualifiedTableName = getQualifiedTableName(
					databaseName, tableName);

			// Create table and add to special lists
			joinTable = new TableMetaDataImpl(tableName, qualifiedTableName,
					databaseName, TableRole.Join);
			tableMap.put(joinTable.getQualifiedTableName(), joinTable);
			tables.add(joinTable);
			joinList = new ArrayList<TableMetaData>(1);
			joinList.add(joinTable);

			// Execute metadata query and populate metadata structure

			SqlRowSet resultSet = null;

			resultSet = this.jdbcTemplate.queryForRowSet(getSqlColumnsQuery(),
					databaseName, tableName);
			while (resultSet.next()) {
				final String columnName = resultSet.getString(1);
				final ColumnMetaDataImpl column = new ColumnMetaDataImpl(
						databaseName, qualifiedTableName, tableName,
						TableRole.Join, columnName, columnName,
						resultSet.getString(2), this);
				((TableMetaDataImpl) joinTable).addColumn(column);
			}

		}
	}

	/**
	 * Builds list of primary key column labels.
	 * 
	 * @param Connection
	 *            connection
	 * @throws SqlResourceException
	 *             if a database access error occurs
	 */
	private void buildPrimaryKeys() {

		SqlRowSet resultSet = null;

		for (final TableMetaData table : tables) {
			// statement.setString(1, table.getDatabaseName());
			// statement.setString(2, table.getTableName());
			resultSet = this.jdbcTemplate.queryForRowSet(getSqlPkQuery(),
					table.getDatabaseName(), table.getTableName());
			while (resultSet.next()) {
				final String columnName = resultSet.getString(1);
				for (final ColumnMetaData column : table.getColumns().values()) {
					if (columnName.equals(column.getColumnName())) {
						((ColumnMetaDataImpl) column).setPrimaryKey(true);
						((TableMetaDataImpl) table).addPrimaryKey(column);
					}
				}
			}
		}

	}

	/**
	 * Builds sequence metadata for all columns.
	 * 
	 * @param connection
	 *            database connection
	 * @throws SQLException
	 *             if a database access error occurs
	 */
	private void buildSequenceMetaData() {

		SqlRowSet resultSet = null;

		for (final TableMetaData table : tables) {
			// statement.setString(1, table.getDatabaseName());
			// statement.setString(2, table.getTableName());
			resultSet = this.jdbcTemplate.queryForRowSet(getSqlColumnsQuery(),
					table.getDatabaseName(), table.getTableName());
			while (resultSet.next()) {
				final String columnName = resultSet.getString(1);
				for (ColumnMetaData column : table.getColumns().values()) {
					if (column.getColumnName().equals(columnName)) {
						setSequenceMetaData((ColumnMetaDataImpl) column,
								resultSet);
						break;
					}
				}
			}
		}

	}

	/**
	 * Builds table and column meta data.
	 * 
	 * @throws SqlResourceException
	 */
	@SuppressWarnings("fallthrough")
	private void buildTablesAndColumns(final SqlRowSet resultSet)
			throws SqlResourceException {
		final SqlRowSetMetaData resultSetMetaData = resultSet.getMetaData();
		final int columnCount = resultSetMetaData.getColumnCount();

		allReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		parentReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		childReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		tableMap = new HashMap<String, TableMetaData>(DEFAULT_NUMBER_TABLES);
		tables = new ArrayList<TableMetaData>(DEFAULT_NUMBER_TABLES);
		childPlusExtTables = new ArrayList<TableMetaData>(DEFAULT_NUMBER_TABLES);
		parentPlusExtTables = new ArrayList<TableMetaData>(
				DEFAULT_NUMBER_TABLES);
		final HashSet<String> databases = new HashSet<String>(
				DEFAULT_NUMBER_DATABASES);

		for (int colNumber = 1; colNumber <= columnCount; colNumber++) {
			final String databaseName, qualifiedTableName, tableName;
			// boolean readOnly = isColumnReadOnly(resultSetMetaData,
			// colNumber);
			// if (readOnly) {
			databaseName = SqlResourceDefinitionUtils
					.getDefaultDatabase(definition);
			tableName = SqlResourceDefinitionUtils.getTable(definition,
					TableRole.Parent).getName();
			qualifiedTableName = getQualifiedTableName(databaseName, tableName);

			final ColumnMetaDataImpl column = new ColumnMetaDataImpl(colNumber,
					databaseName, qualifiedTableName, tableName, getColumnName(
							definition, resultSetMetaData, colNumber),
					resultSetMetaData.getColumnLabel(colNumber),
					resultSetMetaData.getColumnTypeName(colNumber),
					resultSetMetaData.getColumnType(colNumber), true, this);

			TableMetaDataImpl table = (TableMetaDataImpl) tableMap.get(column
					.getQualifiedTableName());
			if (table == null) {
				// Create table metadata object and add to special references
				final Table tableDef = SqlResourceDefinitionUtils.getTable(
						definition, column);
				if (tableDef == null) {
					throw new SqlResourceException(
							"Definition requires table element for "
									+ column.getTableName()
									+ ", referenced by column "
									+ column.getColumnLabel());
				}
				table = new TableMetaDataImpl(tableName, qualifiedTableName,
						databaseName, TableRole.valueOf(tableDef.getRole()));
				tableMap.put(column.getQualifiedTableName(), table);
				tables.add(table);

				switch (table.getTableRole()) {
				case Parent:
					parentTable = table;
					if (tableDef.getAlias() != null) {
						table.setTableAlias(tableDef.getAlias());
					}
					// fall through
				case ParentExtension:
					parentPlusExtTables.add(table);
					break;
				case Child:
					childTable = table;
					if (tableDef.getAlias() != null) {
						table.setTableAlias(tableDef.getAlias());
					}
					// fall through
				case ChildExtension:
					childPlusExtTables.add(table);
					break;
				case Join: // unlikely to be in the select columns, but just in
							// case
					joinTable = table;
					joinList = new ArrayList<TableMetaData>(1);
					joinList.add(joinTable);
					break;
				default: // Unknown
				}
			}

			// Add column to the table
			table.addColumn(column);
			column.setTableRole(table.getTableRole());

			// Add column to special column lists
			allReadColumns.add(column);
			switch (table.getTableRole()) {
			case Parent:
			case ParentExtension:
				parentReadColumns.add(column);
				break;
			case Child:
			case ChildExtension:
				childReadColumns.add(column);
				break;
			default: // Unknown
			}
		}

		// Determine number of databases
		multipleDatabases = databases.size() > 1;
	}

	private List<String> getQualifiedColumnNames(
			final List<ColumnMetaData> columns) {
		if (columns != null) {
			final List<String> names = new ArrayList<String>(columns.size());
			for (final ColumnMetaData column : columns) {
				names.add(column.getQualifiedColumnName());
			}
			return names;
		} else {
			return null;
		}
	}

	private String getQualifiedTableName(final TableMetaData table) {
		if (table != null) {
			return table.getQualifiedTableName();
		} else {
			return null;
		}
	}

	private List<String> getQualifiedTableNames(final List<TableMetaData> tables) {
		if (tables != null) {
			final List<String> names = new ArrayList<String>(tables.size());
			for (final TableMetaData table : tables) {
				names.add(table.getQualifiedTableName());
			}
			return names;
		} else {
			return null;
		}
	}

	// add by hjc
	@XmlTransient
	protected JdbcTemplate jdbcTemplate;

	@Override
	public JdbcOperations getJdbcOperations() {
		return this.jdbcTemplate;
	}
}