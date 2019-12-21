package org.markjay.loggingfilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.markjay.loggingfilter.model.SomeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class LoggingFilterApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void greetingShouldReturnDefaultMessage() {
		assertThat(this.restTemplate.getForObject(getUrl("/index"),
				String.class)).contains("Hello World");
	}

	@Test
	void postByParamShouldReturnValue() {
		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
		bodyMap.add("parameter1", "parameter1Value");
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyMap, getHeaders());

		SomeEntity response = this.restTemplate.postForObject(
				getUrl("/examples/post/by-param"),
				requestEntity,
				SomeEntity.class);

		assertThat(response.getMessage()).contains("body='parameter1=parameter1Value'");
		assertThat(response.getMessage()).matches("^.*headers='[^']*headerkey1=headerValue1[^']*'.*$");
		assertThat(response.getMessage()).matches("^.*headers='[^']*headerkey2=headerValue2[^']*'.*$");
	}

	@Test
	void postByJsonBodyShouldReturnValue() throws JsonProcessingException {
		HttpHeaders headers = getHeaders(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(new SomeEntity("someEntityMessage")), headers);

		SomeEntity response = this.restTemplate.postForObject(
				getUrl("/examples/post/by-json-body"),
				requestEntity,
				SomeEntity.class);

		assertThat(response.getMessage()).contains("body='parameter1={\"message\":\"someEntityMessage\"}'");
		assertThat(response.getMessage()).matches("^.*headers='[^']*headerkey1=headerValue1[^']*'.*$");
		assertThat(response.getMessage()).matches("^.*headers='[^']*headerkey2=headerValue2[^']*'.*$");
	}

	private HttpHeaders getHeaders(MediaType mediaType) {
		HttpHeaders headers = getHeaders();
		headers.setContentType(mediaType);
		return headers;
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("headerKey1", "headerValue1");
		headers.add("headerKey2", "headerValue2");
		headers.setBearerAuth("secretToken");
		return headers;
	}

	private String getUrl(String relativePath) {
		return "http://localhost:" + port + relativePath;
	}
}
