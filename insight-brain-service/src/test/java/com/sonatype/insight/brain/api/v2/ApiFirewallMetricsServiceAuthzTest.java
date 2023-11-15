/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallMetricsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Test
  public void testGetFirewallMetrics_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
    final Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> map = firewallMetricsService.getFirewallMetrics();

    assertThat(map.size()).isEqualTo(6);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetFirewallMetrics_Unauthorized() {
    login();
    firewallMetricsService.getFirewallMetrics();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetFirewallMetrics_Unauthenticated() {
    firewallMetricsService.getFirewallMetrics();
  }
}
