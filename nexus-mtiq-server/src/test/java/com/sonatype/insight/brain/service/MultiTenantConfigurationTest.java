/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.DefaultBranchMonitor;
import com.sonatype.insight.brain.git.PullRequestMonitor;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.relay.RelayClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.continuousmonitoring.RepositoryEvaluationQueueScheduler;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheProvider;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.common.collect.ImmutableMap;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
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
  ObjectProvider<HdsClient> hdsClientsProvider;

  @Mock
  AsyncEventBus asyncEventBus;

  @Mock
  RelayClient relayClient;

  @Mock
  TaskScheduler taskScheduler;

  @Mock
  DefaultBranchMonitor defaultBranchMonitor;

  @Mock
  PullRequestMonitor pullRequestMonitor;

  @Mock
  ReleaseGraphCacheProvider releaseGraphCacheProvider;

  @Mock
  PolicyMonitorScheduler policyMonitorScheduler;

  @Mock
  HistoricalPolicyViolationTelemetryTask historicalPolicyViolationTelemetryTask;

  @Mock
  RepositoryEvaluationQueueScheduler repositoryEvaluationQueueScheduler;

  @Mock
  AutomaticQuarantineReleaseScheduler automaticQuarantineReleaseScheduler;

  @Mock
  WaivedComponentUpgradeScheduler waivedComponentUpgradeScheduler;

  @Mock
  TenantUtil tenantUtil;

  private Configuration underTest;

  @BeforeEach
  public void setup() {
    lenient().when(configurationService.getConfigurationNoAuthz(any(Set.class)))
        .thenAnswer(
            i -> ImmutableMap.of(SystemConfigurationProperty.HDS_URL, TenantThreadLocal.getTenant().tenantSlug));

    underTest = new Configuration(proxyServerConfigurationDAO, reverseProxyAuthenticationConfigurationDAO,
        jiraConfigurationDAO, sourceControlConfigurationDAO, configurationService, hdsClientsProvider,
        asyncEventBus, relayClient, taskScheduler, defaultBranchMonitor, pullRequestMonitor,
        releaseGraphCacheProvider, policyMonitorScheduler, repositoryEvaluationQueueScheduler,
        automaticQuarantineReleaseScheduler,
        waivedComponentUpgradeScheduler, historicalPolicyViolationTelemetryTask, tenantUtil);
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
