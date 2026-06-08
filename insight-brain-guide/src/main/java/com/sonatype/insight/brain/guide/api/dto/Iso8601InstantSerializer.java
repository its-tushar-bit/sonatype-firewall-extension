/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

// Writes an Instant as an ISO-8601 string (e.g. "2020-11-06T21:03:29Z"). Needed because the
// ObjectMapper used by Jersey to render Guide responses does not register JavaTimeModule and
// would otherwise emit Instants as epoch-seconds decimals — a contract drift from Guide SaaS,
// which renders ISO-8601 strings. Paired with Iso8601InstantDeserializer for the inbound side.
// A future cleanup could register JavaTimeModule once globally and drop this class along with
// the @JsonSerialize / @JsonDeserialize annotations on Guide DTO Instant fields.
public class Iso8601InstantSerializer
    extends JsonSerializer<Instant>
{
  @Override
  public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    // Jackson dispatches null values through serializeAsNull() / @JsonInclude separately,
    // so this method is only invoked with a non-null Instant.
    gen.writeString(value.toString());
  }
}
