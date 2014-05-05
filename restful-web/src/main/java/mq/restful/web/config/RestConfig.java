package mq.restful.web.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class RestConfig {

	private Integer maxCacheSize = 1000;

	private Long expireAfterWrite = 3600L;

	private String charset = "UTF-8";

	private Map<String, String> contentTypeMap;

	private String defaultType = "json";

	private Map<String, ObjectMapper> objectMapperMap;

	private CsvConfig csvConfig;

	public RestConfig() {

	}

	public String getContentType(String key) {
		if (null == contentTypeMap) {
			contentTypeMap = new HashMap<String, String>();
			contentTypeMap.put("json", "text/json;charset=" + charset.trim());
			contentTypeMap.put("xml", "text/xml;charset=" + charset.trim());
			contentTypeMap.put("csv", "text/csv;charset=" + charset.trim());
		}
		String ct = null;
		if (null != key) {
			ct = contentTypeMap.get(key.trim().toLowerCase());
		}
		if (null == ct) {
			ct = contentTypeMap.get(defaultType);
		}

		return ct;
	}

	public ObjectMapper getObjectMapper(String key) {
		if (null == objectMapperMap) {
			objectMapperMap = new HashMap<String, ObjectMapper>();

			final XmlMapper xmlMapper = new XmlMapper();
			final CsvMapper csvMapper = new CsvMapper();
			final ObjectMapper jsonMapper = new ObjectMapper();
			objectMapperMap.put("json", jsonMapper);
			objectMapperMap.put("csv", csvMapper);
			objectMapperMap.put("xml", xmlMapper);
		}
		ObjectMapper mapper = null;
		if (null != key) {
			mapper = objectMapperMap.get(key.trim().toLowerCase());
		}
		if (null == mapper)
			mapper = objectMapperMap.get(defaultType);

		return mapper;
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

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public Map<String, String> getContentTypeMap() {
		return contentTypeMap;
	}

	public void setContentTypeMap(Map<String, String> contentTypeMap) {
		this.contentTypeMap = contentTypeMap;
	}

	public String getDefaultType() {
		return defaultType;
	}

	public void setDefaultType(String defaultType) {
		this.defaultType = defaultType.trim().toLowerCase();
	}

	public Map<String, ObjectMapper> getObjectMapperMap() {
		return objectMapperMap;
	}

	public void setObjectMapperMap(Map<String, ObjectMapper> objectMapperMap) {
		this.objectMapperMap = objectMapperMap;
	}

	public CsvConfig getCsvConfig() {
		return csvConfig;
	}

	public void setCsvConfig(CsvConfig csvConfig) {
		this.csvConfig = csvConfig;
	}

}
