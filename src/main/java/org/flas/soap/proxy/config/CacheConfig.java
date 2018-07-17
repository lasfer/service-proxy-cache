package org.flas.soap.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 
 * @author fernando.las@gmail.com
 *
 */
@ConfigurationProperties("cache.util")
@Component
public class CacheConfig {

	@Value("${cache.util.maxSize:100}")
	private long maxSize;
	@Value("${cache.util.elementsExpireTimeout:2}")
	private long elementsExpireTimeout;
	@Value("${cache.util.fileCaching:false}")
	private Boolean fileCaching;
	private String startTagForKey;
	private String endTagForKey;
	private String[] excludes;

	public long getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
	}

	public long getElementsExpireTimeout() {
		return elementsExpireTimeout;
	}

	public void setElementsExpireTimeout(long elementsExpireTimeout) {
		this.elementsExpireTimeout = elementsExpireTimeout;
	}

	public Boolean getFileCaching() {
		return fileCaching;
	}

	public void setFileCaching(Boolean fileCaching) {
		this.fileCaching = fileCaching;
	}

	public String getStartTagForKey() {
		return startTagForKey;
	}

	public void setStartTagForKey(String startTagForKey) {
		this.startTagForKey = startTagForKey;
	}

	public String getEndTagForKey() {
		return endTagForKey;
	}

	public void setEndTagForKey(String endTagForKey) {
		this.endTagForKey = endTagForKey;
	}

	public String[] getExcludes() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

}