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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.embedded.LocalServerPort;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests for HttpSourceConfiguration.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class HttpSourceTests {

	@Autowired
	protected Source channels;

	@LocalServerPort
	protected int port;

	@Autowired
	protected MessageCollector messageCollector;

	protected TestRestTemplate restTemplate;

	@Before
	public void setup() {
		this.restTemplate = new TestRestTemplate();
	}

	@TestPropertySource(properties = "http.pathPattern=/foo")
	public static class NonSecuredTests extends HttpSourceTests {

		@Test
		public void testText() {
			ResponseEntity<?> entity = restTemplate.postForEntity("http://localhost:" + port + "/foo", "hello", Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello")));
		}

		@Test
		public void testBytes() {
			ResponseEntity<?> entity = restTemplate.postForEntity("http://localhost:" + port + "/foo", "hello".getBytes(), Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello".getBytes())));
		}

		@Test
		public void testJson() throws Exception {
			String json = "{\"foo\":1,\"bar\":true}";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON); // ends up as Content-Type in mapper
			headers.set("foo", "bar");
			RequestEntity<String> request = new RequestEntity<String>(json, headers, HttpMethod.POST, new URI("http://localhost:" + port + "/foo"));
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
			RequestEntity<String> request = new RequestEntity<String>(json, headers, HttpMethod.POST, new URI("http://localhost:" + port + "/foo"));
			ResponseEntity<?> response = restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
			Message<?> message = messageCollector.forChannel(channels.output()).poll(1, TimeUnit.SECONDS);
			assertEquals(json, message.getPayload());
			assertEquals(MediaType.APPLICATION_JSON_UTF8, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertFalse(message.getHeaders().containsKey("foo"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
			assertFalse(health.containsKey("diskSpace"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testEnvEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/env", Map.class);
			assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
			assertTrue(response.hasBody());
			assertEquals("Full authentication is required to access this resource", response.getBody().get("message"));
		}

	}


	@TestPropertySource(properties = "management.security.enabled=false")
	public static class NonSecuredManagementDisabledTests extends HttpSourceTests {

		@Test
		public void testText() {
			ResponseEntity<?> entity = restTemplate.postForEntity("http://localhost:" + port, "hello", Object.class);
			assertEquals(HttpStatus.ACCEPTED, entity.getStatusCode());
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("hello")));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
			assertTrue(health.containsKey("diskSpace"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testEnvEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/env", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map env = response.getBody();

			assertTrue(env.containsKey("server.ports"));

			Map ports = (Map) env.get("server.ports");

			assertEquals(this.port, ports.get("local.server.port"));
		}

	}


	@TestPropertySource(properties = {"http.mappedRequestHeaders = *", "http.secured = true"})
	public static class SecuredTests extends HttpSourceTests {

		@Autowired
		private SecurityProperties securityProperties;

		@Override
		@Before
		public void setup() {
			this.restTemplate = new TestRestTemplate(this.securityProperties.getUser().getName(),
				this.securityProperties.getUser().getPassword());
		}

		@Test
		public void testText() throws Exception {
			HttpHeaders headers = new HttpHeaders();
			headers.set("foo", "bar");
			RequestEntity<String> request = new RequestEntity<String>("hello", headers, HttpMethod.POST, new URI("http://localhost:" + port));
			ResponseEntity<?> response = restTemplate.exchange(request, Object.class);
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
			Message<?> message = messageCollector.forChannel(channels.output()).poll(1, TimeUnit.SECONDS);
			assertThat(message, hasPayload("hello"));
			assertThat(message, hasHeader("foo", "bar"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testHealthEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/health", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map health = response.getBody();

			assertEquals("UP", health.get("status"));
			assertTrue(health.containsKey("diskSpace"));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void testEnvEndpoint() throws Exception {
			ResponseEntity<Map> response = this.restTemplate.getForEntity("http://localhost:" + port + "/env", Map.class);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertTrue(response.hasBody());

			Map env = response.getBody();

			assertTrue(env.containsKey("server.ports"));

			Map ports = (Map) env.get("server.ports");

			assertEquals(this.port, ports.get("local.server.port"));
		}

	}

	@SpringBootApplication
	static class HttpSourceApplication {

		public static void main(String[] args) {
			SpringApplication.run(HttpSourceApplication.class, args);
		}

	}

}
