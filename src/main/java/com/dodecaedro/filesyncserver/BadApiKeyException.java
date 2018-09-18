package com.dodecaedro.filesyncserver;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "API key incorrect")
public class BadApiKeyException extends Exception {
}
