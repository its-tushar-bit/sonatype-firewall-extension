/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiPageResult;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiFirewallServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public ApiFirewallService apiFirewallService;

  @Inject
  private InsightConfig config;

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Test
  public void testGetFirewallUnquarantineSummary_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);

    ApiFirewallReleaseQuarantineSummaryDTO dto = apiFirewallService.getReleaseQuarantineSummary();

    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test
  public void testGetFirewallQuarantineSummary_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallQuarantineSummary_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineSummary());
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);

    List<ApiFirewallReleaseQuarantineConfigDTO> dto = apiFirewallService.getReleaseQuarantineConfig();

    assertThat(dto.size()).isGreaterThan(0);
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineConfig());
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiFirewallService.getReleaseQuarantineConfig());
  }

  @Test
  public void testGetFirewallConfiguration_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);

    FirewallConfigurationDTO firewallConfigurationDTO = apiFirewallService.getFirewallConfiguration();

    assertThat(firewallConfigurationDTO).isNotNull();
  }

  @Test
  public void testGetFirewallConfiguration_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiFirewallService.getFirewallConfiguration());
  }

  @Test
  public void testGetFirewallConfiguration_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiFirewallService.getFirewallConfiguration());
  }

  @Test
  public void testSetFirewallConfiguration_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);
    grantGlobalPermission(Permission.WRITE);
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;

    firewallConfigurationDTO = apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);

    assertThat(firewallConfigurationDTO).isNotNull();
  }

  @Test
  public void testSetFirewallConfiguration_Unauthenticated() {
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;

    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO));
  }

  @Test
  public void testSetFirewallConfiguration_Unauthorized() {
    login();

    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO));
  }

  @Test
  public void testGetQuarantineSummary_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);

    assertThat(apiFirewallService.getQuarantineSummary()).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantineSummary_Unauthenticated() {
    apiFirewallService.getQuarantineSummary();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantineSummary_Unauthorized() {
    login();

    apiFirewallService.getQuarantineSummary();
  }

  @Test
  public void testGetUnquarantineList_Authorized() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    grantGlobalPermission(Permission.READ);

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, false, true, null, true, Collections.emptyList());
    final ApiPageResult<ApiFirewallComponentDTO> dto = apiFirewallService.getUnquarantineList(filter);

    assertThat(dto.getTotal()).isZero();
  }

  @Test
  public void testGetUnquarantineList_Unauthorized() {
    login();

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, false, true, null, true, Collections.emptyList());
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiFirewallService.getUnquarantineList(filter));
  }

  @Test
  public void testGetUnquarantineList_Unauthenticated() {
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, false, true, null, true, Collections.emptyList());
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiFirewallService.getUnquarantineList(filter));
  }
}
