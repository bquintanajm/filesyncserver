package com.dodecaedro.filesyncserver.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SyncResponse {
	boolean success;
	@JsonProperty("sync_ts")
	long syncTs;
	@Builder.Default
	List<Map<String, Object>> items = new ArrayList<>();
	@Builder.Default
	List<Map<String, Object>> tags = new ArrayList<>();
	@JsonProperty("deletions_to_add")
	@Builder.Default
	List<Map<String, Object>> deletionsToAdd = new ArrayList<>();
}
