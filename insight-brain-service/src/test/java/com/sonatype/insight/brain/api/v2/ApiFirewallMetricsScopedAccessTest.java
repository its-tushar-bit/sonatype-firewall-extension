/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.LocalDate;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for scoped access to ApiFirewallMetricsService methods.
 */
@Category(SlowTest.class)
public class ApiFirewallMetricsScopedAccessTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Test
  public void testGetFirewallMetrics_ProxyUserCanCall_ReturnsGlobalMetrics() {
    // Create a proxy repository
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "testProxyRepo",
        RepositoryType.proxy, "docker");

    // Create some firewall metrics
    LocalDate fiveDaysAgoLocalDate = LocalDate.now().minusDays(5);
    tempEntity.newFirewallMetrics(WAIVED_COMPONENTS, 20, toDate(fiveDaysAgoLocalDate), fiveDaysAgoLocalDate);

    // Grant READ on the proxy repo (scoped access)
    grantReadPermission(proxyRepo.getId());

    // Proxy user can call getFirewallMetrics and sees global metric values (not scoped)
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> metrics = firewallMetricsService.getFirewallMetrics();

    assertThat(metrics).isNotNull();
    assertThat(metrics).isNotEmpty();
    // The metrics values are global, not filtered to the user's permitted repo
    ApiFirewallMetricsResultDTO waivedMetrics = metrics.get(WAIVED_COMPONENTS);
    assertThat(waivedMetrics).isNotNull();
    assertThat(waivedMetrics.getFirewallMetricsValue()).isEqualTo(20);
  }
}
