package com.dodecaedro.filesyncserver.infrastructure.service;

import com.dodecaedro.filesyncserver.domain.model.SyncResponse;
import com.dodecaedro.filesyncserver.infrastructure.repository.MongoRepository;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongoService {
	private final MongoRepository repository;

	public MongoService(MongoRepository repository) {
		this.repository = repository;
	}

	public SyncResponse sync(JsonObject jsonObject) throws OptimisticLockingException {
		var lastSync = jsonObject.getString("last_sync_ts");

		var items = jsonObject.get("changes").asJsonObject().getJsonArray("items");
		List<String> itemIds = processItems(items);

		return SyncResponse.builder()
				.syncTs(Instant.now().getEpochSecond())
				.items(repository.findItemsNewerThanExcluding(lastSync, itemIds))
				.success(true)
				.build();
	}

	private List<String> processItems(JsonArray items) throws OptimisticLockingException {
		var newItemsToSaveBuilder = Json.createArrayBuilder();
		var existingItemsToUpdateBuilder = Json.createArrayBuilder();

		var itemIds = items.stream()
				.map(jsonValue -> jsonValue.asJsonObject().get("id"))
				.map(JsonValue::toString)
				.collect(Collectors.toList());

		var existingItemsById = repository.findItemsById(itemIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(Collectors.toMap(object -> object.get("id"), Function.identity()));

		for (JsonValue item : items) {
			var itemObject = item.asJsonObject();
			var existingItem = existingItemsById.get(itemObject.get("id"));
			if (existingItem != null) {
				if (itemObject.getInt("changed_ts") < existingItem.getInt("changed_ts")) {
					throw new OptimisticLockingException();
				}
				itemObject.put("_id", existingItem.get("_id"));
				existingItemsToUpdateBuilder.add(itemObject);
			} else {
				newItemsToSaveBuilder.add(itemObject);
			}
		}

		var newItemsToSave = newItemsToSaveBuilder.build();
		if (!newItemsToSave.isEmpty()) {
			repository.saveNewItems(newItemsToSave);
		}

		var existingItemsToUpdate = existingItemsToUpdateBuilder.build();
		if (!existingItemsToUpdate.isEmpty()) {
			repository.updateItems(existingItemsToUpdate);
		}

		return itemIds;
	}
}
