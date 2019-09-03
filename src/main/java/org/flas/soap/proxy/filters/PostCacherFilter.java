package org.flas.soap.proxy.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.flas.soap.proxy.GlobalConstants;
import org.flas.soap.proxy.config.CacheConfig;
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
	
	@Autowired
	private CacheConfig cacheConfig;

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
		String context=(String)((Map<String, Object>)ctx.get("zuulRequestHeaders")).get("x-forwarded-prefix");
		String key = (String) ctx.get(GlobalConstants.CONTEXT_CACHE_KEY);
		if (ctx.getResponseStatusCode() == HttpStatus.OK.value()) {
			
			try {
				if (!cache.contains(key) && ctx.getResponseDataStream() != null) {
					InputStream inputStream = ctx.getResponseGZipped()
							? new GZIPInputStream(ctx.getResponseDataStream())
							: ctx.getResponseDataStream();
					String response = CharStreams.toString(new InputStreamReader(inputStream, "UTF-8"));
					boolean isCacheableResponse = cacheConfig.getEnabled() && 
							(cache.isCacheableResponse(response) || key.endsWith(PreCacheFilter.WSDL_SUFIX));
					if (isCacheableResponse) {
						cache.put(key, response);
					}
					ctx.setResponseBody(response);
					LOGGER.info("Was Cached: " + isCacheableResponse + "\n REAL RESPONSE: \n" + response);
					if (isCacheableResponse)
						LOGGER.info("CACHED - CACHE KEY: " + key);
				}
			} catch (Exception e) {
				LOGGER.error("ERROR SAVING/RETURNING REAL RESPONSE: \n", e);
			}
		} else if ("/webservices".equals(context) && !PreCacheFilter.Actions.SET.name().equals((String)ctx.get(GlobalConstants.CONTEXT_CACHE_OPERATION))) {
			String keyPrefix = (String) ctx.get(GlobalConstants.CONTEXT_CACHE_KEY_PREFIX);
			String response = cache.getAnyFileContentSameService(keyPrefix);
			ctx.setResponseBody(response);
			cache.put(key, response);
			LOGGER.info("RANDOM FROM CACHE: \n" + response);
			LOGGER.info("CACHE KEY(THERE IS NO FILE, RANDOM RESPONSE):" + key);
		}
		return null;
	}

	public boolean shouldFilter() {
		return true;
	}
}