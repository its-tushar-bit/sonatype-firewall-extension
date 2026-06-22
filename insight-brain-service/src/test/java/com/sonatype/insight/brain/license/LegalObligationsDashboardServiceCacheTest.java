/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link LegalObligationsDashboardService} memoization. Uses real Spring wiring so cache
 * behaviour is not sensitive to Mockito / Failsafe B-L shard pollution (see CLM-39641).
 *
 * <p>
 * The cached response holds unmodifiable lists and is handed back by reference, so memoization is verified by a
 * single, branch-free identity assertion: two consecutive calls must return the very same instance. The
 * unmodifiable-list (mutation-isolation) guarantee is covered in isolation by the factory-level unit tests in
 * {@link LegalObligationsDashboardServiceTest}.
 */
public class LegalObligationsDashboardServiceCacheTest
    extends AbstractComponentTest
{
  @Inject
  private LegalObligationsDashboardService service;

  @Test
  public void testGetResponse_cachesPayloadForRepeatedCalls() {
    tempEntity.newApplicationWithParent("legal-obligations-cache");

    LegalObligationsDashboardResponse first = service.getResponse();
    LegalObligationsDashboardResponse second = service.getResponse();

    assertThat(first).isNotNull();
    // Memoization: the second call is a cache hit that returns the exact same memoized instance. The
    // tempEntity above puts the user in scope, so this exercises a cached variant (ALP or
    // top-legal-violations) rather than the permission-denied sentinel, which short-circuits before the
    // cache. The reference-identity assertion cannot pass without the cache wiring serving the hit.
    assertThat(second).isSameAs(first);
  }
}
