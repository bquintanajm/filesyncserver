package com.dodecaedro.filesyncserver;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncIT extends ITBase {
	@Test
	void testSync() {
		String response = when()
					.get("/time")
				.then().assertThat()
					.statusCode(HTTP_OK)
					.extract().jsonPath().get("server_time_ms").toString();

		assertThat(Long.valueOf(response)).isBetween(0L, Instant.now().getEpochSecond());
	}
}
