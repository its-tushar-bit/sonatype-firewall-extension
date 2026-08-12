/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import org.apache.commons.lang3.StringUtils;

/**
 * Derives the deterministic public-id string used to look up the {@code Application} that
 * represents a hosted repository. The id is the sanitized repository pathname, truncated to
 * {@value #MAX_PUBLIC_ID_LENGTH} characters. Called from
 * {@link com.sonatype.insight.brain.api.v2.ApiRepositoryComponentsService},
 * {@link com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService}, and
 * {@link com.sonatype.insight.brain.api.v2.ApiCrossStageViolationService}.
 */
public final class ApplicationForHostedRepositoryComponentService
{
  // Must match ApplicationDAO.MAX_PUBLIC_ID_LENGTH and the application.public_id VARCHAR(200) column.
  private static final int MAX_PUBLIC_ID_LENGTH = 200;

  private ApplicationForHostedRepositoryComponentService() {
    // Static-only utility; not intended to be instantiated.
  }

  public static String generatePublicId(final String repositoryPublicId, final String pathname) {
    String sanitized = pathname != null
        ? pathname.replaceAll("[^a-zA-Z0-9\\-._]", "_")
        : "unknown";
    String repoPrefix = repositoryPublicId != null
        ? repositoryPublicId.replaceAll("[^a-zA-Z0-9\\-._]", "_")
        : "repo";
    String publicId = repoPrefix + "_" + sanitized;
    if (publicId.length() <= MAX_PUBLIC_ID_LENGTH) {
      return publicId;
    }
    // Preserve a fixed repo prefix slice for uniqueness; fill remainder with pathname tail.
    // Split budget: half to repo prefix (min 10), half to pathname.
    int prefixBudget = Math.max(10, MAX_PUBLIC_ID_LENGTH / 2);
    String truncatedPrefix = StringUtils.left(repoPrefix, prefixBudget);
    int remaining = MAX_PUBLIC_ID_LENGTH - truncatedPrefix.length() - 1; // -1 for separator
    return truncatedPrefix + "_" + StringUtils.right(sanitized, remaining);
  }
}
