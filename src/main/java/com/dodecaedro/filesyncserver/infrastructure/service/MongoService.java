package com.dodecaedro.filesyncserver.infrastructure.service;

import com.dodecaedro.filesyncserver.domain.model.OptimisticLockingException;
import com.dodecaedro.filesyncserver.domain.model.SyncResponse;
import com.dodecaedro.filesyncserver.infrastructure.repository.MongoRepository;
import org.springframework.stereotype.Service;

import javax.json.*;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MongoService {
	private final MongoRepository repository;

	private final Function<List<String>, JsonArray> findItemsFunction;
	private final Function<List<String>, JsonArray> findTagsFunction;
	private final Function<List<String>, JsonArray> findDeletionsFunction;

	public MongoService(MongoRepository repository) {
		this.repository = repository;

		findItemsFunction = repository::findItemsById;
		findTagsFunction = repository::findTagsById;
		findDeletionsFunction = repository::findDeletionsById;
	}

	public SyncResponse sync(JsonObject jsonObject) throws OptimisticLockingException {
		var lastSync = jsonObject.getJsonNumber("last_sync_ts").longValue();

		// process items
		var items = jsonObject.get("changes").asJsonObject().getJsonArray("items");
		var itemIds = process(items, findItemsFunction, repository::saveNewItems, repository::updateItems);

		// process tags
		var tags = jsonObject.get("changes").asJsonObject().getJsonArray("tags");
		var tagIds = process(tags, findTagsFunction, repository::saveNewTags, repository::updateTags);

		// deletions
		// 1. add to deletions
		// 2. delete from items/tags

		return SyncResponse.builder()
				.syncTs(Instant.now().getEpochSecond())
				.items(repository.findItemsNewerThanExcluding(lastSync, itemIds))
				.tags(repository.findTagsNewerThanExcluding(lastSync, tagIds))
				.success(true)
				.build();
	}

	public JsonObject findAllItemsTagsAndDeletions() {
		return Json.createObjectBuilder()
				.add("items", repository.findAllItems())
				.add("tags", repository.findAllTags())
				.add("deletions", repository.findAllDeletions())
				.build();
	}

	public void saveAllItemsTagsAndDeletions(JsonObject jsonObject) {
		repository.saveNewItems(jsonObject.getJsonArray("items"));
		repository.saveNewTags(jsonObject.getJsonArray("tags"));
		repository.saveNewDeletions(jsonObject.getJsonArray("deletions"));
	}

	public void deleteAllItemsTagsAndDeletions() {
		repository.deleteAllItems();
		repository.deleteAllTags();
		repository.deleteAllDeletions();
	}

	private List<String> process(
			JsonArray items,
			Function<List<String>, JsonArray> finder,
			Consumer<JsonArray> saver,
			Consumer<JsonArray> updater) throws OptimisticLockingException {

		var newItemsToSaveBuilder = Json.createArrayBuilder();
		var existingItemsToUpdateBuilder = Json.createArrayBuilder();

		var itemIds = items.stream()
				.map(jsonValue -> jsonValue.asJsonObject().get("id"))
				.map(JsonValue::toString)
				.collect(Collectors.toList());

		var existingItemsById = finder.apply(itemIds).stream()
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
			saver.accept(newItemsToSave);
		}

		var existingItemsToUpdate = existingItemsToUpdateBuilder.build();
		if (!existingItemsToUpdate.isEmpty()) {
			updater.accept(existingItemsToUpdate);
		}

		return itemIds;
	}
}
