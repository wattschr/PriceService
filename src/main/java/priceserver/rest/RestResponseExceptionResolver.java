package priceserver.rest;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RestResponseExceptionResolver extends DefaultHandlerExceptionResolver {
	public RestResponseExceptionResolver() {
		setOrder(Ordered.HIGHEST_PRECEDENCE);
	}

	@Override
	public ModelAndView resolveException(HttpServletRequest request,
	                                     HttpServletResponse response,
	                                     Object handler,
	                                     Exception ex) {
		logger.error("Got exception whilst processing request", ex);
		return super.resolveException(request, response, handler, ex);
	}

}
