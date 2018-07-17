package org.flas.soap.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 
 * @author fernando.las@gmail.com
 *
 */
@ConfigurationProperties("basic.auth")
@Component
public class BasicAuthConfig {

	@Value("${basic.auth.enabled:false}")
	private Boolean enabled;
	private String username;
	private String password;

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}