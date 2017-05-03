/*
 * Copyright 2015-2017 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.dsl.http.BaseHttpInboundEndpointSpec;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.http.HttpRequestHandlerEndpointSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

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
		// TODO Until SI Java DSL 1.2.2. Use mappedRequestHeaders() instead
		DefaultMixedCaseContentTypeHttpHeaderMapper defaultHttpHeaderMapper =
				DefaultMixedCaseContentTypeHttpHeaderMapper.inboundMapper();
		defaultHttpHeaderMapper.setInboundHeaderNames(this.properties.getMappedRequestHeaders());

		return Http.inboundChannelAdapter(this.properties.getPathPattern())
				.headerMapper(defaultHttpHeaderMapper)
//				.mappedRequestHeaders(this.properties.getMappedRequestHeaders())
				.statusCodeExpression(new ValueExpression<>(HttpStatus.ACCEPTED))
				.requestMapping(new Consumer<BaseHttpInboundEndpointSpec.RequestMappingSpec>() {

					@Override
					public void accept(BaseHttpInboundEndpointSpec.RequestMappingSpec requestMappingSpec) {
						requestMappingSpec.methods(HttpMethod.POST)
								.consumes(consumes);
					}

				})
				.requestChannel(this.channels.output());
	}

	@Configuration
	@ConditionalOnProperty(prefix = "http", name = "secured", havingValue = "false", matchIfMissing = true)
	@EnableWebSecurity
	protected static class DisableSecurityConfiguration {

	}

}
