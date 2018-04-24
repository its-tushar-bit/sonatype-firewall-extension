/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class HdsPingResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testPingHds() throws Exception {
    getHdsServer().setResponseForURI("ping", "alive", 200);

    HttpResponse response = restRequest().path(HdsPingResource.RESOURCE_PATH).get();

    assertResponseStatus(200, response);
    
    PingResponseDTO result = response.getBody(PingResponseDTO.class);
    assertThat(result.alive, is(true));
    assertThat(result.errorMessage, is(nullValue()));
  }
}
