/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiComponentVersionsResourceV2AuthzTest
{
  private IqTestContext ctx;

  @Test
  void testGetComponentVersions_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).anon().post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }
}
