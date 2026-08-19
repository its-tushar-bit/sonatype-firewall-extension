/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultRowTest
{
  @Test
  public void constructor_dropsNullValuedEntries_matchesBuilderBehaviour() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("kept", "value");
    fields.put("dropped", null);
    fields.put("alsoKept", 42);

    ResultRow row = new ResultRow("APPLICATION", SearchSource.LOCAL.value(), "id-1", "title", null, fields, null);

    assertThat(row.getFields()).containsKeys("kept", "alsoKept");
    assertThat(row.getFields()).doesNotContainKey("dropped");
  }

  @Test
  public void builder_dropsNullValuedFields() {
    ResultRow row = ResultRow.builder()
        .type("APPLICATION")
        .source(SearchSource.LOCAL.value())
        .id("id-1")
        .title("title")
        .field("kept", "v")
        .field("dropped", null)
        .build();

    assertThat(row.getFields()).containsKey("kept");
    assertThat(row.getFields()).doesNotContainKey("dropped");
  }
}
