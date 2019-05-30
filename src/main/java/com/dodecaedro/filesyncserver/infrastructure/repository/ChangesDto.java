package com.dodecaedro.filesyncserver.infrastructure.repository;

import lombok.Data;

import javax.json.JsonObject;
import java.util.List;

@Data
public class ChangesDto {
	private List<JsonObject> newItemsToSave;
	private List<JsonObject> itemsToUpdate;
	private List<JsonObject> newTagsToSave;
	private List<JsonObject> tagsToUpdate;
	private List<String> itemsIdsToDelete;
	private List<String> tagIdsToDelete;
	private List<JsonObject> newDeletions;
}
