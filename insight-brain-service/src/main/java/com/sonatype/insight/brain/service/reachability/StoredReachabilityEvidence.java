/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.reachability;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Server-side storage model for reachability evidence.
 * Mirrors the DTO structure but adds {@code component} (PURL) on method segments.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoredReachabilityEvidence(
    Map<String, VulnerabilityEvidence> evidence)
{
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record VulnerabilityEvidence(List<EvidencePath> paths, boolean truncated)
  {
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EvidencePath(List<PathSegment> segments)
  {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = MethodSegment.class, name = "method"),
    @JsonSubTypes.Type(value = GapSegment.class, name = "gap"),
    @JsonSubTypes.Type(value = ElidedSegment.class, name = "elided")
  })
  @JsonIgnoreProperties(ignoreUnknown = true)
  public sealed interface PathSegment
      permits MethodSegment, GapSegment, ElidedSegment
  {
  }

  /** A single method frame, enriched with component PURL. */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MethodSegment(String method, String filePath, String component)
      implements PathSegment
  {
  }

  /** Intra-jar elision — calls skipped with no boundary crossing. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GapSegment()
      implements PathSegment
  {
  }

  /** Methods skipped including boundary crossings. */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ElidedSegment(int count)
      implements PathSegment
  {
  }
}
