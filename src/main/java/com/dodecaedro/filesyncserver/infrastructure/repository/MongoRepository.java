package com.dodecaedro.filesyncserver.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.bson.Document.parse;

@Slf4j
@Repository
public class MongoRepository {
	private final MongoCollection<Document> itemsCollection;
	private final MongoCollection<Document> tagsCollection;
	private final MongoCollection<Document> deletionsCollection;

	private final MongoClient mongoClient;

	private final ObjectMapper objectMapper;

	public MongoRepository(MongoClient mongoClient, ObjectMapper objectMapper) {
		this.objectMapper = requireNonNull(objectMapper);
		this.mongoClient = requireNonNull(mongoClient, "mongo db client cannot be null");

		MongoDatabase database = mongoClient.getDatabase("mydb");

		itemsCollection = database.getCollection("items");
		tagsCollection = database.getCollection("tags");
		deletionsCollection = database.getCollection("deletion");

		itemsCollection.createIndex(Indexes.ascending("id"));
	}

	public JsonArray findAllItems() {
		return findAllByCollection(itemsCollection);
	}

	public JsonArray findAllTags() {
		return findAllByCollection(tagsCollection);
	}

	public JsonArray findAllDeletions() {
		return findAllByCollection(deletionsCollection);
	}

	public List<JsonObject> findItemsById(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return findInCollectionById(itemsCollection, ids);
	}

	public List<JsonObject> findTagsById(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return findInCollectionById(tagsCollection, ids);
	}

	public JsonArray findItemsNewerThanExcluding(long timestamp, List<String> ids) {
		var builder = Json.createArrayBuilder();
		itemsCollection.find()
				.filter(or(
						gt("changed_ts", timestamp),
						gt("title_ts", timestamp),
						gt("created_on", timestamp)
				))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public JsonArray findTagsNewerThanExcluding(long timestamp, List<String> ids) {
		var builder = Json.createArrayBuilder();
		tagsCollection.find()
				.filter(or(
						gt("changed_ts", timestamp),
						gt("created_on", timestamp)
				))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public JsonArray findDeletionsNewerThanExcluding(long timestamp, List<String> ids) {
		var builder = Json.createArrayBuilder();
		deletionsCollection.find()
				//.filter(and(gt("ts", timestamp), not(in("sync_id", ids))))
				.filter(and(gt("ts", timestamp)))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public void saveNewItems(List<JsonObject> items) {
		if (items != null && !items.isEmpty()) {
			itemsCollection.insertMany(toDocuments(items));
		}
	}
	public void saveNewTags(List<JsonObject> tags) {
		if (tags != null && !tags.isEmpty()) {
			tagsCollection.insertMany(toDocuments(tags));
		}
	}
	public void saveNewDeletions(List<JsonObject> deletions) {
		if (deletions != null && !deletions.isEmpty()) {
			deletionsCollection.insertMany(toDocuments(deletions));
		}
	}

	public void updateItems(List<JsonObject> items) {
		if (items == null || items.isEmpty()) {
			return;
		}

		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> itemsCollection
						.replaceOne(eq("id", item.getString("id")), parse(item.toString())));
	}

	public void updateTags(List<JsonObject> tags) {
		if (tags == null || tags.isEmpty()) {
			return;
		}

		tags.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> tagsCollection
						.replaceOne(eq("id", item.getString("id")), parse(item.toString())));
	}

	public void deleteAllItems() {
		itemsCollection.deleteMany(new Document());
	}

	public void deleteAllTags() {
		tagsCollection.deleteMany(new Document());
	}

	public void deleteAllDeletions() {
		deletionsCollection.deleteMany(new Document());
	}

	public void deleteItems(List<String> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}

		itemsCollection.deleteMany(in("id", itemIds));
	}

	public void deleteTags(List<String> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}

		tagsCollection.deleteMany(in("id", tagIds));
	}

	public JsonArray findDeletionsById(List<String> deletionIds) {
		if (deletionIds == null || deletionIds.isEmpty()) {
			return JsonValue.EMPTY_JSON_ARRAY;
		}

		if (log.isTraceEnabled()) {
			log.trace("Count of documents in the deletion collection: {}", deletionsCollection.countDocuments());
		}

		JsonArrayBuilder builder = Json.createArrayBuilder();
		deletionsCollection.find()
				.filter(in("sync_id", deletionIds))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	private List<JsonObject> findInCollectionById(MongoCollection<Document> collection, List<String> ids) {
		return collection.find()
				.filter(in("id", ids))
				.map(this::fromDocument)
				.into(new ArrayList<>());
	}

	private JsonArray findAllByCollection(MongoCollection<Document> collection) {
		var builder = Json.createArrayBuilder();
		collection.find()
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	private List<Document> toDocuments(List<JsonObject> jsonArray) {
		return jsonArray.stream()
				.map(jsonObject -> parse(jsonObject.toString()))
				.collect(toList());
	}

	private JsonObject fromDocument(Document document) {
		document.remove("_id");
		try {
			return objectMapper.readValue(document.toJson(), JsonObject.class);
		} catch (IOException e) {
			return JsonValue.EMPTY_JSON_OBJECT;
		}
	}

	private void sync() {
		try (ClientSession clientSession = mongoClient.startSession()) {
			clientSession.startTransaction();
			//collection.insertOne(clientSession, docOne);
			//collection.insertOne(clientSession, docTwo);
			clientSession.commitTransaction();
		}

	}
}
