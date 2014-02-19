package org.restsql.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.restsql.core.Config;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceFactory;
import org.restsql.core.SqlResourceFactoryException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.impl.mysql.MySqlSqlResourceMetaData;
import org.restsql.core.impl.postgresql.PostgreSqlSqlResourceMetaData;
import org.restsql.core.sqlresource.ObjectFactory;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("sqlResourceFactory")
public class SqlResourceFactoryImpl implements SqlResourceFactory {

	private Map<String, SqlResource> sqlResources = new HashMap<String, SqlResource>();

	private Config config;

	private DataSource dataSource;

	Logger logger = LoggerFactory.getLogger("SqlResourceFactory");

	public SqlResourceFactoryImpl() {

	}

	@Autowired
	public void setConfig(Config config) {
		this.config = config;
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SqlResource getSqlResource(String resName)
			throws SqlResourceFactoryException, SqlResourceException {
		SqlResource sqlResource = sqlResources.get(resName);
		if (sqlResource == null) {
			final InputStream inputStream = getInputStream(resName);
			JAXBContext context;
			try {
				context = JAXBContext.newInstance(ObjectFactory.class);
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				unmarshaller.setSchema(null);
				final SqlResourceDefinition definition = ((JAXBElement<SqlResourceDefinition>) unmarshaller
						.unmarshal(inputStream)).getValue();

				SqlResourceMetaData sqlResourceMetaData;
				if (config.getDatabaseType().equalsIgnoreCase(
						Config.POSTGRESQL_RESOURCE_METADATA)) {
					sqlResourceMetaData = new PostgreSqlSqlResourceMetaData();
				} else {
					sqlResourceMetaData = new MySqlSqlResourceMetaData();
				}

				sqlResourceMetaData.init(resName, definition, dataSource);

				sqlResource = new SqlResourceImpl(resName, definition,
						sqlResourceMetaData);
				sqlResources.put(resName, sqlResource);
			} catch (final JAXBException exception) {

				StringBuffer bufferE = new StringBuffer(
						"Error unmarshalling SQL Resource ")
						.append(getSqlResourceFileName(resName)).append(" -- ")
						.append(exception.getMessage());
				logger.error(bufferE.toString());
				throw new SqlResourceFactoryException(bufferE.toString());
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (Throwable t) {
					}
				}
			}
		}
		return sqlResource;
	}

	@Override
	public InputStream getSqlResourceDefinition(String resName)
			throws SqlResourceFactoryException {
		return getInputStream(resName);
	}

	@Override
	public List<String> getSqlResourceNames()
			throws SqlResourceFactoryException {
		return getSqlResourceNames(getSqlResourcesDir());
	}

	// Package methods

	/**
	 * Returns available SQL Resource names using the provided directory. Used
	 * by testing infrastructure.
	 * 
	 * @throws SqlResourceFactoryException
	 *             if the provided directory does not exist
	 */
	List<String> getSqlResourceNames(final String dirName)
			throws SqlResourceFactoryException {
		final List<String> resNames = new ArrayList<String>();
		getSqlResourceNames(resNames, dirName, "");
		if (resNames.size() == 0) {
			logger.warn("No SQL Resource definitions found in {}", dirName);
		}
		return resNames;
	}

	@Override
	public String getSqlResourcesDir() {
		return config.getSqlResourcesDir();
	}

	@Override
	public void reloadSqlResource(String resName)
			throws SqlResourceFactoryException, SqlResourceException {
		sqlResources.remove(resName);
		getSqlResource(resName);

	}

	@Override
	public boolean isSqlResourceLoaded(String name) {
		return sqlResources.containsKey(name);
	}

	// Private utils

	/** Opens input stream to resource name. Callers must close stream. */
	@SuppressWarnings("resource")
	private InputStream getInputStream(final String resName)
			throws SqlResourceFactoryException {
		final String fileName = getSqlResourceFileName(resName);
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(fileName);
		} catch (final FileNotFoundException exception) {
			inputStream = this.getClass().getResourceAsStream(fileName);
		}
		if (inputStream == null) {
			throw new SqlResourceFactoryException("SQL Resource " + resName
					+ " not found - expected " + fileName);
		}
		return inputStream;
	}

	private String getSqlResourceFileName(final String resName) {
		final StringBuilder fileName = new StringBuilder(128);
		fileName.append(getSqlResourcesDir());
		final StringTokenizer tokenizer = new StringTokenizer(resName, ".");
		while (tokenizer.hasMoreTokens()) {
			fileName.append("/");
			fileName.append(tokenizer.nextToken());
		}
		fileName.append(".xml");
		return fileName.toString();
	}

	/**
	 * Scans for xml files and recursively descends subdirs.
	 * 
	 * @throws SqlResourceFactoryException
	 *             if the provided directory does not exist
	 */
	private void getSqlResourceNames(final List<String> resNames,
			final String dirName, final String packageName)
			throws SqlResourceFactoryException {
		final File dir = new File(dirName);
		if (dir.exists()) {
			logger.info("listing files for {}", dirName);
			for (final File file : dir.listFiles()) {
				if (file.isFile()) {
					final int extIndex = file.getName().indexOf(".xml");
					if (extIndex > 0) {
						resNames.add(packageName
								+ file.getName().substring(0, extIndex));
					}
				}
			}
			for (final File subDir : dir.listFiles()) {
				if (subDir.isDirectory()) {
					final String subPackageName = packageName.length() == 0 ? subDir
							.getName() + "."
							: packageName + subDir.getName() + ".";
					getSqlResourceNames(resNames, subDir.getAbsolutePath(),
							subPackageName);
				}
			}
		} else {
			final String message = "SQL Resources directory " + dirName
					+ " does not exist";
			logger.error(message);
			throw new SqlResourceFactoryException(message);
		}
	}

}
