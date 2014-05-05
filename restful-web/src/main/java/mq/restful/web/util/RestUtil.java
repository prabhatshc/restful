package mq.restful.web.util;

import java.util.HashMap;
import java.util.Map;

public class RestUtil {

	private static Map<String, String> contentTypeMap;

	public static String getContentType(String key, String charset) {
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
			ct = contentTypeMap.get("json");
		}

		return ct;
	}

}
