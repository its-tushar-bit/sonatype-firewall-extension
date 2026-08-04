/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sonatype.guide.api.dto.SecurityEventDetailDocument;

/**
 * Detailed security event document. Extends the list fields with long-form markdown, the blog URL,
 * reference/classification arrays, and the affected-ecosystem and component-count fields. Field
 * names match the {@code security-events-v1} index wire shape exactly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideSecurityEventDetailDocument(
    String eventId,
    String title,
    String overview,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant publishedDate,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant lastUpdatedDate,
    String eventSeverityCategory,
    String eventThreatType,
    Boolean isKnownExploited,
    String detail,
    String guidance,
    String sonatypeBlogUrl,
    List<String> advisoryReferenceIds,
    List<String> cwes,
    List<String> malwareThreatTypes,
    List<String> malwareAttackVectors,
    List<String> affectedEcosystems,
    Integer affectedComponentVersionsCount)
    implements SecurityEventDetailDocument
{
}
