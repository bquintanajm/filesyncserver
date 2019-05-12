package com.dodecaedro.filesyncserver.infrastructure.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import javax.json.*;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static java.util.Objects.requireNonNull;

@Slf4j
@Repository
public class MongoRepository {
	private final MongoCollection<Document> itemsCollection;
	private final MongoCollection<Document> tagsCollection;
	private final MongoCollection<Document> deletionsCollection;

	public MongoRepository(MongoClient mongoClient) {
		requireNonNull(mongoClient, "mongo db client cannot be null");
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

	public JsonArray findItemsById(List<String> ids) {
		return findInCollectionById(itemsCollection, ids);
	}

	public JsonArray findItemsNewerThanExcluding(long timestamp, List<String> ids) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		itemsCollection.find().filter(and(gt("changed_ts", timestamp), not(in("id", ids)))).iterator()
				.forEachRemaining(document -> builder.add(document.toJson()));
		return builder.build();
	}

	public void saveNewItems(JsonArray items) {
		if (items != null) {
			itemsCollection.insertMany(toDocuments(items));
		}
	}
	public void saveNewTags(JsonArray tags) {
		if (tags != null) {
			tagsCollection.insertMany(toDocuments(tags));
		}
	}
	public void saveNewDeletions(JsonArray deletions) {
		if (deletions != null) {
			deletionsCollection.insertMany(toDocuments(deletions));
		}
	}

	public void updateItems(JsonArray items) {
		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> itemsCollection
						.replaceOne(eq("id", item.getString("id")), Document.parse(item.toString())));
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

	private JsonArray findInCollectionById(MongoCollection<Document> collection, List<String> ids) {
		if (log.isTraceEnabled()) {
			log.trace("Count of documents in the collection: {}", collection.countDocuments());
		}

		JsonArrayBuilder builder = Json.createArrayBuilder();
		collection.find().filter(in("id", ids)).iterator()
				.forEachRemaining(document -> builder.add(document.toJson()));
		return builder.build();
	}

	private JsonArray findAllByCollection(MongoCollection<Document> collection) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		collection.find().iterator()
				.forEachRemaining(document -> builder.add(fromDocument(document)));
		return builder.build();
	}

	private List<Document> toDocuments(JsonArray jsonArray) {
		return jsonArray.stream()
				.map(jsonObject -> Document.parse(jsonObject.toString()))
				.collect(Collectors.toList());
	}

	private JsonObject fromDocument(Document document) {
		document.remove("_id");
		JsonReader jsonReader = Json.createReader(new StringReader(document.toJson()));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();

		return object;
	}
}
