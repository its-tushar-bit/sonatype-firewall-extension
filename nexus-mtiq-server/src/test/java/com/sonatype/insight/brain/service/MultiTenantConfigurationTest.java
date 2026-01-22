/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import java.util.Set;
import jakarta.inject.Provider;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.DefaultBranchMonitor;
import com.sonatype.insight.brain.git.PullRequestMonitor;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheProvider;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.google.common.collect.ImmutableMap;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantConfigurationTest
    extends AbstractMultiTenantTest
{
  @Mock
  ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Mock
  ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  @Mock
  JiraConfigurationDAO jiraConfigurationDAO;

  @Mock
  SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Mock
  ApiConfigurationService configurationService;

  @Mock
  Provider<List<HdsClient>> hdsClientsProvider;

  @Mock
  Provider<AsyncEventBus> asyncEventBusProvider;

  @Mock
  TaskScheduler taskScheduler;

  @Mock
  Provider<DefaultBranchMonitor> defaultBranchMonitorProvider;

  @Mock
  Provider<PullRequestMonitor> pullRequestMonitorProvider;

  @Mock
  Provider<ReleaseGraphCacheProvider> releaseGraphCacheProviderProvider;

  @Mock
  Provider<PolicyMonitorScheduler> policyMonitorSchedulerProvider;

  @Mock
  Provider<HistoricalPolicyViolationTelemetryTask> historicalPolicyViolationTelemetryTaskProvider;

  @Mock
  Provider<AutomaticQuarantineReleaseScheduler> automaticQuarantineReleaseSchedulerProvider;

  @Mock
  Provider<WaivedComponentUpgradeScheduler> waivedComponentUpgradeSchedulerProvider;

  @Mock
  TenantUtil tenantUtil;

  private Configuration underTest;

  @Before
  public void setup() {
    when(configurationService.getConfigurationNoAuthz(any(Set.class))).thenAnswer(
        i -> ImmutableMap.of(SystemConfigurationProperty.HDS_URL, TenantThreadLocal.getTenant().tenantSlug));

    underTest = new Configuration(proxyServerConfigurationDAO, reverseProxyAuthenticationConfigurationDAO,
        jiraConfigurationDAO, sourceControlConfigurationDAO, configurationService, hdsClientsProvider,
        asyncEventBusProvider, taskScheduler, defaultBranchMonitorProvider, pullRequestMonitorProvider,
        releaseGraphCacheProviderProvider, policyMonitorSchedulerProvider, automaticQuarantineReleaseSchedulerProvider,
        waivedComponentUpgradeSchedulerProvider, historicalPolicyViolationTelemetryTaskProvider, tenantUtil);
  }

  @Test
  public void testConfigurationIsRegistered_forEachTenant() {
    testAsGlobalTenant(t -> assertThat(underTest.getHdsUrl()).isEqualTo(GLOBAL_TENANT.tenantSlug));

    Tenant tenant1 = testAsNewTenant(t1 -> {
      underTest.register();

      assertThat(underTest.getHdsUrl()).isEqualTo(t1.tenantSlug);
    });

    testAsNewTenant(t2 -> {
      underTest.register();

      assertThat(underTest.getHdsUrl()).isEqualTo(t2.tenantSlug);
    });

    testAsTenant(tenant1, t -> assertThat(underTest.getHdsUrl()).isEqualTo(tenant1.tenantSlug));
  }

  @Test
  public void testRegistrationPriority() {
    assertThat(underTest.registrationPriority()).isEqualTo(1);
  }
}
