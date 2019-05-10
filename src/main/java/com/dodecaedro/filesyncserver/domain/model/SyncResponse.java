package com.dodecaedro.filesyncserver.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.JsonArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
