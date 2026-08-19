/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

/**
 * Request parameters for component versions endpoint.
 *
 * @param purl Package URL
 * @param extension Artifact extension (e.g. "jar", "whl"); {@code null} or blank means "no filter"
 * @param classifier Artifact classifier (e.g. "sources", "javadoc"); {@code null} or blank means "no filter"
 * @param offset Pagination offset
 * @param limit Pagination limit
 * @param sortField Field to sort by
 * @param sortOrder Sort order (asc/desc)
 * @param severities Filter by severities
 * @param minCvss Minimum CVSS score
 * @param maxCvss Maximum CVSS score
 * @param minVersionScore Minimum version score
 * @param maxVersionScore Maximum version score
 * @param versionQuery Query string for version names
 * @param publishedWindow Time window for publication date
 * @param hasMalware Filter for malware
 * @param isStable Filter for stable versions
 */
public record GuideComponentVersionsRequest(
    String purl,
    String extension,
    String classifier,
    Integer offset,
    Integer limit,
    String sortField,
    String sortOrder,
    List<String> severities,
    Double minCvss,
    Double maxCvss,
    Integer minVersionScore,
    Integer maxVersionScore,
    String versionQuery,
    String publishedWindow,
    Boolean hasMalware,
    Boolean isStable)
{
}
