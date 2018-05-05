package com.dodecaedro.filesyncserver.controller;

import com.dodecaedro.filesyncserver.service.AuthenticationService;
import com.dodecaedro.filesyncserver.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@RestController
public class FileSyncRestController {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileSyncRestController.class);

  private final FileStorageService storageService;
  private final AuthenticationService authenticationService;

  public FileSyncRestController(FileStorageService service,
                                AuthenticationService authenticationService) {
    this.storageService = service;
    this.authenticationService = authenticationService;
  }

  @PostMapping("/push")
  public void receiveFile(
      @RequestParam(value = "key") String key,
      InputStream inputStream)
      throws Exception {

    LOGGER.trace("Push endpoint invoked");

    authenticationService.verifyApiKey(key);
    storageService.saveFile(inputStream);
  }

  @PostMapping("/pull")
  public ResponseEntity<Resource> sendFile(@RequestParam(value = "key") String key)
      throws Exception {

    LOGGER.trace("Pull endpoint invoked");

    authenticationService.verifyApiKey(key);
    return ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new FileSystemResource(storageService.getFile()));
  }

  @GetMapping("/time")
  public String getTime(@RequestParam(value = "key") String key)
      throws Exception {

    LOGGER.trace("Time endpoint invoked");

    authenticationService.verifyApiKey(key);
    return ZonedDateTime.now().format(ISO_OFFSET_DATE_TIME);
  }
}
