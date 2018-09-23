package com.dodecaedro.filesyncserver.infrastructure.controller;

import com.dodecaedro.filesyncserver.infrastructure.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;

@Slf4j
@RestController
public class FileSyncRestController {
  private final FileStorageService storageService;
  private final ObjectMapper objectMapper;

  public FileSyncRestController(FileStorageService service, ObjectMapper objectMapper) {
    this.storageService = service;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/push")
  public void receiveFile(InputStream inputStream) throws Exception {
    log.trace("Push endpoint invoked");
    storageService.saveFile(inputStream);
  }

  @PostMapping("/pull")
  @ResponseBody
  public ResponseEntity<Resource> sendFile(HttpServletRequest request) throws Exception {
    log.trace("Pull endpoint invoked: {}", request.getRequestURL().append('?').append(request.getQueryString()));

    return ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new FileSystemResource(storageService.getFilePath()));
  }

  @RequestMapping(method = RequestMethod.GET, value = "/time")
  public ResponseEntity<String> getTime(HttpServletRequest request) {
    log.trace("Time endpoint invoked: {}", request.getRequestURL().append('?').append(request.getQueryString()));
    log.trace("Current time: {}", System.currentTimeMillis());
    return ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body("{\"server_time_ms\": " + System.currentTimeMillis() + " }");
  }

  @RequestMapping(method = RequestMethod.POST, value = "/sync")
  public ResponseEntity<String> syncJson(HttpServletRequest request) throws Exception {
    log.trace("calling sync: {}", request.getRequestURL().append('?').append(request.getQueryString()));

    return ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectMapper.writeValueAsString(storageService.syncFile(request.getInputStream())));
  }
}
