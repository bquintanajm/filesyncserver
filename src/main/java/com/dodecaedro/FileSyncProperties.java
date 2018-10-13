package com.dodecaedro;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class FileSyncProperties {
	@NonNull
	private String key;
	@NonNull
	private String filePath;
	@NonNull
	private String fileName;
}
