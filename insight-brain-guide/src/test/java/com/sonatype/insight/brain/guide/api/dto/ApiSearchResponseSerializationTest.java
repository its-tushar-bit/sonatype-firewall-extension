/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSearchResponseSerializationTest
{

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void serializesWithCorrectFieldNames() throws Exception {
    ApiSearchResponse<String> response = new GuideSearchResponse<>(
        List.of("a", "b"), 2, 0, 20, null);

    JsonNode json = objectMapper.valueToTree(response);

    assertThat(json.get("hits").isArray()).isTrue();
    assertThat(json.get("hits").size()).isEqualTo(2);
    assertThat(json.get("total").asLong()).isEqualTo(2L);
    assertThat(json.get("offset").asInt()).isEqualTo(0);
    assertThat(json.get("limit").asInt()).isEqualTo(20);
    assertThat(json.has("aggregations")).isFalse();
  }

  @Test
  public void serializesAggregationsWhenPresent() throws Exception {
    Map<String, Map<String, Long>> aggregations = Map.of(
        "format", Map.of("maven", 10L, "npm", 5L));
    ApiSearchResponse<String> response = new GuideSearchResponse<>(
        List.of(), 0, 0, 20, aggregations);

    JsonNode json = objectMapper.valueToTree(response);

    assertThat(json.get("aggregations").get("format").get("maven").asLong()).isEqualTo(10L);
    assertThat(json.get("aggregations").get("format").get("npm").asLong()).isEqualTo(5L);
  }

  @Test
  public void omitsNullFieldsFromJson() throws Exception {
    ApiSearchResponse<String> response = new GuideSearchResponse<>(
        List.of(), 0, 0, 20, null);

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).doesNotContain("aggregations");
  }
}
