package com.dodecaedro.filesyncserver;

import com.dodecaedro.filesyncserver.infrastructure.service.FileStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileSyncServerApplicationTests {
	@Test
	public void contextLoads() throws Exception {
		FileSyncProperties properties = new FileSyncProperties();
		properties.setFilePath("/home/jm/development/filesyncserver/src/test/resources");
		properties.setFileName("file.json");

		FileStorageService fileStorageService = new FileStorageService(properties);
		fileStorageService.syncFile(FileSyncServerApplicationTests.class.getResourceAsStream("/changes.json"));
	}
}
