/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.math.BigDecimal;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiFirewallMetricsServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @Test
  public void testGetFirewallMetrics_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
    final Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> map = firewallMetricsService.getFirewallMetrics();

    assertThat(map.size()).isEqualTo(6);
  }

  @Test
  public void testGetFirewallMetrics_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> firewallMetricsService.getFirewallMetrics());
  }

  @Test
  public void testGetFirewallMetrics_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> firewallMetricsService.getFirewallMetrics());
  }

  @Test
  public void testGetRoiFirewallMetrics_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD));
  }

  @Test
  public void testGetRoiFirewallMetrics_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD));
  }

  @Test
  public void testGetRoiFirewallMetrics() {
    dao.getAll().forEach(dao::delete);
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        30,
        15,
        BigDecimal.valueOf(800),
        BigDecimal.valueOf(400));
    grantConfigureSystemPermission();
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);
    firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD);
  }
}
