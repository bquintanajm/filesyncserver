package com.dodecaedro.filesyncserver.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  @Builder.Default
  List<Map<String, Object>> items = new ArrayList<>();
  @Builder.Default
  List<Map<String, Object>> tags = new ArrayList<>();
  @JsonProperty("deletions_to_add")
  @Builder.Default
  List<Map<String, Object>> deletionsToAdd = new ArrayList<>();
}
