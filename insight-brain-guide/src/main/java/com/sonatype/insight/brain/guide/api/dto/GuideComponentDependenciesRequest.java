/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

/**
 * Request object for fetching dependencies of a specific component.
 */
public record GuideComponentDependenciesRequest(
    // Component identifier
    String purl,
    String format,
    String namespace,
    String name,
    String version,
    // Filter query
    String query,
    // Pagination
    Integer offset,
    Integer limit,
    // Sorting
    String sortField,
    String sortOrder,
    // Dependency filters
    List<String> formats,
    List<String> categories,
    List<String> severities,
    Double minCvss,
    Double maxCvss,
    Integer minVersionScore,
    Integer maxVersionScore,
    List<String> licenseFamilies,
    List<String> licenses,
    String latestStable)
{
}
