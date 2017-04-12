/*
 * Copyright 2017 the original author or authors.
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

import org.hibernate.validator.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.validation.annotation.Validated;

/**
 * @author Artem Bilan
 *
 */
@ConfigurationProperties("http")
@Validated
public class HttpSourceProperties {

	/**
	 * HTTP endpoint path mapping.
	 */
	private String pathPattern = "/";

	/**
	 * Headers that will be mapped.
	 */
	private String[] mappedRequestHeaders = { DefaultHttpHeaderMapper.HTTP_REQUEST_HEADER_NAME_PATTERN };

	/**
	 * Secure or not HTTP source path.
	 */
	private boolean secured;

	@NotEmpty
	public String getPathPattern() {
		return pathPattern;
	}

	public void setPathPattern(String pathPattern) {
		this.pathPattern = pathPattern;
	}

	public String[] getMappedRequestHeaders() {
		return mappedRequestHeaders;
	}

	public void setMappedRequestHeaders(String[] mappedRequestHeaders) {
		this.mappedRequestHeaders = mappedRequestHeaders;
	}

	public boolean isSecured() {
		return this.secured;
	}

	public void setSecured(boolean secured) {
		this.secured = secured;
	}

}
