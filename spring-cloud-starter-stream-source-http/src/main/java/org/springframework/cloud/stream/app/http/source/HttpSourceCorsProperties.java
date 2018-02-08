/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.http.source;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.cors.CorsConfiguration;

/**
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ConfigurationProperties("http.cors")
@Validated
public class HttpSourceCorsProperties {

	/**
	 * List of allowed origins, e.g. "http://domain1.com".
	 */
	private String[] allowedOrigins = { CorsConfiguration.ALL };

	/**
	 * List of request headers that can be used during the actual request.
	 */
	private String[] allowedHeaders = { CorsConfiguration.ALL };

	/**
	 * Whether the browser should include any cookies associated with the domain of the request being annotated.
	 */
	private Boolean allowCredentials;

	@NotEmpty
	public String[] getAllowedOrigins() {
		return this.allowedOrigins;
	}

	public void setAllowedOrigins(String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@NotEmpty
	public String[] getAllowedHeaders() {
		return this.allowedHeaders;
	}

	public void setAllowedHeaders(String[] allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	public Boolean getAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

}
