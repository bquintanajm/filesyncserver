package com.dodecaedro.filesyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;

import javax.json.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DaoIT {

	public static GenericContainer mongoContainer = new GenericContainer("mongo:4.1.10")
			.withEnv()
			.withExposedPorts(27017);

	@BeforeAll
	public static void containerStart() {
		// teardown done automatically on jvm exit
		mongoContainer.start();
	}

	void test() {
		MongoClient mongoClient = MongoClients.create(
				MongoClientSettings.builder()
						.applyToClusterSettings(builder ->
								builder.hosts(List.of(new ServerAddress(
										mongoContainer.getContainerIpAddress(),
										mongoContainer.getFirstMappedPort()
								))))
						.credential()
						.build());

		MongoDatabase database = mongoClient.getDatabase("mydb");
		MongoCollection<Document> collection = database.getCollection("items");
		collection.createIndex(Indexes.ascending("id"));

		JsonReader jsonReader = Json.createReader(DaoIT.class.getResourceAsStream("/file.json"));
		JsonObject jsonObject = jsonReader.readObject();

		List<Document> itemDocuments = toDocuments(jsonObject.getJsonArray("items"));
		collection.insertMany(itemDocuments);

		Document myDoc = collection.find().first();

		assertThat(myDoc).isNotNull();

		FindIterable<Document> documents = collection.find().filter(in("id",
				"6E1CD29CF516485B98B9A22634B48A78", "1B01FDC3028240EA973DEA35A261B14E"
		));

		assertThat(documents).hasSize(2);

		mongoClient.close();
	}

	@Test
	void serialize() throws FileNotFoundException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonReader jsonReader = Json.createReader(DaoIT.class.getResourceAsStream("/file.json"));
		JsonObject jsonObject = jsonReader.readObject();

		JsonWriter writer = Json.createWriter(new FileOutputStream(new File("/tmp/file.json")));
		writer.writeObject(jsonObject);
	}

	private List<Document> toDocuments(JsonArray jsonArray) {
		return jsonArray.stream()
				.map(jsonObject -> Document.parse(jsonObject.toString()))
				.collect(Collectors.toList());
	}
}
