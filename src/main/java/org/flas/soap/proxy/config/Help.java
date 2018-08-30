package org.flas.soap.proxy.config;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.springframework.core.io.ClassPathResource;

public class Help {
	private static final Logger LOGGER = Logger.getLogger(Help.class);
	public static final String TEXT;

	private Help() {
	}

	static {
		String text = StringUtils.EMPTY;
		try (InputStream stream = new ClassPathResource("help.txt").getInputStream()) {

			text = IOUtils.toString(stream, "UTF-8");
		} catch (Exception e) {
			LOGGER.error("error loading help file", e);
		} finally {
			TEXT = text;
		}
	}
}
