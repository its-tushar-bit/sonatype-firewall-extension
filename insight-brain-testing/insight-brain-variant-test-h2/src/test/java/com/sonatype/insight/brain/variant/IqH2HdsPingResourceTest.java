/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.HdsPingResource;
import com.sonatype.insight.brain.hds.PingResponseDTO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2HdsPingResourceTest
{
  private IqTestContext ctx;

  @Test
  void testPingHds() throws Exception {
    ctx.getHdsServer().respondWith("alive").atUri("ping");

    HttpResponse response = ctx.restRequest().path(HdsPingResource.RESOURCE_PATH).get();

    ctx.assertResponseStatus(200, response);

    PingResponseDTO result = response.getBody(PingResponseDTO.class);
    assertThat(result.alive).isTrue();
    assertThat(result.errorMessage).isNull();
  }
}
