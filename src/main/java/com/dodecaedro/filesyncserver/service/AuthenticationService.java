package com.dodecaedro.filesyncserver.service;

import com.dodecaedro.filesyncserver.exception.BadApiKeyException;
import com.dodecaedro.filesyncserver.FileSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

  private final FileSyncProperties properties;

  public AuthenticationService(FileSyncProperties properties) {
    this.properties = properties;
  }

  public void verifyApiKey(String key) throws BadApiKeyException {
    LOGGER.debug("Authenticating key");
    if (!properties.getKey().equals(key)) {
      LOGGER.debug("Wrong authentication key");
      throw new BadApiKeyException();
    }
    LOGGER.debug("Authentication sucessful");
  }
}
