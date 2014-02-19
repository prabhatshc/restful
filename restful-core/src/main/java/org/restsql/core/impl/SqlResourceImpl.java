package org.restsql.core.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQLException;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.RequestSQLParams;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.SqlStruct;
import org.restsql.core.TableMetaData.TableRole;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SqlResourceImpl implements SqlResource {

	private final SqlResourceDefinition definition;
	private final SqlResourceMetaData metaData;
	private final String name;
	private Cache<RequestSQLParams, String> cache;
	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory
			.getLogger(SqlResourceImpl.class);

	public SqlResourceImpl(final String name,
			final SqlResourceDefinition definition,
			final SqlResourceMetaData metaData) {
		this.name = name;
		this.definition = definition;
		definition.getQuery().setValue(
				SqlUtils.removeWhitespaceFromSql(definition.getQuery()
						.getValue()));
		this.metaData = metaData;

		cache = CacheBuilder.newBuilder().maximumSize(1000)
				.expireAfterWrite(60, TimeUnit.MINUTES).build();

		objectMapper = new ObjectMapper();

	}

	@Override
	public SqlResourceDefinition getDefinition() {
		return this.definition;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public SqlResourceMetaData getMetaData() {
		return this.metaData;
	}

	@Override
	public String buildSQL(RequestSQLParams params) throws CQLException,
			FilterToSQLException {

		String sql = cache.getIfPresent(params);
		if (null == sql) {
			SqlStruct struct = new SqlStruct(this.definition.getQuery()
					.getValue(), SqlUtils.buildSQLFilterClause(params
					.getFilter()));
			if (null != params.getLimit())
				struct.setLimit(params.getLimit());

			if (null != params.getOffset())
				struct.setOffset(params.getOffset());

			struct.setOrderByClause(SqlUtils.buildSQLOrderByClause(params
					.getOrderby()));

			sql = SqlUtils.removeWhitespaceFromSql(struct.getStructSql());

			cache.put(params, sql);
		}

		return sql;
	}

	@Override
	public Object read(RequestSQLParams params) {

		try {
			String sql = buildSQL(params);
			logger.debug(sql);
			SqlRowSet resultSet = this.metaData.getJdbcOperations()
					.queryForRowSet(sql);

			if (metaData.isHierarchical()) {
				return buildReadResultsHierachicalCollection(resultSet);
			} else {

				return buildReadResultsFlatCollection(resultSet);

			}

		} catch (CQLException e) {

			logger.error(e.getMessage());
			return e.getMessage();
		} catch (FilterToSQLException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		}

	}

	private List<JsonNode> buildReadResultsFlatCollection(
			final SqlRowSet resultSet) throws SQLException {
		List<JsonNode> listNode = new ArrayList<JsonNode>();
		while (resultSet.next()) {

			ObjectNode objectNode = objectMapper.createObjectNode();

			for (final ColumnMetaData columnData : metaData.getAllReadColumns()) {
				if (!columnData.isNonqueriedForeignKey()) {
					addJsonNodeRow(objectNode, columnData, resultSet);
				}

			}

			listNode.add(objectNode);
		}

		return listNode;

	}

	private void addJsonNodeRow(ObjectNode objectNode,
			ColumnMetaData columnData, SqlRowSet resultSet) throws SQLException {

		String column = columnData.getColumnLabel();
		Object value = SqlUtils.getObjectByColumnNumber(columnData, resultSet);
		Format formatter = definition.getFormatter(column);
		if (value == null) {
			objectNode.putNull(column);
		} else if (value instanceof Integer) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((Integer) value));
			} else {
				objectNode.put(column, (Integer) value);
			}
		} else if (value instanceof String) {
			objectNode.put(column, (String) value);
		} else if (value instanceof Boolean) {
			objectNode.put(column, (Boolean) value);
		} else if (value instanceof Date) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((Date) value));
			} else {
				objectNode.put(column, ((Date) value).getTime());
			}
		} else if (value instanceof Long) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((Long) value));
			} else {
				objectNode.put(column, (Long) value);
			}
		} else if (value instanceof Double) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((Double) value));
			} else {
				objectNode.put(column, (Double) value);
			}
		} else if (value instanceof Float) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((Float) value));
			} else {
				objectNode.put(column, (Float) value);
			}
		} else if (value instanceof BigDecimal) {
			if (null != formatter) {
				objectNode.put(column, formatter.format((BigDecimal) value));
			} else {
				objectNode.put(column, (BigDecimal) value);
			}
		} else if (value instanceof Byte) {
			objectNode.put(column, (Byte) value);
		} else if (value instanceof byte[]) {
			objectNode.put(column, (byte[]) value);
		} else {
			logger.error("Unmappable object type: {}", value.getClass());
			throw new IllegalArgumentException("Unmappable object type: "
					+ value.getClass());
		}
	}

	private List<Map<String, Object>> buildReadResultsHierachicalCollection(
			SqlRowSet resultSet) throws SQLException {
		final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		final List<Object> currentParentPkValues = new ArrayList<Object>(
				metaData.getParent().getPrimaryKeys().size());

		boolean newParent = false;
		final int numberParentElementColumns = metaData.getParentReadColumns()
				.size();
		// final int numberChildElementColumns =
		// metaData.getChildReadColumns().size();
		final String childRowsName = getChildRowsName();
		Map<String, Object> parentRow = null;
		List<JsonNode> childRows = null;

		while (resultSet.next()) {
			// Assess state of parent
			if (currentParentPkValues.isEmpty()) {
				// First row
				newParent = true;
			} else {
				// Not the first row, check if parent differs from the last
				newParent = false;
				for (int i = 0; i < currentParentPkValues.size(); i++) {
					final ColumnMetaData column = metaData.getParent()
							.getPrimaryKeys().get(i);
					if (!currentParentPkValues.get(i).equals(
							SqlUtils.getObjectByColumnLabel(column, resultSet))) {
						newParent = true;
						break;
					}
				}
			}

			// Set current parent row pk values as well as in the parent row
			// object
			if (newParent) {
				childRows = new ArrayList<JsonNode>();
				parentRow = new HashMap<String, Object>(
						numberParentElementColumns);
				parentRow.put(childRowsName, childRows);
				results.add(parentRow);
				currentParentPkValues.clear();

				for (final ColumnMetaData column : metaData
						.getParentReadColumns()) {
					final Object value = SqlUtils.getObjectByColumnLabel(
							column, resultSet);
					if (column.isPrimaryKey()
							&& column.getTableRole() == TableRole.Parent) {
						currentParentPkValues.add(value);
					}
					parentRow.put(column.getColumnLabel(), value);
				}

			}

			// Populate the child row object
			ObjectNode childRow = objectMapper.createObjectNode();
			boolean nullPk = false;
			for (final ColumnMetaData column : metaData.getChildReadColumns()) {
				final Object value = SqlUtils.getObjectByColumnLabel(column,
						resultSet);
				if (column.isPrimaryKey()) {
					nullPk = value == null;
				}
				if (null == value) {
					childRow.putNull(column.getColumnLabel());
				} else {
					addJsonNodeRow(childRow, column, resultSet);
				}
			}

			if (nullPk) {
				childRow = null;
			} else {
				childRows.add(childRow);
			}

		}

		return results;

	}

	private String getChildRowsName() {
		return metaData.getChild().getTableAlias() + "s";
	}

}
