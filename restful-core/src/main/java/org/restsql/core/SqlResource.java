/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import org.restsql.core.sqlresource.SqlResourceDefinition;

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


	
}