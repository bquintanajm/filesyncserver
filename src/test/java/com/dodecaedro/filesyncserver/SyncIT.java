package com.dodecaedro.filesyncserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncIT extends ITBase {
	@BeforeEach
	void setUp() throws Exception {
		when()
				.post("/wipe")
			.then().assertThat()
				.statusCode(HTTP_OK);

		given()
				.body(ClassLoader.getSystemResourceAsStream("file.json").readAllBytes())
				.when()
				.post("/push")
				.then().assertThat()
				.statusCode(HTTP_OK);
	}

	@Test
	void timeEndpoint() {
		long time = when()
					.get("/time")
				.then().assertThat()
					.statusCode(HTTP_OK)
					.extract().jsonPath().getLong("server_time_ms");

		assertThat(time).isBetween(0L, Instant.now().getEpochSecond());
	}

	@Test
	void pullEndpoint() {
		String response = when()
					.post("/pull")
				.then().assertThat()
					.statusCode(HTTP_OK)
					.extract().body().asString();

		assertThat(response).isNotNull();
	}

	@Test
	void syncEndpoint() throws Exception {
		given()
			.body(ClassLoader.getSystemResourceAsStream("changes.json").readAllBytes())
		.when()
			.post("/sync")
		.then().assertThat()
			.statusCode(HTTP_OK);
	}
}
