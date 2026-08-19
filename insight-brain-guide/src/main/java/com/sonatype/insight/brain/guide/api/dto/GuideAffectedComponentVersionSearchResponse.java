/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;

/**
 * Search response for affected component versions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideAffectedComponentVersionSearchResponse(
    @JsonDeserialize(contentAs = GuideAffectedComponentVersion.class) List<AffectedComponentVersion> hits,
    long total,
    int offset,
    int limit,
    Map<String, Map<String, Long>> aggregations)
    implements ApiSearchResponse<AffectedComponentVersion>
{
  public GuideAffectedComponentVersionSearchResponse {
    hits = hits != null ? hits : List.of();
  }
}
