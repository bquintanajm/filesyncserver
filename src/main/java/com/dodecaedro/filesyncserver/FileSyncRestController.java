package com.dodecaedro.filesyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class FileSyncRestController {
	private final FileSyncProperties properties;
	private final FileDao fileDao;
	private final ObjectMapper objectMapper;

	public FileSyncRestController(FileSyncProperties properties, FileDao fileDao, ObjectMapper objectMapper) {
		this.properties = properties;
		this.fileDao = fileDao;
		this.objectMapper = objectMapper;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/time")
	public ResponseEntity<String> getTime(
			@RequestParam(value = "key") String key) throws Exception {
		log.trace("calling get time");

		verifyApiKey(key);

		return ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"server_time_ms\": " + System.currentTimeMillis() + " }");
	}

	@RequestMapping(method = RequestMethod.POST, value = "/sync")
	public ResponseEntity<String> syncJson(
			@RequestParam(value = "key") String key,
			HttpServletRequest requestEntity) throws Exception {
		log.trace("calling sync");

		verifyApiKey(key);

		return ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(objectMapper.writeValueAsString(fileDao.syncFile(requestEntity.getInputStream())));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/push")
	public void receiveFile(
			@RequestParam(value = "key") String key,
			HttpServletRequest requestEntity) throws Exception {
		log.trace("calling push");

		verifyApiKey(key);

		fileDao.saveFile(requestEntity.getInputStream());
	}

	@RequestMapping(method = RequestMethod.POST, value = "/pull")
	public ResponseEntity<FileSystemResource> sendFile(
			@RequestParam(value = "key") String key) throws Exception {
		log.trace("calling pull");

		verifyApiKey(key);

		return ResponseEntity
				.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new FileSystemResource(fileDao.getFile()));
	}

	private void verifyApiKey(String key) throws BadApiKeyException {
		if (!properties.getKey().equals(key)) {
			log.debug("authentication key failure!");
			throw new BadApiKeyException();
		}
	}
}
