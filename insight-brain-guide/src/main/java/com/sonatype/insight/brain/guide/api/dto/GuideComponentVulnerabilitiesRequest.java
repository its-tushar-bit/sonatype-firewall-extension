/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

/**
 * Request object for fetching vulnerabilities affecting a specific component.
 */
public record GuideComponentVulnerabilitiesRequest(
    // Component identifier
    String purl,
    String format,
    String namespace,
    String name,
    String version,
    // Pagination
    Integer offset,
    Integer limit,
    // Sorting
    String sortField,
    String sortOrder,
    // Vulnerability filters
    List<String> severities,
    Double minCvss,
    Double maxCvss,
    Double minEpss,
    Double maxEpss,
    Boolean hasMalware,
    Boolean patchAvailable,
    List<String> cwes,
    Boolean exploitationKnown,
    String publishedWindow)
{
}
