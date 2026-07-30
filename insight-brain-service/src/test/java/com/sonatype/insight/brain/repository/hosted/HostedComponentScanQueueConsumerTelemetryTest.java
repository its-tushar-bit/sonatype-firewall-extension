/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
  private com.sonatype.insight.brain.dataaccess.policy.PolicyDAO policyDAO;

  @Mock
  private jakarta.inject.Provider<ReportDataStore> reportDataStoreProvider;

  @Mock
  private LifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Mock
  private jakarta.inject.Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private OwnerComponentDAO applicationComponentDAO;

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
        policyDAO,
        reportDataStoreProvider,
        lifecycleReportPersistenceService,
        scanPolicyEvaluatorProvider,
        policyViolationDAO,
        applicationComponentDAO,
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
        eq(scanId),
        eq(applicationId),
        eq("release"),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING),
        isNull(),
        isNull(),
        any()))
            .thenReturn(expectedTelemetryData);

    // When
    consumer.sendHostedScanEvaluationTelemetry(scanId, applicationId, stage, componentInfos,
        componentInfos.size());

    // Then - verify TelemetryUtils is called with HOSTED_REPOSITORY_SCANNING enum
    ArgumentCaptor<ScanTriggerType> triggerTypeCaptor = ArgumentCaptor.forClass(ScanTriggerType.class);
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        eq(scanId),
        eq(applicationId),
        eq("release"),
        triggerTypeCaptor.capture(),
        isNull(),
        isNull(),
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
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry(scanId, applicationId, stage, componentInfos,
        componentInfos.size());

    // Then - format counts are correctly grouped
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
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
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "release", componentInfos, 2);

    // Then - null format becomes "unknown"
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
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
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When
    consumer.sendHostedScanEvaluationTelemetry(
        "scan", "app", "RELEASE",
        List.of(new ScanComponentInfo("a.jar", "h1", "maven2")),
        1);

    // Then - stage passed to telemetry is lowercased to match policy_evaluation column convention
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        eq("release"),
        any(),
        any(),
        any(),
        any());
  }

  @Test
  public void sendHostedScanEvaluationTelemetry_swallowsExceptionsToProtectScanFlow() {
    // Given - TelemetryUtils throws (e.g., NPE from a malformed param)
    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenThrow(new RuntimeException("simulated failure"));

    // When + Then - call must not propagate the exception; the hosted scan flow must not break
    // because of telemetry failures (telemetry is auxiliary).
    consumer.sendHostedScanEvaluationTelemetry(
        "scan", "app", "release",
        List.of(new ScanComponentInfo("a.jar", "h1", "maven2")),
        1);

    // And no send is attempted since build failed
    verify(telemetrySender, never()).send(any(TelemetryData.class));
  }

  /**
   * CLM-42079 regression: for an identified-outer scan (pypi/rubygems/maven), the hosted
   * repository report collapses to "1 COMPONENT" via {@code forceComponentCount(1)}. Telemetry
   * must reflect the same effective count and attribute it to the outer's format, NOT the raw
   * scanner grouping (which would send N components with mixed formats).
   */
  @Test
  public void sendHostedScanEvaluationTelemetry_collapsesToOneKeyedOnOuterFormat() {
    // Given - 6 scanned entries (outer + 5 inner unknowns), gate collapses to 1
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("ansible-2.8.0.tar.gz", "outer-hash", "pypi"), // outer
        new ScanComponentInfo("nested/1.py", "h1", null),
        new ScanComponentInfo("nested/2.py", "h2", null),
        new ScanComponentInfo("nested/3.py", "h3", null),
        new ScanComponentInfo("nested/4.py", "h4", null),
        new ScanComponentInfo("nested/5.py", "h5", null));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When - effective count is 1 (gate collapsed)
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "build", componentInfos, 1);

    // Then - component_counts is {pypi: 1}, NOT {pypi: 1, unknown: 5}
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts)
        .as("collapse must attribute the single component to the outer's format only")
        .containsExactly(org.assertj.core.api.Assertions.entry("pypi", 1L));
  }

  /**
   * CLM-42079 regression: for keep-nested formats (nuget/go/pub) with an identified outer, the
   * effective count equals the full scanner list size and each nested format contributes its own
   * count — matches the "N COMPONENTS" the UI shows for those formats.
   */
  @Test
  public void sendHostedScanEvaluationTelemetry_keepsGroupedFormatsWhenEffectiveCountMatchesSize() {
    // Given - nuget-like scenario: outer + several nested DLLs, no collapse
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("outer.nupkg", "outer-hash", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/cs.dll", "h1", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/zh.dll", "h2", "nuget"));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When - effective count matches full list (gate did not collapse)
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "build", componentInfos, 3);

    // Then - nuget: 3
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts).containsEntry("nuget", 3L);
  }

  /**
   * CLM-42079 regression: when the authoritative DB count is 0 (e.g. row missing or explicitly
   * stamped 0), the collapse branch must NOT fabricate a fake {@code {outerFormat: 1}} entry —
   * that would mis-report a "1 component" scan the UI shows as empty. Emit an empty per-format
   * map so downstream {@code number_of_components} sums to 0 too.
   */
  @Test
  public void sendHostedScanEvaluationTelemetry_emitsEmptyMapWhenEffectiveCountIsZero() {
    // Given - a scan with scanner-visible components but the authoritative DB view says 0
    // (e.g. all repository_component rows deleted, or explicitly zeroed).
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("a.jar", "h1", "maven2"),
        new ScanComponentInfo("b.jar", "h2", "maven2"));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When - effective count is 0 (authoritative DB view)
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "build", componentInfos, 0);

    // Then - per-format map is empty (no phantom `{maven2: 1}` fabrication)
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    assertThat(componentCounts).as("effectiveCount=0 must not fabricate a per-format entry").isEmpty();
  }

  /**
   * CLM-42079 regression: for keep-nested formats (nuget/go/pub/npm) the ScanPolicyEvaluator
   * dedup can reduce the effective component count BELOW the raw scanner grouping. When that
   * happens the telemetry payload must cap number_of_components to match what the UI header
   * shows — attributing the reduction to the outer's format so the format distribution stays
   * dominated by the outer (which is what the user actually uploaded).
   */
  @Test
  public void sendHostedScanEvaluationTelemetry_capsGroupingWhenScannerOvercountsVsEffective() {
    // Given - 10 scanner entries but ScanPolicyEvaluator deduped to 3
    List<ScanComponentInfo> componentInfos = List.of(
        new ScanComponentInfo("outer.nupkg", "outer-hash", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/cs.dll", "h1", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/zh.dll", "h2", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/pt.dll", "h3", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/de.dll", "h4", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/es.dll", "h5", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/fr.dll", "h6", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/it.dll", "h7", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/ja.dll", "h8", "nuget"),
        new ScanComponentInfo("outer.nupkg!/localization/ko.dll", "h9", "nuget"));

    when(telemetryUtils.buildApplicationEvaluationTelemetryData(
        any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));

    // When - effective count is 3 (post-dedup UI value)
    consumer.sendHostedScanEvaluationTelemetry("scan", "app", "build", componentInfos, 3);

    // Then - grouping must sum to 3, not the raw scanner count of 10
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(telemetryUtils).buildApplicationEvaluationTelemetryData(
        any(), any(), any(), any(), any(), any(), attributesCaptor.capture());

    @SuppressWarnings("unchecked")
    Map<String, Long> componentCounts = (Map<String, Long>) attributesCaptor.getValue().get("component_counts");
    long sum = componentCounts.values().stream().mapToLong(Long::longValue).sum();
    assertThat(sum).as("cap: sum(per-format) must equal effectiveComponentCount, not scanner size")
        .isEqualTo(3L);
    assertThat(componentCounts).containsEntry("nuget", 3L);
  }

  // ---------------------------------------------------------------------------------------------
  // CLM-42079 normalizeStage tests
  //
  // Verifies that NXRM's inconsistent stage-column shapes are canonicalized before being written
  // to policy_evaluation.stage_type_id and emitted in telemetry. Production DB survey (2026-07-01)
  // observed: "RELEASE", "release", "BUILD", "STAGE_RELEASE" (underscore), and NULL rows in
  // hosted_component_scan_queue.policy_evaluation_stage. Prior to this fix, "STAGE_RELEASE" fell
  // through toLowerCase() as "stage_release" (invalid, silently mis-routed evaluation) and NULLs
  // masked NXRM misconfiguration in telemetry.
  // ---------------------------------------------------------------------------------------------

  @Test
  public void normalizeStage_lowercasesUppercaseValues() {
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("RELEASE", "job-1"))
        .isEqualTo("release");
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("BUILD", "job-1"))
        .isEqualTo("build");
  }

  @Test
  public void normalizeStage_preservesAlreadyLowercaseValues() {
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("release", "job-1"))
        .isEqualTo("release");
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("build", "job-1"))
        .isEqualTo("build");
  }

  @Test
  public void normalizeStage_convertsUnderscoresToHyphens_stageRelease() {
    // STAGE_RELEASE (NXRM's shape) → stage-release (IQ canonical shape).
    // Before the fix this would have been "stage_release" (underscore) and silently no-matched
    // any registered stage type.
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("STAGE_RELEASE", "job-1"))
        .isEqualTo("stage-release");
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("stage_release", "job-1"))
        .isEqualTo("stage-release");
  }

  @Test
  public void normalizeStage_convertsMixedCaseUnderscoreToHyphenLowercase() {
    // Guards against NXRM sending a title-cased or hybrid-cased underscore value like
    // "Stage_Release", "Stage_release", or "STAGE_release" — production DB survey shows
    // NXRM's stage casing is inconsistent and this method must not privilege one specific
    // shape.
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("Stage_Release", "job-mixed-1"))
        .isEqualTo("stage-release");
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("Stage_release", "job-mixed-2"))
        .isEqualTo("stage-release");
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("STAGE_release", "job-mixed-3"))
        .isEqualTo("stage-release");
  }

  @Test
  public void normalizeStage_nullFallsBackToComplianceStageId() {
    // NULL from queue row → compliance/stage-release fallback (existing behaviour). The WARN
    // log is exercised implicitly; asserting the return keeps this test independent of the
    // logging framework.
    assertThat(HostedComponentScanQueueConsumer.normalizeStage(null, "job-nxrm-missing-stage"))
        .isEqualTo(com.sonatype.insight.brain.model.policy.stages.ComplianceStageType.ID);
  }

  @Test
  public void normalizeStage_blankFallsBackToComplianceStageId() {
    // Empty/whitespace-only values are treated the same as NULL — either indicates NXRM did
    // not populate the column.
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("", "job-blank"))
        .isEqualTo(com.sonatype.insight.brain.model.policy.stages.ComplianceStageType.ID);
    assertThat(HostedComponentScanQueueConsumer.normalizeStage("   ", "job-whitespace"))
        .isEqualTo(com.sonatype.insight.brain.model.policy.stages.ComplianceStageType.ID);
  }
}
