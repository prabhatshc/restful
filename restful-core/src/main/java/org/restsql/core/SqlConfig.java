package org.restsql.core;

public class SqlConfig {

	public static final String MYSQL_RESOURCE_METADATA = "mysql";

	public static final String POSTGRESQL_RESOURCE_METADATA = "postgresql";

	public static final String DATETIME_FORMAT = "yyyyMMddHHmm";

	private String sqlResourcesDir = "/resource";

	private String databaseType = "postgresql";

	private Integer limit = 1000;

	private Integer maxCacheSize = 1000;

	private Long expireAfterWrite = 3600L;

	public SqlConfig() {

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

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getMaxCacheSize() {
		return maxCacheSize;
	}

	public void setMaxCacheSize(Integer maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	public Long getExpireAfterWrite() {
		return expireAfterWrite;
	}

	public void setExpireAfterWrite(Long expireAfterWrite) {
		this.expireAfterWrite = expireAfterWrite;
	}

}
