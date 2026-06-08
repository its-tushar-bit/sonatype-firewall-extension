/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

/**
 * Request parameters for vulnerability affected components endpoint.
 *
 * @param id Vulnerability ID (CVE or Sonatype ID)
 * @param query Search query
 * @param offset Pagination offset
 * @param limit Pagination limit
 * @param sortField Field to sort by
 * @param sortOrder Sort order (asc/desc)
 */
public record GuideAffectedComponentVersionRequest(
    String id,
    String query,
    Integer offset,
    Integer limit,
    String sortField,
    String sortOrder)
{
}
