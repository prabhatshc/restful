package org.restsql.core;

public class Config {

	public static final String MYSQL_RESOURCE_METADATA = "mysql";

	public static final String POSTGRESQL_RESOURCE_METADATA = "postgresql";

	private String sqlResourcesDir;

	private String databaseType = "postgresql";

	public Config() {

	}

	public String getSqlResourcesDir() {
		return sqlResourcesDir;
	}

	public void setSqlResourcesDir(String sqlResourcesDir) {
		this.sqlResourcesDir = sqlResourcesDir;
	}

	public String getDatabaseType() {
		return databaseType;
	}

	public void setDatabaseType(String databaseType) {
		this.databaseType = databaseType;
	}

}
