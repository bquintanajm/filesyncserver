package com.dodecaedro.filesyncserver;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FileSyncConfiguration {
	private final FileSyncProperties properties;

	public FileSyncConfiguration(FileSyncProperties properties) {
		this.properties = properties;
	}

	@Bean
	public MongoClient mongoClient() {
		return MongoClients.create(
				MongoClientSettings.builder()
						.applyToClusterSettings(builder ->
								builder.hosts(List.of(new ServerAddress(
										properties.getMongoHost(),
										properties.getMongoPort()
								))))
						.build());
	}
}
