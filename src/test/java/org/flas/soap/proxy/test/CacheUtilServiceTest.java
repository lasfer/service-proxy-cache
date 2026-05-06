package org.flas.soap.proxy.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.flas.soap.proxy.config.CacheConfig;
import org.flas.soap.proxy.services.CacheUtilService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.util.ReflectionTestUtils;

public class CacheUtilServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void getAnyFileContentSameServiceDoesNotReturnWsdlCacheFiles() throws Exception {
		CacheUtilService cache = cacheWithTempDir();
		writeCacheFile("Customer.wsdl_GET.soa", "<definitions/>");

		assertEquals("", cache.getAnyFileContentSameService("Customer"));
	}

	@Test
	public void getAnyFileContentSameServiceReturnsSoapResponseFiles() throws Exception {
		CacheUtilService cache = cacheWithTempDir();
		writeCacheFile("Customer.123.soa", "<soap:Envelope/>");

		assertEquals("<soap:Envelope/>\r\n", cache.getAnyFileContentSameService("Customer"));
	}

	private CacheUtilService cacheWithTempDir() {
		CacheConfig cacheConfig = new CacheConfig();
		cacheConfig.setDirPath(tempFolder.getRoot().getAbsolutePath());
		cacheConfig.setFileCaching(true);

		CacheUtilService cache = new CacheUtilService();
		ReflectionTestUtils.setField(cache, "cacheConfig", cacheConfig);
		return cache;
	}

	private void writeCacheFile(String name, String content) throws Exception {
		File file = new File(tempFolder.getRoot(), name);
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}
}
