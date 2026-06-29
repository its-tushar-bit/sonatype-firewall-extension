/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for telemetry emission from {@link HostedComponentScanQueueConsumer}.
 * <p>
 * Verifies CLM-41693 acceptance criteria: hosted repository scans emit
 * {@code APPLICATION_EVALUATION_COMPONENT_COUNTS} telemetry with
 * {@code scan_trigger_type = "HOSTED_REPOSITORY_SCANNING"}.
 */
@RunWith(MockitoJUnitRunner.class)
public class HostedComponentScanQueueConsumerTelemetryTest
{
  @Mock
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private HostedComponentScanQueueDAO scanQueueDAO;

  @Mock
  private jakarta.inject.Provider<ScanPersistenceService> scanPersistenceServiceProvider;

  @Mock
  private jakarta.inject.Provider<ScanUploader> scanUploaderProvider;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Mock
  private jakarta.inject.Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  @Mock
  private ApplicationForHostedRepositoryComponentService applicationForHostedComponentService;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Mock
  private jakarta.inject.Provider<ReportDataStore> reportDataStoreProvider;

  @Mock
  private ApplicationReportPersistenceService applicationReportPersistenceService;

  @Mock
  private TelemetryUtils telemetryUtils;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private ShutdownHandler shutdownHandler;

  private HostedComponentScanQueueConsumer consumer;

  @Before
  public void setUp() {
    consumer = new HostedComponentScanQueueConsumer(
        apiConfigurationService,
        scanQueueDAO,
        scanPersistenceServiceProvider,
        scanUploaderProvider,
        repositoryDAO,
        repositoryComponentDAO,
        repositoryPolicyViolationDAO,
        repositoryPolicyEvaluatorProvider,
        applicationForHostedComponentService,
        policyEvaluationDAO,
        reportDataStoreProvider,
        applicationReportPersistenceService,
        telemetryUtils,
        telemetrySender,
        shutdownHandler);
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_emitsScanTriggerTypeHostedRepositoryScanning() {
    // Given
    String scanId = "test-scan-id";
    String applicationId = "test-app-id";
    String stage = "release";
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("test/lib.jar", "hash1", "maven2"),
        new ScanComponentInfo("test/lib2.jar", "hash2", "maven2"));

    TelemetryData expectedTelemetryData = new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.eq(scanId),
        org.mockito.ArgumentMatchers.eq(applicationId),
        org.mockito.ArgumentMatchers.eq("release"),
        org.mockito.ArgumentMatchers.eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING),
        org.mockito.ArgumentMatchers.isNull(),
        org.mockito.ArgumentMatchers.isNull(),
        org.mockito.ArgumentMatchers.any()))
            .thenReturn(expectedTelemetryData);

    // When
    consumer.sendHostedScanEvaluationTelemetry(scanId, applicationId, stage, componentInfos);

    // Then - verify TelemetryUtils is called with HOSTED_REPOSITORY_SCANNING enum
    ArgumentCaptor<ScanTriggerType> triggerTypeCaptor = ArgumentCaptor.forClass(ScanTriggerType.class);
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.eq(scanId),
        org.mockito.ArgumentMatchers.eq(applicationId),
        org.mockito.ArgumentMatchers.eq("release"),
        triggerTypeCaptor.capture(),
        org.mockito.ArgumentMatchers.isNull(),
        org.mockito.ArgumentMatchers.isNull(),
        attributesCaptor.capture());
    assertThat(triggerTypeCaptor.getValue()).isEqualTo(ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    // Then - verify TelemetrySender.send was called with the built telemetry data
    verify(telemetrySender).send(expectedTelemetryData);

    // Then - verify component_counts attribute groups components by format
    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts).containsEntry("maven2", 2L);
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_groupsComponentCountsByFormat() {
    // Given - mixed formats
    String scanId = "test-scan-id";
    String applicationId = "test-app-id";
    String stage = "release";
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("a.jar", "h1", "maven2"),
        new ScanComponentInfo("b.gem", "h2", "rubygems"),
        new ScanComponentInfo("c.tgz", "h3", "npm"),
        new ScanComponentInfo("d.jar", "h4", "maven2"));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry(scanId, applicationId, stage, componentInfos);

    // Then - format counts are correctly grouped
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts)
        .containsEntry("maven2", 2L)
        .containsEntry("rubygems", 1L)
        .containsEntry("npm", 1L);
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_handlesNullFormatAsUnknown() {
    // Given - one component has a null format
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("a.jar", "h1", "maven2"),
        new ScanComponentInfo("b.bin", "h2", null));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "release", componentInfos);

    // Then - null format becomes "unknown"
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts)
        .containsEntry("maven2", 1L)
        .containsEntry("unknown", 1L);
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_lowercasesStage() {
    // Given - stage in uppercase / mixed case
    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry(
        "scan", "app", "RELEASE",
        List.of(new ScanComponentInfo("a.jar", "h1", "maven2")));

    // Then - stage passed to telemetry is lowercased to match policy_evaluation column convention
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.eq("release"),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_swallowsExceptionsToProtectScanFlow() {
    // Given - TelemetryUtils throws (e.g., NPE from a malformed param)
    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()))
            .thenThrow(new RuntimeException("simulated failure"));

    // When + Then - call must not propagate the exception; the hosted scan flow must not break
    // because of telemetry failures (telemetry is auxiliary).
    consumer.sendHostedScanEvaluationTelemetry(
        "scan", "app", "release",
        List.of(new ScanComponentInfo("a.jar", "h1", "maven2")));

    // And no send is attempted since build failed
    verify(telemetrySender, never()).send(org.mockito.ArgumentMatchers.any(TelemetryData.class));
  }
}
