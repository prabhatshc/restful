package mq.restful.web.res;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.restsql.core.SqlResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/res")
public class ResResource {

	@Autowired
	private SqlResourceFactory sqlResourceFactory;

	Logger logger = LoggerFactory.getLogger(ResResource.class);

	@RequestMapping(value = "/{resName}", method = RequestMethod.GET)
	public void get(@PathVariable final String resName,
			HttpServletRequest request, HttpServletResponse response) {

	}
}
