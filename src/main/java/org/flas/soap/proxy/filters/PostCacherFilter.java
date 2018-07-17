package org.flas.soap.proxy.filters;

import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.flas.soap.proxy.GlobalConstants;
import org.flas.soap.proxy.services.CacheUtilService;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.google.common.io.CharStreams;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * 
 * @author fernando.las@gmail.com
 * 
 *
 */
public class PostCacherFilter extends ZuulFilter {
	private static final Logger LOGGER = Logger.getLogger(PostCacherFilter.class);

	@Autowired
	private CacheUtilService cache;

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 10;
	}

	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (ctx.getResponseStatusCode() == HttpStatus.OK.value()) {
			String key = (String) ctx.get(GlobalConstants.CONTEXT_CACHE_KEY);
			try {
				if (!cache.contains(key) && ctx.getResponseDataStream() != null) {
					cache.put(key, CharStreams.toString(
							new InputStreamReader(new GZIPInputStream(ctx.getResponseDataStream()), "UTF-8")));					
					String response = cache.get(key);
					ctx.setResponseBody(response);
					LOGGER.info("REAL RESPONSE: \n" + response);
					LOGGER.info("CACHE KEY:" + key);
				}
			} catch (Exception e) {
				LOGGER.error("ERROR AL GUARDAR EL RESPONSE REAL: \n", e);
			}
		}
		return null;
	}

	public boolean shouldFilter() {
		return true;
	}
}