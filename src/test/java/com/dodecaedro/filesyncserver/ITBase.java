package com.dodecaedro.filesyncserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.SSLConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.testcontainers.containers.GenericContainer;

import java.io.File;

import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.junit.Assert.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ITBase {
	static {
		GenericContainer mongoContainer = new GenericContainer("mongo:4.1.10")
				.withExposedPorts(27017);

		mongoContainer.start();

		System.setProperty("application.mongo-host", mongoContainer.getContainerIpAddress());
		System.setProperty("application.mongo-port", mongoContainer.getFirstMappedPort() + "");
	}

	@Autowired
	private ObjectMapper objectMapper;
	@LocalServerPort
	private int port;

	@BeforeEach
	public void setUpRestAssured() {
		RestAssured.baseURI = "https://localhost";
		RestAssured.basePath = "/";
		RestAssured.port = port;
		RestAssured.requestSpecification = new RequestSpecBuilder()
				.addQueryParam("key", "ABCDEF")
				.build();
		RestAssured.config = config()
				.objectMapperConfig(config().getObjectMapperConfig()
						.jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
				.sslConfig(sslConfig()
						.relaxedHTTPSValidation()
						.allowAllHostnames());
	}

	String toJson(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			fail();
			return null;
		}
	}
}
