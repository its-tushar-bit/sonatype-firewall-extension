/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Set;

/**
 * Canonical vocabulary for the denormalized {@code policyWaiverExpiryStatus} keyword written at
 * index time and accepted by the WAIVER {@code expiryStatus} index-query filter.
 * <p>
 * Lowercase on purpose: {@code QueryCompiler} lowercases keyword term values before building
 * Lucene/OpenSearch term queries, so Title Case tokens would silently match nothing.
 */
public final class PolicyWaiverExpiryStatuses
{
  public static final String ACTIVE = "active";

  public static final String EXPIRED = "expired";

  public static final String NEVER = "never";

  public static final Set<String> ALL = Set.of(ACTIVE, EXPIRED, NEVER);

  private PolicyWaiverExpiryStatuses() {
  }
}
