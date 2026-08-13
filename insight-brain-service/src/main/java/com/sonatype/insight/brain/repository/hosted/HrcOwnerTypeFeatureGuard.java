/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import jakarta.ws.rs.NotAuthorizedException;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

/**
 * Runtime feature check for polymorphic-path resources that accept multiple {@link OwnerType}s
 * — {@code ComponentLabelResource}, {@code ApiLicenseOverrideResource}, and
 * {@code ComponentInfoResource}. Class-level {@code @HasFeature(HOSTED_REPOSITORY_EVALUATION)}
 * cannot be used on those resources because they also serve application/organization/repository
 * paths, and gating the whole class would break non-HRC customers.
 * <p>
 * Every dedicated HRC resource (six classes in CLM-44276) already carries class-level
 * {@code @HasFeature(HOSTED_REPOSITORY_EVALUATION)}. This helper preserves that invariant on the
 * three polymorphic resources by throwing when the caller resolves to
 * {@link OwnerType#HOSTED_REPOSITORY_COMPONENT} and the feature is disabled — avoiding stale-access
 * disclosure of HRC data after a customer toggles the flag off.
 * <p>
 * Mirrors the existing pattern at
 * {@code HostedComponentResource.java:91}.
 */
public final class HrcOwnerTypeFeatureGuard
{
  private HrcOwnerTypeFeatureGuard() {
  }

  /**
   * No-op when {@code ownerType} is anything other than {@link OwnerType#HOSTED_REPOSITORY_COMPONENT}.
   * When it is HRC and the {@code HOSTED_REPOSITORY_EVALUATION} feature is disabled, throws
   * {@link NotAuthorizedException} with the same message every other HRC endpoint returns.
   */
  public static void requireHrcFeatureIfHrc(final OwnerType ownerType) {
    if (ownerType == OwnerType.HOSTED_REPOSITORY_COMPONENT
        && !SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled())
    {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.getId() + " feature is disabled");
    }
  }
}
