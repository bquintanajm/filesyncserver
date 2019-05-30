package com.dodecaedro.filesyncserver;

import com.dodecaedro.filesyncserver.infrastructure.service.FileSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.time.Instant;

import static org.springframework.web.servlet.function.ServerResponse.ok;

@Slf4j
@Component
public class FileSyncHandler {
	private final FileSyncService fileSyncService;
	private final ObjectMapper objectMapper;

	public FileSyncHandler(FileSyncService fileSyncService, ObjectMapper objectMapper) {
		this.fileSyncService = fileSyncService;
		this.objectMapper = objectMapper;
	}

	ServerResponse handlePushFile(ServerRequest serverRequest) throws Exception {
		log.trace("-- calling push: {}", serverRequest.servletRequest()
				.getRequestURL().append('?').append(serverRequest.servletRequest().getQueryString()));

		fileSyncService.saveAllItemsTagsAndDeletions(
				objectMapper.readValue(serverRequest.servletRequest().getInputStream(), JsonObject.class)
		);

		return ok().build();
	}

	ServerResponse handlePullFile(ServerRequest serverRequest) throws Exception {
		log.trace("-- calling pull");

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(objectMapper.writeValueAsString(fileSyncService.findAllItemsTagsAndDeletions()));
	}

	ServerResponse handleGetTime(ServerRequest serverRequest) {
		log.trace("-- calling time: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		long epochMilli = Instant.now().toEpochMilli();
		log.trace("current time: {} ms", epochMilli);
		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"server_time_ms\": " + epochMilli + " }");
	}

	ServerResponse handleSyncJson(ServerRequest serverRequest) throws Exception {
		log.trace("-- calling sync: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						objectMapper.writeValueAsString(fileSyncService.sync(
								objectMapper.readValue(serverRequest.servletRequest().getInputStream(),
										JsonObject.class
								)
						)
				));
	}

	ServerResponse handleWipe(ServerRequest serverRequest) {
		log.trace("-- calling wipe: {}", serverRequest.servletRequest().getRequestURL().append('?')
				.append(serverRequest.servletRequest().getQueryString()));

		fileSyncService.deleteAllItemsTagsAndDeletions();

		return ok().build();
	}
}
