/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.variant;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.POLICY_WAIVER_REASONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiPolicyWaiverReasonResourceAuthzTest
{
  private IqTestContext ctx;

  @Test
  void testGetPolicyWaiverReasons_unauthenticated() throws Exception {
    final var response = ctx.restRequest().path(POLICY_WAIVER_REASONS_PATH).anon().get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetPolicyWaiverReasons_authenticated() throws Exception {
    final var response = ctx.restRequest().path(POLICY_WAIVER_REASONS_PATH).auth().get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }
}
