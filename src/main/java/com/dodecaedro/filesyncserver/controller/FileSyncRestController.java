package com.dodecaedro.filesyncserver.controller;

import com.dodecaedro.filesyncserver.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@Slf4j
@RestController
public class FileSyncRestController {
  private final FileStorageService storageService;

  public FileSyncRestController(FileStorageService service) {
    this.storageService = service;
  }

  @PostMapping("/push")
  public void receiveFile(InputStream inputStream) throws Exception {
    log.trace("Push endpoint invoked");
    storageService.saveFile(inputStream);
  }

  @PostMapping("/pull")
  public ResponseEntity<Resource> sendFile() throws Exception {
    log.trace("Pull endpoint invoked");
    return ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new FileSystemResource(storageService.getFile()));
  }

  @RequestMapping(method = RequestMethod.GET, value = "/time")
  public ResponseEntity<String> getTime() throws Exception {
    log.trace("calling get time");
    return ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body("{\"server_time_ms\": " + System.currentTimeMillis() + " }");
  }
}
