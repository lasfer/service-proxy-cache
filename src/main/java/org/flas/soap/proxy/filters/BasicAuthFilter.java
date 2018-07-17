package org.flas.soap.proxy.filters;

import java.util.Base64;

import org.flas.soap.proxy.config.BasicAuthConfig;
import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * 
 * @author fernando.las@gmail.com
 *
 */
public class BasicAuthFilter extends ZuulFilter {

	@Autowired
	private BasicAuthConfig basicAuthConfig;

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
		ctx.addZuulRequestHeader("Authorization", "Basic " + Base64.getEncoder()
				.encodeToString((basicAuthConfig.getUsername() + ":" + basicAuthConfig.getPassword()).getBytes()));
		return null;
	}

	public boolean shouldFilter() {
		return basicAuthConfig.getEnabled();
	}
}