package com.dodecaedro.filesyncserver.infrastructure.service;

import com.dodecaedro.filesyncserver.domain.model.OptimisticLockingException;
import com.dodecaedro.filesyncserver.infrastructure.repository.ChangesDto;
import com.dodecaedro.filesyncserver.infrastructure.repository.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

@Service
@Slf4j
public class FileSyncService {
	private final MongoRepository repository;

	public FileSyncService(MongoRepository repository) {
		this.repository = repository;
	}

	public JsonObject sync(JsonObject jsonObject) {
		if (log.isTraceEnabled()) {
			log.trace("request:");
			log.trace(jsonObject.toString());
		}

		var lastSync = 0L;
		var jsonValue = jsonObject.get("last_sync_ts");
		if (jsonValue != null && jsonValue.getValueType() == JsonValue.ValueType.NUMBER) {
			lastSync = ((JsonNumber)jsonValue).longValue();
		}

		var updatedTimestamp = Instant.now().getEpochSecond();

		// process tags
		var tags = jsonObject.get("changes").asJsonObject().getJsonArray("tags").getValuesAs(JsonObject.class);
		var tagIds = process(tags, repository::findTagsById, repository::saveNewTags, repository::updateTags);

		// process items
		var items = jsonObject.get("changes").asJsonObject().getJsonArray("items").getValuesAs(JsonObject.class);
		var itemIds = process(items, repository::findItemsById, repository::saveNewItems, repository::updateItems);

		// deletions
		// 1. add to deletions
		// 2. delete from items/tags
		var deletions = jsonObject.get("changes").asJsonObject().getJsonArray("deletions");
		processDeletions(deletions);

		var deletedItems = deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(element -> "i".equals(element.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList());
		repository.deleteItems(deletedItems);

		var deletedTags = deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(jsonObject1 -> "t".equals(jsonObject1.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList());
		repository.deleteTags(deletedTags);

		// remove tag from items


		var deletedSyncIds = deletions.stream()
				.map(JsonValue::asJsonObject)
				.map(deletionObject -> deletionObject.getString("sync_id"))
				.collect(toList());


		ChangesDto changes = new ChangesDto();
/*
		changes.setNewDeletions(jsonObject.get("changes")
				.asJsonObject()
				.getJsonArray("deletions")
				.stream()
				.map(JsonValue::asJsonObject)
				.collect(toList()));

		changes.setItemsIdsToDelete(deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(element -> "i".equals(element.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList()));

		changes.setTagIdsToDelete(deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(jsonObject1 -> "t".equals(jsonObject1.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList()));
 */

		JsonObject response = createObjectBuilder()
				.add("sync_ts", updatedTimestamp)
				.add("items", repository.findItemsNewerThanExcluding(lastSync, itemIds))
				.add("tags", repository.findTagsNewerThanExcluding(lastSync, tagIds))
				.add("deletions_to_add", repository.findDeletionsNewerThanExcluding(lastSync, deletedSyncIds))
				.add("success", true)
				.add("time_delta_ms", jsonObject.getJsonNumber("time_delta_ms").longValue())
				.build();

		if (log.isTraceEnabled()) {
			log.trace("response:");
			log.trace(response.toString());
		}

		return response;
	}

	private void addTagsToDto(ChangesDto changes, JsonArray tags) {
		var tagIds = tags.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		var existingTagsById = repository.findTagsById(tagIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));

		tags.stream()
				.map(JsonValue::asJsonObject)
				.forEach(tag -> {
					JsonObject existingTag = existingTagsById.get(tag.getString("id"));
					if (existingTag != null) {
						if (existingTag.getJsonNumber("changed_ts").longValue()
								> tag.getJsonNumber("changed_ts").longValue()) {
							log.error("Trying to overwrite a tag with title {} with an older version",
									existingTag.getString("title"));
							throw new OptimisticLockingException();
						}
						changes.getTagsToUpdate().add(tag);
					} else {
						changes.getNewTagsToSave().add(tag);
					}
				});
	}

	private void addItemsToDto(ChangesDto changes, JsonArray items) {
		var itemIds = items.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		var existingItemsById = repository.findItemsById(itemIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));

		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> {
					JsonObject existingItem = existingItemsById.get(item.getString("id"));
					if (existingItem != null) {
						if (existingItem.getJsonNumber("changed_ts").longValue()
								> item.getJsonNumber("changed_ts").longValue()) {
							log.error("Trying to overwrite an item with title {} with an older version",
									existingItem.getString("title"));
							throw new OptimisticLockingException();
						}
						changes.getItemsToUpdate().add(item);
					} else {
						changes.getNewItemsToSave().add(item);
					}
				});
	}


	public JsonObject findAllItemsTagsAndDeletions() {
		return createObjectBuilder()
				.add("items", repository.findAllItems())
				.add("tags", repository.findAllTags())
				.add("deletions", repository.findAllDeletions())
				.build();
	}

	public void saveAllItemsTagsAndDeletions(JsonObject jsonObject) {
		var items = Objects.requireNonNull(jsonObject.getJsonArray("items").getValuesAs(JsonObject.class));
		var tags = Objects.requireNonNull(jsonObject.getJsonArray("tags").getValuesAs(JsonObject.class));
		var deletions = Objects.requireNonNull(jsonObject.getJsonArray("deletions").getValuesAs(JsonObject.class));

		deleteAllItemsTagsAndDeletions();
		repository.saveNewItems(items);
		repository.saveNewTags(tags);
		repository.saveNewDeletions(deletions);
	}

	public void deleteAllItemsTagsAndDeletions() {
		repository.deleteAllItems();
		repository.deleteAllTags();
		repository.deleteAllDeletions();
	}

	private List<String> process(
			List<JsonObject> items,
			Function<List<String>, List<JsonObject>> finder,
			Consumer<List<JsonObject>> saver,
			Consumer<List<JsonObject>> updater) throws OptimisticLockingException {

		List<JsonObject> newItemsToSave = new ArrayList<>();
		List<JsonObject> existingItemsToUpdate = new ArrayList<>();

		var itemIds = items.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		var existingItemsById = finder.apply(itemIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));

		for (JsonValue item : items) {
			var itemObject = item.asJsonObject();
			var existingItem = existingItemsById.get(itemObject.getString("id"));
			if (existingItem != null) {
				if (itemObject.getInt("changed_ts") < existingItem.getInt("changed_ts")) {
					log.error("Trying to overwrite the item with title {} with an older version",
							existingItem.getString("title"));
					throw new OptimisticLockingException();
				}
				existingItemsToUpdate.add(itemObject);
			} else {
				newItemsToSave.add(itemObject);
			}
		}

		saver.accept(newItemsToSave);
		updater.accept(existingItemsToUpdate);

		return itemIds;
	}

	private void processDeletions(JsonArray deletions) {
		var deletedElementsIds = deletions.stream()
				.map(JsonValue::asJsonObject)
				.map(element -> element.getString("sync_id"))
				.collect(toList());

		var existingDeletionsById = repository.findDeletionsById(deletedElementsIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("sync_id"), identity()));

		// ignore already deleted elements
		var elementsToDelete = deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(jsonObject -> !existingDeletionsById.containsKey(jsonObject.getString("sync_id")))
				.collect(toList());

		repository.saveNewDeletions(elementsToDelete);
	}
}
