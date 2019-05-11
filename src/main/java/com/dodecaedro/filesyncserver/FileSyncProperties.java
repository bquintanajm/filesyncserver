package com.dodecaedro.filesyncserver;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "application")
@Validated
@Data
public class FileSyncProperties {
  @NotBlank
  private String key;
  @NotNull
  private String filePath;
  @NotNull
  private String fileName;
  @NotEmpty
	private String mongoHost = "mongo";
	private int mongoPort = 27017;
}
