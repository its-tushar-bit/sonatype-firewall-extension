/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.repository.firewall.FirewallResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.repository.firewall.FirewallResource.STATUS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallResourceTest
    extends AbstractResourceTest
{
  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Before
  public void setup() throws Exception {
    //enable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true)));
  }

  @Test
  public void testGetFirewallStatus() throws Exception {
    // when GETing config
    HttpResponse response = restRequest().path(RESOURCE_PATH, STATUS_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    FirewallStatusDTO firewallStatusDTO = response.getBody(FirewallStatusDTO.class);
    assertThat(firewallStatusDTO.experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true);
  }

  @Test
  public void testGetFirewallStatus_MissingFirewallAutoUnquarantineFeature() throws Exception {
    // setup remove firewall feature
    setMissingFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    // when GETing status
    HttpResponse response = restRequest().path(RESOURCE_PATH, STATUS_PATH).get();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }

  @Test
  public void testGetFirewallStatus_MissingReleaseIntegrityFeature() throws Exception {
    // setup remove firewall feature
    setMissingFeature(LicensedFeature.RELEASE_INTEGRITY);

    // when GETing status
    HttpResponse response = restRequest().path(RESOURCE_PATH, STATUS_PATH).get();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }
}
