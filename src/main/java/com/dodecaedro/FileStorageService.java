package com.dodecaedro;

import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
public class FileStorageService {
	private final FileSyncProperties properties;

	public FileStorageService(FileSyncProperties properties) {
		this.properties = properties;
	}

	public void saveFile(InputStream inputStream) throws IOException {
		var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
		log.debug("Saving file in: {}", target);

		Files.copy(inputStream, target, REPLACE_EXISTING);
	}

	public InputStream getFileInputStream() throws IOException {
		var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
		log.debug("Retrieving file from: {}", target);

		if (!target.toFile().exists()) {
			log.debug("File in {} does not exist", target);
			throw new FileNotFoundException();
		}
		if (!target.toFile().canRead()) {
			log.error("Cannot read the file on the path: {}", target);
		}

		return Files.newInputStream(target);
	}
}
