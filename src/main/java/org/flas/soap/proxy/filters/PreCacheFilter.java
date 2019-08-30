package org.flas.soap.proxy.filters;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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
		SAVE, CLEAN, ENABLE_FILES, DISABLE_FILES, SET, OFFLINE, ONLINE, KEY, HELP, ENABLE_CACHE, DISABLE_CACHE
	}

	public static final String WSDL_SUFIX = ".%s_GET";

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
		
		String keyPrefix=getKeyPrefix(document, ctx.getRequest().getRequestURI());
		
		boolean get = HttpMethod.GET.name().equals(ctx.getRequest().getMethod());
		String cacheKey=StringUtils.EMPTY;

		if (Actions.CLEAN.name().equals(document)) {
			cache.clean();
			ctx.setSendZuulResponse(false);
			writeCurrentConfig(ctx);
		} else if (document != null && document.startsWith(Actions.KEY.name())) {
			String[] parts = document.split(":::");
			try {
				cacheKey = cache.generateHash(keyPrefix, parts[1], true);
				ctx.getResponse().getOutputStream().write(("key:    " + cacheKey).getBytes());
			} catch (Exception e) {
				LOGGER.error("Error writting KEY: " + cacheKey, e);
			}
			ctx.setSendZuulResponse(false);
		} else if (Actions.HELP.name().equals(document)) {
			try {
				ctx.getResponse().getOutputStream().write(Help.TEXT.getBytes());
			} catch (Exception e) {
				LOGGER.error("Error writting HELP", e);
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
			writeCurrentConfig(ctx);
		} else if (Actions.ENABLE_CACHE.name().equals(document)) {
			cacheConfig.setEnabled(true);
			ctx.setSendZuulResponse(false);
			writeCurrentConfig(ctx);
		} else if (Actions.DISABLE_CACHE.name().equals(document)) {
			cache.store();
			cache.clean();
			cacheConfig.setEnabled(false);
			ctx.setSendZuulResponse(false);
			writeCurrentConfig(ctx);
		} else if (Actions.OFFLINE.name().equals(document)) {
			// OFFLINE needs filecaching enabled
			cacheConfig.setFileCaching(true);
			cacheConfig.setOffline(true);
			ctx.setSendZuulResponse(false);
			writeCurrentConfig(ctx);
		} else if (Actions.ONLINE.name().equals(document)) {
			cacheConfig.setOffline(false);
			ctx.setSendZuulResponse(false);
			writeCurrentConfig(ctx);
		} else if (document != null && document.startsWith(Actions.SET.name())) {
			try {
				String[] parts = document.split(":::");
				cacheKey = cache.generateHash(keyPrefix, parts[1], true);
				cache.put(cacheKey, parts[2]);
				ctx.getResponse().getOutputStream().write(("SAVED:" + cacheKey).getBytes());
			} catch (Exception e) {
				LOGGER.error("Error setting mock response: " + cacheKey, e);
			}
			ctx.setSendZuulResponse(false);
		} else {
			if (get) {
				String param=ctx.getRequest().getParameter("wsdl")!=null?"wsdl":"";
				
				cacheKey = keyPrefix + String.format(WSDL_SUFIX,param );
			} else {
				cacheKey = cache.generateHash(keyPrefix, document, true);
			}
			LOGGER.info("REQUEST: \n" + document);
			ctx.set(GlobalConstants.CONTEXT_CACHE_KEY, cacheKey);
			if (cache.contains(cacheKey) && cacheConfig.getEnabled()) {
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
					String response = cache.getAnyFileContentSameService(keyPrefix);
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
	
	/**
	 * Returns key prefix
	 * based on pattern if it is defined
	 * or based on url service
	 * 
	 * @param document
	 * @param requestURI
	 * @return
	 */
	private String getKeyPrefix(String document, String requestURI) {
		String keyPrefix;
		if (StringUtils.isNotBlank(cacheConfig.getKeyPrefix())) {
			keyPrefix = getRequestMethodName(document);
		} else {
			// get url ending (after last /)
			String[] parts1 = requestURI.split("/");
			// get url ending (after last . if exist)
			String[] parts2 = parts1[parts1.length - 1].split("\\.");
			keyPrefix = parts2[parts2.length - 1];
		}
		return keyPrefix;
	}

	public boolean shouldFilter() {
		return true;
	}
	/**
	 * Given a request, takes the soap method name
	 * 
	 * @param document
	 * @return
	 */
	public String getRequestMethodName(String document) {
		Pattern p = Pattern.compile(cacheConfig.getKeyPrefix(), Pattern.DOTALL);
		Matcher m = p.matcher(document);
		String method = StringUtils.EMPTY;
		while (m.find()) {
			method = m.group(2);
		}
		return method;
	}
	
	/**
	 * Returns current configuration in response
	 * @param ctx
	 * @return
	 */
	public void writeCurrentConfig(RequestContext ctx) {
		try {
			ctx.getResponse().getOutputStream()
					.write(ToStringBuilder.reflectionToString(cacheConfig, ToStringStyle.MULTI_LINE_STYLE).getBytes());
		} catch (Exception e) {
			LOGGER.error("error writing configurations to response", e);
		}
	}
}