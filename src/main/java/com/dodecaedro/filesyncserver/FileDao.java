package com.dodecaedro.filesyncserver;

import com.dodecaedro.filesyncserver.domain.model.SyncResponse;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

@Slf4j
@Repository
public class FileDao {
    private final FileSyncProperties properties;

    public FileDao(FileSyncProperties properties) {
        this.properties = properties;
    }

    public File getFile() {
        return new File(properties.getFilePath());
    }

    public void saveFile(InputStream inputStream) throws IOException {
        StreamUtils.copy(inputStream, new FileOutputStream(new File(properties.getFilePath())));
    }

    public SyncResponse syncFile(InputStream inputStream) throws IOException {
        DocumentContext contextChanges = JsonPath.parse(inputStream);
        DocumentContext contextDB = JsonPath.parse(getFile());

        log.debug(contextChanges.jsonString());

        // 1. get ids of elements that may have potentially changed in the db
        List<String> changedIds = contextChanges.read("$.changes.items[?].id", filter(where("is_existing_item").is(true)));

        // 2. get those elements from the db - optimization: get from db only those elements that may have changed, not all
        List<Map<String, Object>> elementsToBeChangedInDB =
            contextDB.read("$.items[?]", filter(where("id").in(changedIds)));

        // 3. delete the changes which are older than the same ones currently in db
        elementsToBeChangedInDB.forEach(dbElement -> contextChanges.delete("$.changes.items[?]",
            filter(where("id").is(dbElement.get("id"))
                .and("changed_ts").lte(dbElement.get("changed_ts")))));

        // 4. delete in db whatever is left in list of changes
        contextDB.delete("$.items[?]", filter(where("id").in(contextChanges.read("$.changes.items[*].id", List.class))));

        // 5. add elements from changes - also adds elements which did not exist
        contextChanges.read("$.changes.items[*]", List.class)
            .forEach(changedItem -> contextDB.add("$.items", changedItem));

        // 6. save file - todo: the whole file is in memory
        StreamUtils.copy(contextDB.jsonString(), Charset.defaultCharset(), new FileOutputStream(new File(properties.getFilePath())));

        // 7. get newer elements since the update - return only those which are not in changes?
        int lastUpdated = contextChanges.read("$.last_sync_ts");

        SyncResponse response = SyncResponse.builder()
            .success(true)
            .syncTs(System.currentTimeMillis())
            .items(contextDB.read("$.items[?]", List.class,
                filter(where("changed_ts").gt(lastUpdated).and("id").nin(contextChanges.read("$.changes.items[*].id", List.class)))))
            .build();

        try {
            response.setDeletionsToAdd(contextDB.read("$.deletions[?]", List.class, filter(where("ts").gt(lastUpdated))));
        } catch (PathNotFoundException exception) {
            // do nothing
        }

        return response;
    }
}
