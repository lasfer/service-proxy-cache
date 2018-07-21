package org.flas.soap.proxy.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.flas.soap.proxy.config.CacheConfig;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 
 * @author fernando.las@gmail.com
 *
 */

@Service
public class CacheUtilService {
	private static final Logger LOGGER = Logger.getLogger(CacheUtilService.class);

	private Cache<String, String> cache;
	@Autowired
	private CacheConfig cacheConfig;

	@PostConstruct
	public void init() {
		cache = CacheBuilder.newBuilder().maximumSize(cacheConfig.getMaxSize())
				.expireAfterAccess(cacheConfig.getElementsExpireTimeout(), TimeUnit.MINUTES).build();
	}

	public void put(String key, String value) {
		cache.put(key, value);
		if (cacheConfig.getFileCaching()) {
			try {
				Files.write(Paths.get(getFileName(key)), value.getBytes());
			} catch (IOException e) {
				LOGGER.error("Error writing file for key: " + key, e);
			}
		}
	}

	public boolean contains(String key) {
		return cache.getIfPresent(key) != null || checkFile(key);
	}
	
	private boolean checkFile(String key) {
		return BooleanUtils.isTrue(cacheConfig.getFileCaching()) && fileExists(getFileName(key));
	}

	public String get(String key) {
		String value = cache.getIfPresent(key);
		if (value == null && checkFile(key)) {
			value = getFileContent(getFileName(key));
			cache.put(key, value);
		}
		return value;
	}

	public String generateHash(String service, String document) {

		String documentAux = null;
		String startTag = cacheConfig.getStartTagForKey();
		String endTag = cacheConfig.getEndTagForKey();
		String []excludes = cacheConfig.getExcludes();

		Pattern p = Pattern.compile(startTag + "(.*?)" + endTag, Pattern.DOTALL);
		Matcher m = p.matcher(document);
		while (m.find()) {
			documentAux = (m.group(1));
		}
		if (documentAux == null) {
			documentAux = document;
		}
		for (String exclude : excludes) {
			documentAux = documentAux.replaceAll(exclude + "?.*?" + exclude, "");
		}
		return service + "." + documentAux.hashCode();
	}

	public void clean() {
		cache.cleanUp();
		cache.invalidateAll();
	}

	public void store() {
		Map<String, String> map = cache.asMap();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			try {
				Files.write(Paths.get(getFileName(entry.getKey())), entry.getValue().getBytes());
			} catch (Exception e) {
				LOGGER.error("Error writing file for key: " + entry.getKey(), e);
			}
		}
	}

	private String getFileName(String key) {
		String[] stringSplited = key.split("[.]");
		int length = stringSplited.length;
		return cacheConfig.getDirPath() + stringSplited[length - 2] + "." + stringSplited[length - 1] + ".soa";
	}

	private String getFileContent(String filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\r\n"));
		} catch (IOException e) {
			LOGGER.error("Error getting file content", e);
		}
		return contentBuilder.toString();

	}
	/**
	 * Used for offline mode, 
	 * if there is any response cached for this service it will be returned
	 * 
	 * @param service
	 * @return random response
	 */
	public String getAnyFileContentSameService(String service) {
		StringBuilder contentBuilder = new StringBuilder();
		String[] stringSplited = service.split("[.]");
		int length = stringSplited.length;		
		Path dir = Paths.get(cacheConfig.getDirPath());
		List<File> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, stringSplited[length - 1]+ "\\.*soa")) {
		    for (Path entry: stream) {
		        files.add(entry.toFile());
		    }
		} catch (Exception e) {
			LOGGER.error("Error finding any response for service" + service, e);
		}
		if(CollectionUtils.isNotEmpty(files)) {
			Random r = new Random();
			File file = files.get(r.nextInt(files.size()));			
			try (Stream<String> stream2 = Files.lines(Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
				stream2.forEach(s -> contentBuilder.append(s).append("\r\n"));
			} catch (IOException e) {
				LOGGER.error("Error getting file content", e);
			}
		}else {
			LOGGER.info("There is no response for service" + service);
		}
		return contentBuilder.toString();
	}

	private boolean fileExists(String filePath) {
		LOGGER.debug("Getting File:" + filePath);
		File tmpDir = new File(filePath);
		return tmpDir.exists();
	}

}
