package geotools;

import static org.junit.Assert.*;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.restsql.core.impl.SqlUtils;

public class TestFilterToSql {

	FilterToSQL filterToSQL;

	@Before
	public void setUp() throws Exception {
		filterToSQL = new FilterToSQL();
	}

	@After
	public void tearDown() throws Exception {
		filterToSQL = null;
	}

	@Test
	public void test() throws CQLException, FilterToSQLException {
		Filter filter = ECQL.toFilter("ATTR1<10 AND ATTR2<2 OR ATTR3>10");
		String result = SqlUtils.buildSQLFilterClause("ATTR1<+10+AND ATTR2<2 OR ATTR3>10");
		System.out.println(result);
		fail("Not yet implemented");
	}

}
