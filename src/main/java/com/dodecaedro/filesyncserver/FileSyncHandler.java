package com.dodecaedro.filesyncserver;

import com.dodecaedro.filesyncserver.infrastructure.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;

import static org.springframework.web.servlet.function.ServerResponse.ok;

@Slf4j
@Component
public class FileSyncHandler {
  private final FileStorageService fileStorageService;
  private final ObjectMapper objectMapper;

  public FileSyncHandler(FileStorageService fileStorageService, ObjectMapper objectMapper) {
    this.fileStorageService = fileStorageService;
    this.objectMapper = objectMapper;
  }

  ServerResponse handlePushFile(ServerRequest serverRequest) throws IOException {
    log.trace("Push endpoint invoked");
    fileStorageService.saveFile(serverRequest.servletRequest().getInputStream());
    return ok().build();
  }

  ServerResponse handlePullFile(ServerRequest serverRequest) throws FileNotFoundException {
    log.trace("Pull endpoint invoked: {}", serverRequest.servletRequest()
      .getRequestURL().append('?').append(serverRequest.servletRequest().getQueryString()));

    return ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(new FileSystemResource(fileStorageService.getFilePath()));
  }

  ServerResponse handleGetTime(ServerRequest serverRequest) {
    log.trace("Time endpoint invoked: {}", serverRequest.servletRequest().getRequestURL().append('?')
      .append(serverRequest.servletRequest().getQueryString()));
    log.trace("Current time: {}", Instant.now().getEpochSecond());

    return ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body("{\"server_time_ms\": " + System.currentTimeMillis() + " }");
  }

  ServerResponse handleSyncJson(ServerRequest serverRequest) throws Exception {
    log.trace("calling sync: {}", serverRequest.servletRequest().getRequestURL().append('?')
      .append(serverRequest.servletRequest().getQueryString()));

    return ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectMapper.writeValueAsString(fileStorageService.syncFile(serverRequest.servletRequest().getInputStream())));
  }
}
