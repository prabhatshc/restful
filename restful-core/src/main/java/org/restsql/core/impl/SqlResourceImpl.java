package org.restsql.core.impl;

import java.util.concurrent.TimeUnit;

import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQLException;
import org.restsql.core.RequestSQLParams;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.SqlStruct;
import org.restsql.core.sqlresource.SqlResourceDefinition;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SqlResourceImpl implements SqlResource {

	private final SqlResourceDefinition definition;
	private final SqlResourceMetaData metaData;
	private final String name;
	private Cache<RequestSQLParams, String> cache;

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

}
