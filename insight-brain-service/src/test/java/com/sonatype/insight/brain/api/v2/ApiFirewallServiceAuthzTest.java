/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallService apiFirewallService;

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Test
  public void testGetReleaseQuarantineSummary_Authorized() {
    grantGlobalPermission(Permission.READ);

    ApiFirewallReleaseQuarantineSummaryDTO dto = apiFirewallService.getReleaseQuarantineSummary();

    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReleaseQuarantineSummary_Unauthorized() {
    login();
    apiFirewallService.getReleaseQuarantineSummary();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReleaseQuarantineSummary_Unauthenticated() {
    apiFirewallService.getReleaseQuarantineSummary();
  }

  @Test
  public void testGetReleaseQuarantineConfig_Authorized() {
    grantGlobalPermission(Permission.READ);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.getReleaseQuarantineConfig();

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReleaseQuarantineConfig_Unauthorized() {
    login();
    apiFirewallService.getReleaseQuarantineConfig();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReleaseQuarantineConfig_Unauthenticated() {
    apiFirewallService.getReleaseQuarantineConfig();
  }

  @Test
  public void testSetReleaseQuarantineConfig_Authorized() {
    grantGlobalPermission(Permission.READ);
    grantGlobalPermission(Permission.WRITE);

    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = apiFirewallService.setReleaseQuarantineConfig(new ArrayList<>());

    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetReleaseQuarantineConfig_Unauthorized() {
    login();
    apiFirewallService.setReleaseQuarantineConfig(null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetReleaseQuarantineConfig_Unauthenticated() {
    apiFirewallService.setReleaseQuarantineConfig(null);
  }

  @Test
  public void testGetQuarantineSummary_Authorized() {
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
  public void testGetComponents_Authorized() {
    grantGlobalPermission(Permission.READ);

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    final ApiPageResult<ApiFirewallComponentDTO> dto = apiFirewallService.getComponents(filter);

    assertThat(dto.getTotal()).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponents_Unauthorized() {
    login();

    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    apiFirewallService.getComponents(filter);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponents_Unauthenticated() {
    final FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());
    apiFirewallService.getComponents(filter);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthenticated() {
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess_Authorized() {
    grantGlobalPermission(Permission.WRITE);
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetQuarantinedComponentViewAnonymousAccess_Unauthorized() {
    login();
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(true);
  }
}
