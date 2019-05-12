package com.dodecaedro.filesyncserver;

import com.dodecaedro.filesyncserver.infrastructure.service.MongoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.json.Json;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;

import static org.springframework.web.servlet.function.ServerResponse.ok;

@Slf4j
@Component
public class FileSyncHandler {
	private final MongoService mongoService;
	private final ObjectMapper objectMapper;

	public FileSyncHandler(MongoService mongoService, ObjectMapper objectMapper) {
		this.mongoService = mongoService;
		this.objectMapper = objectMapper;
	}

	ServerResponse handlePushFile(ServerRequest serverRequest) throws IOException {
		log.trace("Push endpoint invoked: {}", serverRequest.servletRequest()
				.getRequestURL().append('?').append(serverRequest.servletRequest().getQueryString()));

		var reader = Json.createReader(serverRequest.servletRequest().getInputStream());
		mongoService.saveAllItemsTagsAndDeletions(reader.readObject());

		return ok().build();
	}

	ServerResponse handlePullFile(ServerRequest serverRequest) {
		log.trace("Pull endpoint invoked");

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(mongoService.findAllItemsTagsAndDeletions());
	}

	ServerResponse handleGetTime(ServerRequest serverRequest) {
		log.trace("Time endpoint invoked: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"server_time_ms\": " + Instant.now().getEpochSecond() + " }");
	}

	ServerResponse handleSyncJson(ServerRequest serverRequest) throws Exception {
		log.trace("calling sync: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		var reader = Json.createReader(serverRequest.servletRequest().getInputStream());

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(objectMapper.writeValueAsString(mongoService.sync(reader.readObject())));
	}

	ServerResponse handleWipe(ServerRequest serverRequest) throws Exception {
		log.trace("calling wipe: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		mongoService.deleteAllItemsTagsAndDeletions();

		return ok().build();
	}
}
