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
import com.sonatype.guide.api.dto.ComponentArtifact;
import com.sonatype.guide.api.dto.Refid;

/**
 * Concrete implementation of ComponentArtifact for Jackson deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideComponentArtifact(
    String extension,
    String classifier,
    String sha1,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant publishedDate,
    @JsonDeserialize(contentAs = GuideRefid.class) List<? extends Refid> refids)
    implements ComponentArtifact
{
}
