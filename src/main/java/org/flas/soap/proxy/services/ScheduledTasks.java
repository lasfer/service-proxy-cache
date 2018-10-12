package org.flas.soap.proxy.services;

import org.apache.log4j.Logger;
import org.flas.soap.proxy.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

	@Autowired
	private CacheUtilService cache;
	@Autowired
	private CacheConfig cacheConfig;

	private static final Logger LOGGER = Logger.getLogger(ScheduledTasks.class);

	/**
	 * Every 30 minutes store all cache entries on File System
	 */
	@Scheduled(fixedRate = 60000 * 30)
	public void autoStoreInFilesDaemon() {
		if (cacheConfig.getAutoStoreInFilesDaemon()) {
			LOGGER.info("Autostoring on FS cache entries: " + cache.size());
			cache.store();
			LOGGER.info(cache.size() + " cache entries astored on FS succesfully.");
		}
	}
}