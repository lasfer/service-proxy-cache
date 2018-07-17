package org.flas.soap.proxy.filters;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.flas.soap.proxy.GlobalConstants;
import org.flas.soap.proxy.config.CacheConfig;
import org.flas.soap.proxy.services.CacheUtilService;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PreCacheFilter extends ZuulFilter {
	private static final Logger LOGGER = Logger.getLogger(PreCacheFilter.class);

	private enum Actions {
		SAVE, CLEAN, ENABLE_FILES, DISABLE_FILES, SET
	}

	@Autowired
	private CacheUtilService cache;
	@Autowired
	private CacheConfig cacheConfig;

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 10;
	}

	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(ctx.getRequest().getInputStream(), writer, "UTF-8");
		} catch (IOException e) {
			LOGGER.error("error al copiar el contenido del request", e);
		}
		String document = writer.toString();
		String cacheKey = cache.generateHash(ctx.getRequest().getRequestURI(), document);

		if (Actions.CLEAN.name().equals(document)) {
			cache.clean();
			ctx.setSendZuulResponse(false);
		} else if (Actions.SAVE.name().equals(document)) {
			cache.store();
			ctx.setSendZuulResponse(false);
		} else if (Actions.ENABLE_FILES.name().equals(document)) {
			cacheConfig.setFileCaching(true);
			ctx.setSendZuulResponse(false);
		} else if (Actions.DISABLE_FILES.name().equals(document)) {
			cacheConfig.setFileCaching(false);
			ctx.setSendZuulResponse(false);
		} else if (document!=null && document.startsWith(Actions.SET.name())) {
			try {
			String [] parts=document.split(":::");
			String cacheKey1 = cache.generateHash(ctx.getRequest().getRequestURI(), parts[1]);
			cache.put(cacheKey1, parts[2]);
			ctx.getResponse().getOutputStream().write(("SAVED:"+  cacheKey).getBytes());
			}catch(Exception e) {
				LOGGER.error("Error setting mock response: " + cacheKey ,e);
			}
			
			ctx.setSendZuulResponse(false);
		} else {

			LOGGER.info("REQUEST: \n" + document);
			ctx.set(GlobalConstants.CONTEXT_CACHE_KEY, cacheKey);
			if (cache.contains(cacheKey)) {

				try {
					ctx.getResponse().getOutputStream().write(cache.get(cacheKey).getBytes());
					LOGGER.info("FROM CACHE: \n" + cache.get(cacheKey));
					LOGGER.info("CACHE KEY:" + cacheKey);
				} catch (IOException e) {
					LOGGER.error("error al escribir el response desde el cache", e);
				}

				ctx.setSendZuulResponse(false);
			}
		}
		return null;
	}

	public boolean shouldFilter() {
		return true;
	}
}