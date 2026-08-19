/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sonatype.guide.api.dto.SecurityEventDocument;

/**
 * Security event list/search document. Field names match the {@code security-events-v1} index
 * wire shape returned by the search service, satisfying the contract accessors directly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideSecurityEventDocument(
    String eventId,
    String title,
    String overview,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant publishedDate,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant lastUpdatedDate,
    String eventSeverityCategory,
    String eventThreatType,
    Boolean isKnownExploited)
    implements SecurityEventDocument
{
}
