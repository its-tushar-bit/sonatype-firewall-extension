/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.Set;

/**
 * Authorization-resolved population for dashboard SQL metrics.
 *
 * @param ownerIds accessible owner closure used by hierarchy-applicable waiver counts
 * @param policyOwnerIds directly readable document owners used by policy counts
 */
public record ResolvedScope(
    Kind kind,
    DenyReason denyReason,
    Set<String> ownerIds,
    Set<String> policyOwnerIds,
    Set<String> organizationIds,
    Set<String> applicationIds,
    boolean requestFiltersApplied)
{
  public enum Kind
  {
    GLOBAL,
    RESTRICTED,
    DENY_ALL
  }

  public enum DenyReason
  {
    NO_ACCESS,
    RESOLUTION_FAILED
  }

  public ResolvedScope {
    ownerIds = Set.copyOf(ownerIds);
    policyOwnerIds = Set.copyOf(policyOwnerIds);
    organizationIds = Set.copyOf(organizationIds);
    applicationIds = Set.copyOf(applicationIds);
    if ((kind == Kind.DENY_ALL) != (denyReason != null)) {
      throw new IllegalArgumentException("denyReason must be present only for DENY_ALL");
    }
  }

  public static ResolvedScope denyAll(final DenyReason reason) {
    return new ResolvedScope(Kind.DENY_ALL, reason, Set.of(), Set.of(), Set.of(), Set.of(), false);
  }

  public boolean isQueryable() {
    return kind != Kind.DENY_ALL;
  }
}
