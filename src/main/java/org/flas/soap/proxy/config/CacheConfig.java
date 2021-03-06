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
	@Value("${cache.util.offline:false}")
	private Boolean offline;
	@Value("${cache.util.dirPath:/tmp}")
	private String dirPath;
	private String startTagForKey;
	private String endTagForKey;
	private String keyPrefix;
	private String[] excludes;
	@Value("${cache.util.response.pattern.include}")
	private String responseIncludePattern;
	@Value("${cache.util.response.pattern.exclude}")
	private String responseExcludePattern;
	@Value("${cache.util.autoStoreInFilesDaemon:false}")
	private Boolean autoStoreInFilesDaemon;
	@Value("${cache.util.enabled:true}")
	private Boolean enabled;
	
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

	public Boolean getOffline() {
		return offline;
	}

	public void setOffline(Boolean offline) {
		this.offline = offline;
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

	public String getDirPath() {
		if (dirPath.endsWith("/"))
			return dirPath;
		else
			return dirPath + "/";
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	public String getResponseIncludePattern() {
		return responseIncludePattern;
	}

	public void setResponseIncludePattern(String responseIncludePattern) {
		this.responseIncludePattern = responseIncludePattern;
	}

	public String getResponseExcludePattern() {
		return responseExcludePattern;
	}

	public void setResponseExcludePattern(String responseExcludePattern) {
		this.responseExcludePattern = responseExcludePattern;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public Boolean getAutoStoreInFilesDaemon() {
		return autoStoreInFilesDaemon;
	}

	public void setAutoStoreInFilesDaemon(Boolean autoStoreInFilesDaemon) {
		this.autoStoreInFilesDaemon = autoStoreInFilesDaemon;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
