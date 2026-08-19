/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

/**
 * Request object for searching components.
 */
public record GuideComponentSearchRequest(
    String query,
    Integer offset,
    Integer limit,
    String sortField,
    String sortOrder,
    List<String> formats,
    List<String> categories,
    List<String> severities,
    Double minCvss,
    Double maxCvss,
    Double minEpss,
    Double maxEpss,
    List<String> licenseFamilies,
    List<String> licenses,
    Integer minVersionScore,
    Integer maxVersionScore,
    String latestStable,
    String publishedWindow,
    Boolean hasMalware,
    Integer minDocCount)
{
}
