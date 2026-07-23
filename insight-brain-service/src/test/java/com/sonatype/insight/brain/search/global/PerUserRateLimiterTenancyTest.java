/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MTIQ isolation coverage for {@link PerUserRateLimiter}: the same username under two distinct tenants must
 * NOT share a concurrency bucket, because the limiter is a process-wide singleton and keys buckets by
 * {@code tenantId + username}. Under the old bare-username keying the tenant-b acquire below would be
 * rate-limited by the tenant-a permit held for the identical username.
 */
public class PerUserRateLimiterTenancyTest
{
  @Before
  public void setup() {
    TenantTestHelper.initMultiTenantMode();
  }

  @After
  public void cleanup() {
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void sameUsernameInDifferentTenants_haveIndependentBuckets() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);

    TenantTestHelper.testAsNewTenant("tenant-a", tenantA -> {
      PerUserRateLimiter.Permit heldInA = limiter.acquire("admin");
      try {
        // Same tenant, same user: the single permit is taken, so a second acquire is rate-limited.
        assertThatThrownBy(() -> limiter.acquire("admin"))
            .isInstanceOf(RateLimitedException.class);

        // Different tenant, identical username: distinct bucket, so this acquire MUST succeed even
        // while tenant-a still holds its only permit for "admin".
        TenantTestHelper.testAsNewTenant("tenant-b", tenantB -> {
          try (PerUserRateLimiter.Permit heldInB = limiter.acquire("admin")) {
            assertThat(heldInB).isNotNull();
          }
        });
      }
      finally {
        heldInA.close();
      }
    });
  }

  @Test
  public void permitsAreReturnedPerTenantAndUser() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);

    TenantTestHelper.testAsNewTenant("tenant-a", tenantA -> {
      try (PerUserRateLimiter.Permit first = limiter.acquire("admin")) {
        assertThat(first).isNotNull();
      }
      // After release the tenant-a "admin" bucket has its permit back.
      try (PerUserRateLimiter.Permit again = limiter.acquire("admin")) {
        assertThat(again).isNotNull();
      }
    });
  }
}
