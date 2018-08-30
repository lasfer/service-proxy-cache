package org.flas.soap.proxy.filters;

import java.io.InputStream;
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
					InputStream inputStream = ctx.getResponseGZipped()
							? new GZIPInputStream(ctx.getResponseDataStream())
							: ctx.getResponseDataStream();
					String response = CharStreams.toString(new InputStreamReader(inputStream, "UTF-8"));
					boolean isCacheableResponse = cache.isCacheableResponse(response);
					if (isCacheableResponse)
						cache.put(key, response);
					ctx.setResponseBody(response);
					LOGGER.info("Was Cached: " + isCacheableResponse + "\n REAL RESPONSE: \n" + response);
					if (isCacheableResponse)
						LOGGER.info("CACHED - CACHE KEY: " + key);
				}
			} catch (Exception e) {
				LOGGER.error("ERROR SAVING/RETURNING REAL RESPONSE: \n", e);
			}
		}
		return null;
	}

	public boolean shouldFilter() {
		return true;
	}
}