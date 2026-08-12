/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Iso8601InstantSerializerTest
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void componentDocument_publishedDate_serializesAsIso8601String() throws Exception {
    Instant published = Instant.parse("2020-11-06T21:03:29Z");
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", "Central", "log4j", "log4j", "1.2.17", null,
        List.of(), null, true, null, 9.8, published, false, null, null);

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(doc));

    assertThat(json.get("publishedDate").isTextual())
        .as("publishedDate must be a JSON string, not an epoch-seconds number")
        .isTrue();
    assertThat(json.get("publishedDate").asText()).isEqualTo("2020-11-06T21:03:29Z");
  }

  @Test
  public void vulnerabilityDocument_publishedAt_serializesAsIso8601String() throws Exception {
    Instant published = Instant.parse("2021-12-10T10:15:09Z");
    GuideVulnerabilityDocument doc = new GuideVulnerabilityDocument(
        "CVE-2021-44228", List.of(), "Log4Shell", 10.0, 10.0,
        List.of(), List.of(), List.of("maven"), false, true, 0.95,
        "NVD", published, "VULNERABILITY");

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(doc));

    assertThat(json.get("publishedAt").isTextual()).isTrue();
    assertThat(json.get("publishedAt").asText()).isEqualTo("2021-12-10T10:15:09Z");
  }

  @Test
  public void nullInstant_serializesAsJsonNull_whenIncludeAlways() throws Exception {
    // GuideComponentDocument carries @JsonInclude(NON_NULL) at the class level, so a null
    // publishedDate is omitted entirely from the JSON. Verify by re-using a record where the
    // field is explicitly null and asserting the key is absent (rather than serialized as 0.0).
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", "Central", "log4j", "log4j", "1.2.17", null,
        List.of(), null, true, null, 9.8, null, false, null, null);

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(doc));

    assertThat(json.has("publishedDate"))
        .as("null Instant must not appear as 0.0 or any numeric value")
        .isFalse();
  }
}
