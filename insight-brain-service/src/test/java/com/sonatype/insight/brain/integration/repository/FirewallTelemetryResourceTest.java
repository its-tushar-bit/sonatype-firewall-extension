/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.repository.FirewallTelemetry;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallTelemetryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testPostFirewallTelemetryData() throws Exception {
    FirewallTelemetry firewallTelemetryDTO = new FirewallTelemetry();

    final HttpResponse response =
        restRequest().path(FirewallTelemetryResource.RESOURCE_PATH).body(firewallTelemetryDTO).post();

    assertThat(response.getStatusCode()).isEqualTo(Status.NO_CONTENT.getStatusCode());
  }
}
