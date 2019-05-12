package com.dodecaedro.filesyncserver.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.JsonArray;
import javax.json.JsonObject;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SyncResponse {
  boolean success;
  @JsonProperty("sync_ts")
  long syncTs;
  @JsonProperty("time_delta_ms")
  double timeDelta;
	JsonArray items;
	JsonArray tags;
  @JsonProperty("deletions_to_add")
	JsonArray deletionsToAdd;
}
