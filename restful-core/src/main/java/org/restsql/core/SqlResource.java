/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQLException;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.springframework.dao.DataAccessException;

/**
 * Represents an SQL Resource, a queryable and updatable database "view".
 * 
 * @author Mark Sawers
 */
public interface SqlResource {
	/**
	 * Returns SQL resource information defined by the user, including query, validated attributes and trigger.
	 * 
	 * @return definition
	 */
	public SqlResourceDefinition getDefinition();

	/**
	 * Returns SQL resource name.
	 * 
	 * @return SQL resource name
	 */
	public String getName();

	/**
	 * Returns meta data for SQL resource.
	 * 
	 * @return SQL rsource meta data
	 */
	public SqlResourceMetaData getMetaData();
	
	

	/**
	 * @param RequestSQLParams
	 * 
	 * Returns sql.
	 * 
	 * @return String
	 */
	public String buildSQL(RequestSQLParams params) throws CQLException, FilterToSQLException;
	
	/**
	 * @param RequestSQLParams
	 * 
	 * Returns JsonNode.
	 * 
	 * @return JsonNode
	 * @throws FilterToSQLException 
	 * @throws DataAccessException 
	 * @throws CQLException 
	 */
	public Object read(RequestSQLParams params) throws CQLException, DataAccessException, FilterToSQLException ;
	
	public SqlResourceFactory getSqlResourceFactory();


	
}