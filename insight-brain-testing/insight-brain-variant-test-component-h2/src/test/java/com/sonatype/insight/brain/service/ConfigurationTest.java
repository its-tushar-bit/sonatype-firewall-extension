/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheProvider;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectProvider;

@ComponentH2Test
public class ConfigurationTest
    extends AbstractComponentH2Test
{
  private static final Set<String> RESET_CONFIGURATION_PROPERTIES = Set.of(
      SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
      SystemConfigurationProperty.BASE_URL,
      SystemConfigurationProperty.FORCE_BASE_URL,
      SystemConfigurationProperty.HDS_URL,
      SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
      SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
      SystemConfigurationProperty.POLICY_MONITORING_HOUR,
      SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR,
      SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
      SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
      SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
      SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
      SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS);

  @Mock
  HdsClient hdsClient1;

  @Mock
  HdsClient hdsClient2;

  @Mock
  HistoricalPolicyViolationTelemetryTask historicalPolicyViolationTelemetryTask;

  @Mock
  ReleaseGraphCacheProvider releaseGraphCacheProvider;

  @Mock
  PolicyMonitorScheduler policyMonitorScheduler;

  @Mock
  AutomaticQuarantineReleaseScheduler automaticQuarantineReleaseScheduler;

  @Mock
  WaivedComponentUpgradeScheduler waivedComponentUpgradeScheduler;

  @Mock
  TaskScheduler taskScheduler;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private Configuration configuration;

  @Inject
  private AsyncEventBus asyncEventBus;

  @BeforeEach
  public void setUpConfigurationTest() {
    configurationService.deleteConfigurationInDatabaseNoAuthz(RESET_CONFIGURATION_PROPERTIES);
    configuration.register();
    asyncEventBus.setMaxPoolSize(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);

    @SuppressWarnings("unchecked")
    ObjectProvider<HdsClient> hdsClientProvider = (ObjectProvider<HdsClient>) mock(ObjectProvider.class);
    lenient().when(hdsClientProvider.orderedStream()).thenReturn(Stream.of(hdsClient1, hdsClient2));
    applyBeanFieldOverride(Configuration.class, "hdsClients", hdsClientProvider);
  }

  @Test
  public void testConfigurationChanged_OnlyReloadsThePropertyThatIsUpdated() {
    final String givenSomeCustomBaseUrl1 = "http://my-custom-base-url-1/";
    final String givenSomeCustomBaseUrl2 = "http://my-custom-base-url-2/";

    givenCacheAndDatabaseAreNotInSync(
        AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1, givenSomeCustomBaseUrl1);

    // none of the cache values are updated
    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
    assertThat(configuration.getBaseUrlConfiguration().getBaseUrl()).isEqualTo(null);

    // should only update the BASE_URL
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.BASE_URL));
    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
    assertThat(configuration.getBaseUrlConfiguration().getBaseUrl()).isEqualTo(givenSomeCustomBaseUrl1);

    givenCacheAndDatabaseAreNotInSync(
        AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1, givenSomeCustomBaseUrl2);

    // none of the cache values are updated
    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
    assertThat(configuration.getBaseUrlConfiguration().getBaseUrl()).isEqualTo(givenSomeCustomBaseUrl1);

    // should only update the DEFAULT_MAX_POOL_SIZE
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE));
    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);
    assertThat(configuration.getBaseUrlConfiguration().getBaseUrl()).isEqualTo(givenSomeCustomBaseUrl1);
  }

  @Test
  public void testConfigurationChanged() {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);

    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE));

    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);
  }

  @Test
  public void testConfigurationChanged_shouldFireServerConfigurationChangedEventOnHdsClientForUrlChange() {
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.HDS_URL));

    verify(hdsClient1).serverConfigurationChanged();
    verify(hdsClient2).serverConfigurationChanged();
  }

  @Test
  public void testConfigurationChanged_shouldFireServerConfigurationChangedEventOnHdsClientForConnectionTimeoutChange() {
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS));

    verify(hdsClient1).serverConfigurationChanged();
    verify(hdsClient2).serverConfigurationChanged();
  }

  @Test
  public void testConfigurationChanged_shouldFireServerConfigurationChangedEventOnHdsClientForSocketTimeoutChange() {
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS));

    verify(hdsClient1).serverConfigurationChanged();
    verify(hdsClient2).serverConfigurationChanged();
  }

  @Test
  public void testConfigurationChanged_shouldInitializeReleaseGraphCacheOnCacheSizeUpdate() {
    verify(releaseGraphCacheProvider, times(0)).initializeCache();

    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE));

    verify(releaseGraphCacheProvider).initializeCache();
  }

  @Test
  public void testConfigurationChanged_shouldUpdatePolicyMonitorScheduleWhenSchedulerEnabledAndHourUpdated() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // given that the configuration has changed
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR, 1);

    // check that initially we have not called schedulePolicyMonitoring
    verify(policyMonitorScheduler, times(0)).schedulePolicyMonitoring();

    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.POLICY_MONITORING_HOUR));
    verify(policyMonitorScheduler).schedulePolicyMonitoring();
  }

  @Test
  public void testConfigurationChanged_shouldUpdateHistoricalPolicyViolationTelemetryTaskWhenUpdated() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // given that the configuration has changed
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, 1);

    // check that initially we have not called schedulePolicyMonitoring
    verify(historicalPolicyViolationTelemetryTask, times(0)).scheduleHistoricalPolicyViolationTelemetryTask();

    configuration.configurationChanged(
        ImmutableSet.of(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR));
    verify(historicalPolicyViolationTelemetryTask).scheduleHistoricalPolicyViolationTelemetryTask();
  }

  @Test
  public void testConfigurationChanged_shouldScheduleAutomaticQuarantineReleaseWhenQuarantineReleaseTimeChanged() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // given that the configuration has changed
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 31);

    // check that initially we have not called scheduleAutomaticQuarantineRelease
    verify(
        automaticQuarantineReleaseScheduler, times(0)).scheduleAutomaticQuarantineRelease();

    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES));
    verify(automaticQuarantineReleaseScheduler).scheduleAutomaticQuarantineRelease();
  }

  @Test
  public void testConfigurationChanged_shouldScheduleWaivedComponentUpgradesWhenUpgradeInspectionHourChanged() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // given that the configuration has changed
    // and waived_component_upgrade_monitoring_is_enabled
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, true);
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED));
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR, 2);

    // called when we enable monitoring, make sure this was the only call
    verify(
        waivedComponentUpgradeScheduler, times(1)).scheduleWaivedComponentUpgradeInspection();

    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR));

    // called again when we updated the hour with the scheduler enabled
    verify(waivedComponentUpgradeScheduler, times(2)).scheduleWaivedComponentUpgradeInspection();
  }

  @Test
  public void testConfigurationChanged_shouldDeregisterWaivedComponentSchedulerIfDisabled() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // enable the scheduler
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, true);
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED));

    // called when we enable monitoring, make sure this was the only call
    verify(
        waivedComponentUpgradeScheduler, times(0)).deregister();

    // toggle it back to false
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, false);
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED));

    // called again when we updated the hour with the scheduler enabled
    verify(waivedComponentUpgradeScheduler).deregister();
  }

  @Test
  public void testConfigurationChanged_shouldNotUpdateAnySchedulersIfSchedulingIsDisabled() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(false);

    // given that the configuration has changed
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR, 1);
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, 12);
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 31);
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR, 2);

    // then fire updates
    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.POLICY_MONITORING_HOUR));
    configuration.configurationChanged(
        ImmutableSet.of(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR));
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES));
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED));
    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR));

    verify(policyMonitorScheduler, times(0)).schedulePolicyMonitoring();
    verify(historicalPolicyViolationTelemetryTask, times(0)).scheduleHistoricalPolicyViolationTelemetryTask();
    verify(
        automaticQuarantineReleaseScheduler, times(0)).scheduleAutomaticQuarantineRelease();
    verify(waivedComponentUpgradeScheduler, times(0)).scheduleWaivedComponentUpgradeInspection();
  }

  @Test
  public void testConfigurationChanged_shouldNotScheduleWaivedComponentUpgradesWhenUpgradeInspectionHourChangedAndDisabled() {
    when(taskScheduler.isSchedulerInitialized()).thenReturn(true);

    // given that the configuration has changed, but the scheduler is disabled
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR, 2);

    configuration.configurationChanged(ImmutableSet.of(
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR));

    // not called even though there was a change because waivedComponentUpgradeMonitoring is not enabled
    verify(waivedComponentUpgradeScheduler, times(0)).scheduleWaivedComponentUpgradeInspection();
  }

  @Test
  public void testGetMatcherConfiguration_DisableConanNamespaceMatching_False() {
    Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();

    assertThat(matcherConfiguration).containsEntry("disableConanNamespaceMatching", "false");
  }

  @Test
  public void testGetMatcherConfiguration_DisableConanNamespaceMatching_True() {
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, true);
    configurationService.applyConfigurationToClients(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING);

    Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();

    assertThat(matcherConfiguration).containsEntry("disableConanNamespaceMatching", "true");
  }

  @Test
  public void testInitializeValues_LoadUserTokenDefaultExpirationDays() {
    // Given a value is set in the database before initialization
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 45);

    // When the configuration is reinitialized (simulating server startup)
    configuration.register();

    // Then the value should be available from the configuration cache
    assertThat(configuration.getUserTokenDefaultExpirationDays()).isEqualTo(45);
  }

  private void givenCacheAndDatabaseAreNotInSync(
      final int maxPoolSize,
      final String givenSomeCustomBaseUrl)
  {
    // given the database and the cache are out of sync
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        maxPoolSize);
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.BASE_URL,
        givenSomeCustomBaseUrl);
  }
}
