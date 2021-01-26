/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.repository.firewall.FirewallResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.repository.firewall.FirewallResource.STATUS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetStatus() throws Exception {
    // when GETing config
    HttpResponse response = restRequest().path(RESOURCE_PATH, STATUS_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    FirewallStatusDTO firewallStatusDTO = response.getBody(FirewallStatusDTO.class);
    assertThat(firewallStatusDTO.experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false);
  }
}
