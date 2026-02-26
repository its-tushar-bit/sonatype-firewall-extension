/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import io.micrometer.core.instrument.Tags;

/**
 * Shared tag builder for SCM metrics. Null-safe — any tag with a null value is omitted rather than
 * causing a {@link NullPointerException} in Micrometer's {@link Tags#of} methods.
 */
public final class ScmMetricsTags
{
  private static final String CLIENT_ID_TAG = "client_id";

  private static final String USER_ID_TAG = "user_id";

  private static final String TENANT_ID_TAG = "tenant_id";

  private ScmMetricsTags() {
  }

  /**
   * Builds Micrometer {@link Tags} for SCM metrics with client_id, user_id, and tenant_id dimensions.
   * Null values are omitted from the tag set.
   *
   * @param clientId the SCM provider identifier (e.g. "github", "gitlab"), may be null
   * @param userId the user or synchronization key for the API client, may be null
   * @return tags containing only non-null values plus the current tenant id
   */
  public static Tags buildTagsWithTenantId(final String clientId, final String userId) {
    Tenant tenant = TenantThreadLocal.getTenant();
    Tags tags = Tags.empty();
    if (clientId != null) {
      tags = tags.and(CLIENT_ID_TAG, clientId);
    }
    if (userId != null) {
      tags = tags.and(USER_ID_TAG, userId);
    }
    if (tenant != null && tenant.tenantSlug != null) {
      tags = tags.and(TENANT_ID_TAG, tenant.tenantSlug);
    }
    return tags;
  }
}
