package com.dodecaedro.filesyncserver.infrastructure.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.json.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static java.util.Objects.requireNonNull;

public class MongoRepository {
	private final MongoClient mongoClient;
	private MongoCollection<Document> itemsCollection;
	private MongoCollection<Document> tagsCollection;
	private MongoCollection<Document> deletionsCollection;

	public MongoRepository(MongoClient mongoClient) {
		this.mongoClient = requireNonNull(mongoClient, "mongo db client cannot be null");
		MongoDatabase database = mongoClient.getDatabase("mydb");

		itemsCollection = database.getCollection("items");
		tagsCollection = database.getCollection("tags");
		deletionsCollection = database.getCollection("deletion");


	}

	public JsonArray findItemsById(List<String> ids) {
		return findInCollectionById(itemsCollection, ids);
	}

	public JsonArray findItemsNewerThanExcluding(String timestamp, List<String> ids) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		itemsCollection.find().filter(and(gt("changed_ts", timestamp), not(in("id", ids)))).iterator()
				.forEachRemaining(document -> builder.add(document.toJson()));
		return builder.build();
	}

	public void saveNewItems(JsonArray items) {
		itemsCollection.insertMany(toDocuments(items));
	}

	public void updateItems(JsonArray items) {
		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> itemsCollection
						.replaceOne(eq("id", item.getString("id")), Document.parse(item.toString())));
	}

	private JsonArray findInCollectionById(MongoCollection<Document> collection, List<String> ids) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		collection.find().filter(in("id", ids)).iterator()
				.forEachRemaining(document -> builder.add(document.toJson()));
		return builder.build();
	}

	private List<Document> toDocuments(JsonArray jsonArray) {
		return jsonArray.stream()
				.map(jsonObject -> Document.parse(jsonObject.toString()))
				.collect(Collectors.toList());
	}
}
