/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.githubapp;

/**
 * Wire shape for {@code GET /api/v2/githubApp}. The {@code relayLinkState} +
 * {@code relayLinkAttempts} fields surface the relay-side health of the App so the UI can
 * render a status badge (see {@code RelayLinkState} for the enum-as-string contract). Older
 * clients ignore the new fields; they default to {@code "UNREGISTERED"}/{@code 0} on the
 * server when the feature gate is off.
 */
public record ApiGitHubAppListDTO(
    String id,
    Integer appId,
    String slug,
    String githubOrganizationName,
    Long installationId,
    boolean isActive,
    String lastUpdatedAt,
    String installationUrl,
    String relayLinkState,
    int relayLinkAttempts)
{
}
