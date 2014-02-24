package mq.restful.web.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mq.restful.web.config.RestConfig;

import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQLException;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.RequestSQLParams;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceFactory;
import org.restsql.core.SqlResourceFactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Controller
@RequestMapping(value = "/res")
public class ResResource {

	@Autowired
	private SqlResourceFactory sqlResourceFactory;
	@Autowired
	private RestConfig restConfig;

	private Cache<RequestSQLParams, Object> cache;

	final Logger logger = LoggerFactory.getLogger(ResResource.class);

	@RequestMapping(value = "/{resName}", method = RequestMethod.GET)
	public void get(@PathVariable final String resName,
			@RequestParam(value = "_filter", required = false) String filter,
			@RequestParam(value = "_orderby", required = false) String orderby,
			@RequestParam(value = "_limit", required = false) Integer limit,
			@RequestParam(value = "_offset", required = false) Integer offset,
			@RequestParam(value = "_output", required = false) String output,
			HttpServletRequest request, HttpServletResponse response) {

		PrintWriter writer = null;

		try {

			response.setCharacterEncoding(restConfig.getCharset());
			response.setContentType(restConfig.getContentType(output));
			writer = response.getWriter();

			SqlResource sqlResource = sqlResourceFactory
					.getSqlResource(resName);

			if (null == sqlResource) {
				writer.print("[Error]请检查Resource Name,该Resource不存在.");
			} else {

				RequestSQLParams requestSQLParams = genRequestSQLParams(
						resName, filter, orderby, limit, offset);

				Object result = getCache().getIfPresent(requestSQLParams);

				if (null == result) {

					try {
						result = sqlResource.read(requestSQLParams);
					} catch (DataAccessException | CQLException
							| FilterToSQLException e) {
						writer.print(genThrowableMessage(e));
						logger.error(e.getMessage());
					}

					if (null != result) {
						cache.put(requestSQLParams, result);
					}
				}

				ObjectMapper mapper = restConfig.getObjectMapper(output);

				if (mapper instanceof CsvMapper) {
					if (!sqlResource.getMetaData().isHierarchical()) {
						Builder builder = CsvSchema.builder();
						for (ColumnMetaData meta : sqlResource.getMetaData()
								.getParentReadColumns()) {
							builder.addColumn(meta.getColumnLabel());
						}
						CsvSchema schema = builder
								.build()
								.withLineSeparator(
										restConfig.getCsvLineSeparator())
								.withColumnSeparator(
										restConfig.getCsvColumnSeparator()
												.trim().charAt(0));
						((CsvMapper) mapper).writer(schema).writeValue(writer,
								result);
					} else {
						writer.print("[Error]CsvMapper不支持一对多关系");
					}
				} else {
					mapper.writeValue(writer, result);
				}

			}

		} catch (SqlResourceFactoryException e) {

			// logger.error(e.getMessage());
			writer.print(genThrowableMessage(e));
			logger.error(e.getMessage());
			// e.printStackTrace();
		} catch (SqlResourceException e) {
			// logger.error(e.getMessage());
			writer.print(genThrowableMessage(e));
			logger.error(e.getMessage());
			// e.printStackTrace();
		} catch (IOException e) {
			logger.error("[Error]" + e.getMessage());
			logger.error(e.getMessage());
			// e.printStackTrace();
		} finally {
			if (null != writer) {
				writer.flush();
				writer.close();
			}
		}

	}

	@RequestMapping(value = "/CLEARCACHE", method = RequestMethod.GET)
	public void clearCache() {
		getCache().cleanUp();
	}

	private String genThrowableMessage(Throwable t) {
		return "[Error]" + t.getMessage();
	}

	private RequestSQLParams genRequestSQLParams(String resName, String filter,
			String orderby, Integer limit, Integer offset) {

		RequestSQLParams params = new RequestSQLParams();
		params.setResName(resName);

		params.setFilter(filter);
		params.setOrderby(orderby);

		if (null != limit) {
			params.setLimit(limit);
		} else {
			params.setLimit(sqlResourceFactory.getSqlConfig().getLimit());
		}

		if (null != offset) {
			params.setOffset(offset);
		}

		return params;
	}

	private Cache<RequestSQLParams, Object> getCache() {
		if (null == cache)
			cache = CacheBuilder
					.newBuilder()
					.maximumSize(restConfig.getMaxCacheSize())
					.expireAfterWrite(restConfig.getExpireAfterWrite(),
							TimeUnit.SECONDS).build();
		return cache;
	}

}
