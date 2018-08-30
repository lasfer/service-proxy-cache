package org.flas.soap.proxy.filters;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.flas.soap.proxy.GlobalConstants;
import org.flas.soap.proxy.config.CacheConfig;
import org.flas.soap.proxy.config.Help;
import org.flas.soap.proxy.services.CacheUtilService;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.ribbon.proxy.annotation.Http.HttpMethod;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PreCacheFilter extends ZuulFilter {
	private static final Logger LOGGER = Logger.getLogger(PreCacheFilter.class);

	private enum Actions {
		SAVE, CLEAN, ENABLE_FILES, DISABLE_FILES, SET, OFFLINE, ONLINE, KEY, HELP
	}

	public static final String WSDL_SUFIX = ".wsdl_GET";

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
		String cacheKey;
		String document = writer.toString();
		// get url ending (after last /)
		String[] parts1 = ctx.getRequest().getRequestURI().split("/");
		// get url ending (after last . if exist)
		String[] parts2 = parts1[parts1.length - 1].split("\\.");
		String service = parts2[parts2.length - 1];
		boolean get = HttpMethod.GET.name().equals(ctx.getRequest().getMethod());
		if (get) {
			cacheKey = service + WSDL_SUFIX;
		} else {
			cacheKey = cache.generateHash(service, document, true);
		}

		if (Actions.CLEAN.name().equals(document)) {
			cache.clean();
			ctx.setSendZuulResponse(false);
		} else if (document != null && document.startsWith(Actions.KEY.name())) {
			String[] parts = document.split(":::");
			try {
				cacheKey = cache.generateHash(service, parts[1], true);
				ctx.getResponse().getOutputStream().write(("key:    " + cacheKey).getBytes());
			} catch (Exception e) {
				LOGGER.error("Error writting KEY: " + cacheKey, e);
			}
			ctx.setSendZuulResponse(false);
		} else if (Actions.HELP.name().equals(document)) {
			try {
				ctx.getResponse().getOutputStream().write(Help.TEXT.getBytes());
			} catch (Exception e) {
				LOGGER.error("Error writting KEY: " + cacheKey, e);
			}
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
		} else if (Actions.OFFLINE.name().equals(document)) {
			// OFFLINE needs filecaching enabled
			cacheConfig.setFileCaching(true);
			cacheConfig.setOffline(true);
			ctx.setSendZuulResponse(false);
		} else if (Actions.ONLINE.name().equals(document)) {
			cacheConfig.setOffline(false);
			ctx.setSendZuulResponse(false);
		} else if (document != null && document.startsWith(Actions.SET.name())) {
			try {
				String[] parts = document.split(":::");
				cacheKey = cache.generateHash(service, parts[1], true);
				cache.put(cacheKey, parts[2]);
				ctx.getResponse().getOutputStream().write(("SAVED:" + cacheKey).getBytes());
			} catch (Exception e) {
				LOGGER.error("Error setting mock response: " + cacheKey, e);
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
					LOGGER.error("error writing response from cache (old)", e);
				}

				ctx.setSendZuulResponse(false);
			} else if (cacheConfig.getOffline() && !get) {
				try {
					String response = cache.getAnyFileContentSameService(service);
					ctx.getResponse().getOutputStream().write(response.getBytes());
					LOGGER.info("RANDOM FROM CACHE: \n" + response);
					LOGGER.info("CACHE KEY(THERE IS NO FILE, RANDOM RESPONSE):" + cacheKey);
				} catch (IOException e) {
					LOGGER.error("error writing response from cache (random)", e);
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