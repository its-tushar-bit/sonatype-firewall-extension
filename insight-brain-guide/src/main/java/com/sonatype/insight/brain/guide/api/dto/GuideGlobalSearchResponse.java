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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.SearchResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideGlobalSearchResponse(
    // DEDUCTION (no discriminator field) requires the subtypes to keep disjoint required-field
    // fingerprints: component -> format/name/version, vulnerability -> vulnId/summary, security
    // event -> eventId/title/overview. If a field rename or new subtype removes that disjointness,
    // Jackson 500s the whole /global-search response instead of skipping the unresolvable element.
    // SaaS doesn't emit a discriminator, so we can't switch to use=NAME without breaking contract
    // parity.
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION) @JsonSubTypes(
    {
      @JsonSubTypes.Type(value = GuideComponentDocument.class),
      @JsonSubTypes.Type(value = GuideVulnerabilityDocument.class),
      @JsonSubTypes.Type(value = GuideSecurityEventDocument.class)
    }) List<SearchResult> hits,
    long total,
    int offset,
    int limit,
    Map<String, Map<String, Long>> aggregations)
    implements ApiSearchResponse<SearchResult>{
  public GuideGlobalSearchResponse {
    hits = hits != null ? hits : List.of();
  }
}
