package com.dodecaedro.filesyncserver;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonTest {
	@Test
	public void name() {
		DocumentContext contextChanges = JsonPath.parse(JsonTest.class.getResourceAsStream("/changes1.json"));
		DocumentContext contextDB = JsonPath.parse(JsonTest.class.getResourceAsStream("/db.json"));

		// 1. get ids of elements that may have potentially changed in the db
		List<String> changedIds = contextChanges.read("$.changes.items[?].id", filter(where("is_existing_item").is(true)));
		assertThat(changedIds).containsOnly(
				"C8ED885585334715BB6F7A4754455F23", "C8ED885585334715BB6F7A4754455F24", "C8ED885585334715BB6F7A4754455F25"
		);

		// 2. get those elements from the db - optimization: get from db only those elements that may have changed, not all
		List<Map<String, Object>> elementsToBeChangedInDB =
				contextDB.read("$.items[?]", filter(where("id").in(changedIds)));
		assertThat(elementsToBeChangedInDB).hasSize(3);

		// 3. delete the changes which are older than the same ones currently in db
		elementsToBeChangedInDB.forEach(dbElement -> contextChanges.delete("$.changes.items[?]",
				filter(where("id").is(dbElement.get("id"))
						.and("changed_ts").lte(dbElement.get("changed_ts")))));
		assertThat(contextChanges.read("$.changes.items[?].id", List.class, filter(where("is_existing_item").is(true))))
				.containsOnly(
				"C8ED885585334715BB6F7A4754455F23", "C8ED885585334715BB6F7A4754455F24"
		);

		// 4. delete in db whatever is left in list of changes
		contextDB.delete("$.items[?]", filter(where("id").in(contextChanges.read("$.changes.items[*].id", List.class))));
		assertThat(contextDB.read("$.items[*].id", List.class))
				.containsOnly(
				"C8ED885585334715BB6F7A4754455F25", "C8ED885585334715BB6F7A4754455F27"
		);

		// 5. add elements from changes - also adds elements which did not exist
		contextChanges.read("$.changes.items[*]", List.class)
				.forEach(changedItem -> contextDB.add("$.items", changedItem));
		assertThat(contextDB.read("$.items[*].id", List.class))
				.containsOnly(
				"C8ED885585334715BB6F7A4754455F23",
				"C8ED885585334715BB6F7A4754455F24",
				"C8ED885585334715BB6F7A4754455F25",
				"C8ED885585334715BB6F7A4754455F26",
				"C8ED885585334715BB6F7A4754455F27"
		);

		// 6. get newer elements since the update - return only those which are not in changes?
		int lastUpdated = contextChanges.read("$.last_sync_ts");
		assertThat(lastUpdated).isEqualTo(1537113620);
		List<Map<String, Object>> newElementsInDB =
				contextDB.read("$.items[?]", List.class,
						filter(where("changed_ts").gt(lastUpdated).and("id").nin(contextChanges.read("$.changes.items[*].id", List.class))));
		assertThat(newElementsInDB).hasSize(2);

	}
}
