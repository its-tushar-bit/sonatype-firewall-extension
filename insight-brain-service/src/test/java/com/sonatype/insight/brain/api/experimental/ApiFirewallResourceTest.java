/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallResourceTest
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
  public void testGetFirewallUnquarantineSummary() throws Exception {
    // when GETing unquarantine summary
    HttpResponse response = restRequest().path(
        ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.RELEASE_QUARANTINE_SUMMARY_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    ApiFirewallReleaseQuarantineSummaryDTO dto = response.getBody(ApiFirewallReleaseQuarantineSummaryDTO.class);
    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test
  public void testGetFirewallConfiguration() throws Exception {
    // when GETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    FirewallConfigurationDTO firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
  }

  @Test
  public void testSetFirewallConfiguration_Enabled() throws Exception {
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(firewallConfigurationDTO).put();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isTrue();
  }

  @Test
  public void testSetFirewallConfiguration_Disabled() throws Exception {
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = false;

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(firewallConfigurationDTO).put();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
  }

  @Test
  public void testGetFirewallConfiguration_FeatureFlagDisabled() throws Exception {
    //disable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false)));

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is bad request 400
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void testSetFirewallConfiguration_FeatureFlagDisabled() throws Exception {
    //disable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false)));

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(new FirewallConfigurationDTO()).put();

    // then result is bad request 400
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void testGetFirewallConfiguration_MissingLicensedFeature() throws Exception {
    // setup remove firewall feature
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL);

    // when GETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }

  @Test
  public void testSetFirewallConfiguration_MissingLicensedFeature() throws Exception {
    // setup remove firewall feature
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL);

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(new FirewallConfigurationDTO()).put();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }
}
