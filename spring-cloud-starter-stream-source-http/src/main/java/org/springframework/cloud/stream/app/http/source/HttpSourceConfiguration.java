/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.http.dsl.HttpRequestHandlerEndpointSpec;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * A source module that listens for HTTP requests and emits the body as a message payload.
 * If the Content-Type matches 'text/*' or 'application/json', the payload will be a String,
 * otherwise the payload will be a byte array.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties(HttpSourceProperties.class)
public class HttpSourceConfiguration {

	@Autowired
	private Source channels;

	@Autowired
	private HttpSourceProperties properties;

	@Bean
	public HttpRequestHandlingEndpointSupport httpSourceString() {
		return buildHttpRequestHandlerEndpointSpec("text/*", "application/json")
				.requestPayloadType(String.class)
				.get();
	}

	@Bean
	public HttpRequestHandlingEndpointSupport httpSourceBytes() {
		return buildHttpRequestHandlerEndpointSpec("*/*")
				.get();
	}

	private HttpRequestHandlerEndpointSpec buildHttpRequestHandlerEndpointSpec(final String... consumes) {
		return Http.inboundChannelAdapter(this.properties.getPathPattern())
				.mappedRequestHeaders(this.properties.getMappedRequestHeaders())
				.statusCodeExpression(new ValueExpression<>(HttpStatus.ACCEPTED))
				.requestMapping(requestMapping ->
						requestMapping.methods(HttpMethod.POST)
								.consumes(consumes))
				.crossOrigin(crossOrigin ->
						crossOrigin.origin(this.properties.getCors().getAllowedOrigins())
								.allowedHeaders(this.properties.getCors().getAllowedHeaders())
								.allowCredentials(this.properties.getCors().getAllowCredentials()))
				.requestChannel(this.channels.output());
	}

	/**
	 * The custom {@link WebSecurityConfigurerAdapter} to disable security in the application
	 * if {@code http.enableSecurity = false} (default).
	 * When {@code http.enableSecurity = true} and {@code http.enableCsrf = false} (default),
	 * the CSRF protection is disabled in the application.
	 * If both options are {@code true}, then this configuration falls back to the default
	 * Spring Security configuration.
	 * @see org.springframework.boot.autoconfigure.security.servlet.SpringBootWebSecurityConfiguration
	 */
	@Configuration
	protected static class HttpSourceSecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Autowired
		private HttpSourceProperties properties;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			if (!this.properties.isEnableSecurity()) {
				http.requestMatcher(request -> false);
			}
			else if (!this.properties.isEnableCsrf()) {
				super.configure(http);
				http.csrf().disable();
			}
			else {
				super.configure(http);
			}
		}

	}

}
