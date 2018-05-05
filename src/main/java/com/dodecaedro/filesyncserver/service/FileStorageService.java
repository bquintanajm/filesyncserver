package com.dodecaedro.filesyncserver.service;

import com.dodecaedro.filesyncserver.FileSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
public class FileStorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

  private final FileSyncProperties properties;

  public FileStorageService(FileSyncProperties properties) {
    this.properties = properties;
  }

  public void saveFile(InputStream inputStream) throws IOException {
    var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
    LOGGER.debug("Saving file in: {}", target);

    Files.copy(inputStream, target, REPLACE_EXISTING);
  }

  public File getFile() throws FileNotFoundException {
    var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
    LOGGER.debug("Retrieving file from: {}", target);

    if (!target.toFile().exists()) {
      LOGGER.debug("File in {} does not exist", target);
      throw new FileNotFoundException();
    }

    return target.toFile();
  }
}
