/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Parses an ISO-8601 string into an Instant. Needed because the JsonUtils ObjectMapper used by
// HdsClient does not register JavaTimeModule. Paired with Iso8601InstantSerializer for the
// outbound side. A future cleanup could register JavaTimeModule once in JsonUtils and drop both
// classes along with the @JsonDeserialize / @JsonSerialize annotations on Guide DTO Instant
// fields.
public class Iso8601InstantDeserializer
    extends JsonDeserializer<Instant>
{
  private static final Logger log = LoggerFactory.getLogger(Iso8601InstantDeserializer.class);

  @Override
  public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getValueAsString();
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    }
    catch (DateTimeParseException e) {
      log.debug("Failed to parse Instant from: {}", value);
      return null;
    }
  }
}
