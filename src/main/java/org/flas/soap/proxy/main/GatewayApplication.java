package org.flas.soap.proxy.main;

import org.flas.soap.proxy.filters.BasicAuthFilter;
import org.flas.soap.proxy.filters.PostCacherFilter;
import org.flas.soap.proxy.filters.PreCacheFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

@EnableZuulProxy
@SpringBootApplication(scanBasePackages = { "org.flas.soap.proxy" })
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public BasicAuthFilter simpleFilter() {
		return new BasicAuthFilter();
	}

	@Bean
	public PreCacheFilter simpleFilter1() {
		return new PreCacheFilter();
	}

	@Bean
	public PostCacherFilter simpleFilter2() {
		return new PostCacherFilter();
	}

}