package geotools;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestStringBuffer {

	StringBuffer buffer;

	@Before
	public void setUp() throws Exception {
		buffer = new StringBuffer();
	}

	@After
	public void tearDown() throws Exception {
		buffer = null;
	}

	@Test
	public void test() {
		buffer.append("where");

		assertTrue(buffer.indexOf("where") == 1);

	}

}
