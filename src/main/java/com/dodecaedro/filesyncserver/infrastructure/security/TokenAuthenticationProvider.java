package com.dodecaedro.filesyncserver.infrastructure.security;

import com.dodecaedro.filesyncserver.FileSyncProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

@Slf4j
public class TokenAuthenticationProvider implements AuthenticationProvider {
  private final FileSyncProperties properties;

  public TokenAuthenticationProvider(FileSyncProperties properties) {
    this.properties = properties;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String key = Optional.ofNullable(authentication.getCredentials())
      .map(Object::toString)
      .orElse(null);

    log.debug("Authenticating key");
    if (!properties.getKey().equals(key)) {
      log.debug("Wrong authentication key");
      return null;
    } else {
      log.debug("Authentication sucessful");
      return authentication;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }
}
