/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.githubapp;

public record ApiGitHubAppListDTO(
    String id,
    Integer appId,
    String slug,
    String githubOrganizationName,
    Long installationId,
    boolean isActive,
    String lastUpdatedAt,
    String installationUrl)
{
}
