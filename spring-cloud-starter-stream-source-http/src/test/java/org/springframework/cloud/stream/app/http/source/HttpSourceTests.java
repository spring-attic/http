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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

/**
 * Tests for HttpSourceConfiguration.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class HttpSourceTests {

	@Autowired
	protected Source channels;

	@Autowired
	protected MessageCollector messageCollector;

	@Autowired
	protected TestRestTemplate restTemplate;

	@TestPropertySource(properties = {
			"http.pathPattern = /foo"})
	public static class NonSecuredTests extends HttpSourceTests {

		@Test
		public void testText() {
			ResponseEntity<?> entity = this.restTemplate.postForEntity("/foo", "hello", Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello")));
		}

		@Test
		public void testBytes() {
			ResponseEntity<?> entity = this.restTemplate.postForEntity("/foo", "hello".getBytes(), Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello".getBytes())));
		}

		@Test
		public void testJson() throws Exception {
			String json = "{\"foo\":1,\"bar\":true}";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON); // ends up as Content-Type in mapper
			headers.set("foo", "bar");
			RequestEntity<String> request = new RequestEntity<>(json, headers, HttpMethod.POST, new URI("/foo"));
			ResponseEntity<?> response = restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
			Message<?> message = messageCollector.forChannel(channels.output()).poll(1, TimeUnit.SECONDS);
			assertEquals(json, message.getPayload());
			assertEquals(MediaType.APPLICATION_JSON_UTF8, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertFalse(message.getHeaders().containsKey("foo"));
		}

		@Test
		public void testJsonLowerCaseContentType() throws Exception {
			String json = "{\"foo\":1,\"bar\":true}";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8); // ends up as content-type in mapper
			headers.set("foo", "bar");
			RequestEntity<String> request = new RequestEntity<>(json, headers, HttpMethod.POST, new URI("/foo"));
			ResponseEntity<?> response = restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
			Message<?> message = messageCollector.forChannel(channels.output()).poll(1, TimeUnit.SECONDS);
			assertEquals(json, message.getPayload());
			assertEquals(MediaType.APPLICATION_JSON_UTF8, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertFalse(message.getHeaders().containsKey("foo"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testEnvEndpoint() {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/env", Map.class);
			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		}

	}


	@TestPropertySource(properties = "management.endpoints.web.exposure.include = *")
	public static class NonSecuredManagementDisabledTests extends HttpSourceTests {

		@Test
		public void testText() {
			ResponseEntity<?> entity = this.restTemplate.postForEntity("/", "hello", Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello")));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
		}

		@Test
		public void testEnvEndpoint() {
			ResponseEntity<Object> response = this.restTemplate.getForEntity("/actuator/env", Object.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());
		}

	}


	@TestPropertySource(properties = {
			"http.mappedRequestHeaders = *",
			"spring.cloud.streamapp.security.enabled = true",
			"spring.cloud.streamapp.security.csrf-enabled=false",
			"http.cors.allowedOrigins = /bar"
	})
	public static class SecuredTests extends HttpSourceTests {

		@Autowired
		private SecurityProperties securityProperties;

		@Before
		public void setup() {
			this.restTemplate.getRestTemplate().getInterceptors().add(new BasicAuthenticationInterceptor(
					securityProperties.getUser().getName(), securityProperties.getUser().getPassword()));
		}

		@Test
		public void testText() throws Exception {
			HttpHeaders headers = new HttpHeaders();
			headers.set("foo", "bar");
			headers.set(HttpHeaders.ORIGIN, "/bar");
			RequestEntity<String> request = new RequestEntity<>("hello", headers, HttpMethod.POST, new URI("/"));
			ResponseEntity<?> response = this.restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
			Message<?> message = messageCollector.forChannel(channels.output()).poll(1, TimeUnit.SECONDS);
			assertThat(message, hasPayload("hello"));
			assertThat(message, hasHeader("foo", "bar"));

			headers.set(HttpHeaders.ORIGIN, "/junk");
			request = new RequestEntity<>("junk", headers, HttpMethod.POST, new URI("/"));
			response = this.restTemplate.exchange(request, String.class);
			assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
			assertEquals("Invalid CORS request", response.getBody());
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("/actuator/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
		}

		@Test
		public void testEnvEndpoint() {
			ResponseEntity<Object> response = this.restTemplate.getForEntity("/actuator/env", Object.class);
			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		}

	}

	@TestPropertySource(properties = {
			"http.mappedRequestHeaders = *",
			"spring.cloud.streamapp.security.enabled = true",
			"spring.cloud.streamapp.security.csrf-enabled=true"
	})
	public static class CsrfEnabledTests extends HttpSourceTests {

		@Autowired
		private SecurityProperties securityProperties;

		@Before
		public void setup() {
			this.restTemplate.getRestTemplate().getInterceptors().add(new BasicAuthenticationInterceptor(
					securityProperties.getUser().getName(), securityProperties.getUser().getPassword()));
		}

		@Test
		public void testText() throws Exception {
			RequestEntity<String> request =
					new RequestEntity<>("hello", new HttpHeaders(), HttpMethod.POST, new URI("/"));
			ResponseEntity<?> response = this.restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		}

	}

	@SpringBootApplication
	static class HttpSourceApplication {

	}

}
