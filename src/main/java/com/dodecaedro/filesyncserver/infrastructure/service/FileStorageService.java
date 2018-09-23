package com.dodecaedro.filesyncserver.infrastructure.service;

import com.dodecaedro.filesyncserver.FileSyncProperties;
import com.dodecaedro.filesyncserver.domain.model.SyncResponse;
import com.jayway.jsonpath.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@Service
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

  public void saveFile(String body) throws IOException {
    var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
    log.debug("Saving file in: {}", target);

    Files.copy(new ByteArrayInputStream(body.getBytes()), target, REPLACE_EXISTING);
  }

  private File getOrCreateFile() throws IOException {
    var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName()).toFile();
    log.debug("Retrieving file from: {}", target.getPath());

    if (!target.exists()) {
      log.warn("File in {} does not exist. Will create a new one.", target.getPath());
      saveFile(new ClassPathResource("empty.json").getInputStream());
      return getFilePath().toFile();
    }
    return target;
  }

  public Path getFilePath() throws FileNotFoundException {
    var target = Paths.get(properties.getFilePath()).resolve(properties.getFileName());
    log.debug("Retrieving file from: {}", target);

    if (!target.toFile().exists()) {
      log.debug("File in {} does not exist", target);
      throw new FileNotFoundException();
    }
    if (!target.toFile().canRead()) {
      log.error("Cannot read the file on the path: {}", target);
    }

    return target;
  }

  public SyncResponse syncFile(InputStream inputStream) throws Exception {
    DocumentContext contextChanges = JsonPath.parse(inputStream);
    DocumentContext contextDB = JsonPath.parse(getOrCreateFile());

    log.debug(contextChanges.jsonString());

    Long lastUpdated = contextChanges.read("$.last_sync_ts", Long.class);

    SyncResponse response = new SyncResponse();
    // 1. process items
    response.setItems(processItems(contextChanges, contextDB, lastUpdated));
    // 2. process tags
    response.setTags(processTags(contextChanges, contextDB, lastUpdated));
    // 3. process deletions
    response.setDeletionsToAdd(processDeletions(contextChanges, contextDB, lastUpdated));

    // 6. save file - todo: the whole file is in memory
    saveFile(contextDB.jsonString());

    response.setSuccess(true);
    response.setSyncTs(System.currentTimeMillis());
    log.debug("Sync sucessful at: {}", response.getSyncTs());

    return response;
  }

  private List<Map<String, Object>> processItems(DocumentContext contextChanges, DocumentContext contextDB, Long lastUpdated) {
    // 1. get ids of elements that may have potentially changed in the db
    List<String> changedIds = getStringList(
      contextChanges,
      "$.changes.items[*].id"
    );

    // 2. get those elements from the db - optimization: get from db only those elements that may have changed, not all
    List<Map<String, Object>> elementsToBeChangedInDB = getMapList(
      contextDB,
      "$.items[?]",
      filter(where("id").in(changedIds))
    );

    // 3. delete the changes which are older than the same ones currently in db
    elementsToBeChangedInDB.forEach(dbElement -> contextChanges.delete("$.changes.items[?]",
      filter(where("id").is(dbElement.get("id"))
        .and("changed_ts").lte(dbElement.get("changed_ts")))));

    // 4. delete in db whatever is left in list of changes
    contextDB.delete(
      "$.items[?]",
      filter(where("id").in(contextChanges.read("$.changes.items[*].id", List.class)))
    );

    // 5. add elements from changes - also adds elements which did not exist
    contextChanges.read("$.changes.items[*]", List.class)
      .forEach(changedItem -> contextDB.add("$.items", changedItem));

    if (lastUpdated == null) {
      // never synchronized before
      return getMapList(
        contextDB,
        "$.items[?]",
        filter(where("id").nin(contextChanges.read("$.changes.items[*].id", List.class)))
      );
    } else {
      return getMapList(
        contextDB,
        "$.items[?]",
        filter(where("changed_ts").gt(lastUpdated).and("id").nin(contextChanges.read("$.changes.items[*].id", List.class)))
      );
    }
  }

  private List<Map<String, Object>> processTags(DocumentContext contextChanges, DocumentContext contextDB, Long lastUpdated) {
    // 1. get ids of elements that may have potentially changed in the db
    List<String> changedIds = getStringList(
      contextChanges,
      "$.changes.tags[*].id"
    );

    // 2. get those elements from the db - optimization: get from db only those elements that may have changed, not all
    List<Map<String, Object>> elementsToBeChangedInDB = getMapList(
      contextDB,
      "$.tags[?]",
      filter(where("id").in(changedIds))
    );

    // 3. delete the changes which are older than the same ones currently in db
    elementsToBeChangedInDB.forEach(dbElement -> contextChanges.delete("$.changes.tags[?]",
      filter(where("id").is(dbElement.get("id"))
        .and("changed_ts").lte(dbElement.get("changed_ts")))));

    // 4. delete in db whatever is left in list of changes
    contextDB.delete(
      "$.tags[?]",
      filter(where("id").in(contextChanges.read("$.changes.tags[*].id", List.class)))
    );

    // 5. add elements from changes - also adds elements which did not exist
    contextChanges.read("$.changes.tags[*]", List.class)
      .forEach(changedItem -> contextDB.add("$.tags", changedItem));

    if (lastUpdated == null) {
      // never synchronized before
      return getMapList(
        contextDB,
        "$.tags[?]",
        filter(where("id").nin(contextChanges.read("$.changes.tags[*].id", List.class)))
      );
    } else {
      return getMapList(
        contextDB,
        "$.tags[?]",
        filter(where("changed_ts").gt(lastUpdated).and("id").nin(contextChanges.read("$.changes.tags[*].id", List.class)))
      );
    }
  }

  public List<Map<String, Object>> processDeletions(DocumentContext contextChanges, DocumentContext contextDB, Long lastUpdated) {
    contextDB.delete(
      "$.items[?]",
      filter(where("id").in(contextChanges.read("$.changes.deletions[*].id", List.class)))
    );
    contextDB.delete(
      "$.tags[?]",
      filter(where("id").in(contextChanges.read("$.changes.deletions[*].id", List.class)))
    );

    if (lastUpdated == null) {
      // never synchronized before
      return getMapList(
        contextDB,
        "$.deletions[?]",
        filter(where("id").nin(contextChanges.read("$.changes.deletions[*].id", List.class)))
      );
    } else {
      return getMapList(
        contextDB,
        "$.deletions[?]",
        filter(where("changed_ts").gt(lastUpdated).and("id").nin(contextChanges.read("$.changes.deletions[*].id", List.class)))
      );
    }
  }

  private List<Map<String, Object>> getMapList(DocumentContext documentContext, String path, Filter filter) {
    try {
      return documentContext.read(path, List.class, filter);
    } catch (Exception exception) {
      log.debug("cannot read the path: {}", path);
      return Collections.emptyList();
    }
  }

  private List<Map<String, Object>> getMapList(DocumentContext documentContext, String path) {
    try {
      return documentContext.read(path, List.class);
    } catch (Exception exception) {
      log.debug("cannot read the path: {}", path);
      return Collections.emptyList();
    }
  }

  private List<String> getStringList(DocumentContext documentContext, String path, Filter filter) {
    try {
      return documentContext.read(path, List.class, filter);
    } catch (Exception exception) {
      log.debug("cannot read the path: {}", path);
      return Collections.emptyList();
    }
  }

  private List<String> getStringList(DocumentContext documentContext, String path) {
    try {
      return documentContext.read(path, List.class);
    } catch (Exception exception) {
      log.debug("cannot read the path: {}", path);
      return Collections.emptyList();
    }
  }
}
