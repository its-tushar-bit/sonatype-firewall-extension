/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

public record GuideSecurityEventSearchRequest(
    String query,
    Integer offset,
    Integer limit,
    String sortField,
    String sortOrder,
    List<String> severities,
    List<String> threatTypes,
    Boolean knownExploited,
    List<String> affectedEcosystems)
{
  public GuideSecurityEventSearchRequest {
    offset = offset != null ? offset : 0;
    limit = limit != null ? Math.min(limit, 250) : 25;
    severities = severities != null ? List.copyOf(severities) : List.of();
    threatTypes = threatTypes != null ? List.copyOf(threatTypes) : List.of();
    affectedEcosystems = affectedEcosystems != null ? List.copyOf(affectedEcosystems) : List.of();
  }
}
