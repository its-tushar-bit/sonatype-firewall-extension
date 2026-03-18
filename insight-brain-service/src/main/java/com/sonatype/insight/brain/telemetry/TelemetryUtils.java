/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.SbomPostImportMetricsTelemetry;
import com.sonatype.insight.brain.telemetry.ClientUserAgentUtil.UserAgent;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.REPOSITORY_COMPONENT_TELEMETRY;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.COMPONENT_ANALYSIS_COMPONENT_COUNTS;

@Named
@Singleton
public final class TelemetryUtils
{
  public static final String REAL_APPLICATION_ID = "real_application_id";

  public static final String REAL_OWNER_ID = "real_owner_id";

  private final TelemetryDataObfuscator telemetryDataObfuscator;

  @Inject
  public TelemetryUtils(TelemetryDataObfuscator telemetryDataObfuscator) {
    this.telemetryDataObfuscator = telemetryDataObfuscator;
  }

  public TelemetryData buildThirdPartyScanTelemetryData(
      final String applicationId,
      final Stage stage,
      final String thirdPartyScanType,
      final ScanTriggerType scanTriggerType,
      final String userAgent)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    includeRealApplicationId(attributes, applicationId);
    attributes.put("stage_id", stage.getStageTypeId());
    attributes.put("source", thirdPartyScanType);
    if (scanTriggerType != null) {
      attributes.put("scan_type", scanTriggerType.getId());
    }
    if (userAgent != null) {
      attributes.put("user_agent", userAgent);
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }

  public TelemetryData buildThirdPartyScanComponentInfoTelemetryData(
      final SbomComponentInfoTelemetry componentInfoTelemetry,
      final boolean isSkipValidationFeatureFlagEnabled,
      final boolean isSbomValid)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SBOM_DATA_METRICS);
    telemetryData.put(SbomComponentInfoTelemetry.ATTRIBUTE_NAME, componentInfoTelemetry);
    telemetryData.put("is_skip_sbom_validation_feature_flag_enabled", isSkipValidationFeatureFlagEnabled);
    telemetryData.put("is_sbom_valid", isSbomValid);

    return telemetryData;
  }

  public TelemetryData buildThirdPartyScanSbomImportTelemetryData(
      final SbomPostImportMetricsTelemetry sbomPostImportMetricsTelemetry)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    telemetryData.put(SbomPostImportMetricsTelemetry.ATTRIBUTE_NAME,
        SerializationUtils.clone(sbomPostImportMetricsTelemetry));
    sbomPostImportMetricsTelemetry.reset();

    return telemetryData;
  }

  public TelemetryData buildComponentsAnalysisTelemetryData(
      final String scanId,
      final String applicationId,
      final String stageId,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final Map<String, Object> requestedAttributes)
  {
    return buildComponentCountsTelemetryData(
        COMPONENT_ANALYSIS_COMPONENT_COUNTS,
        scanId,
        applicationId,
        stageId,
        scanTriggerType,
        clientUserAgent,
        clientInstanceId,
        requestedAttributes);
  }

  public TelemetryData buildApplicationEvaluationTelemetryData(
      String scanId,
      String applicationId,
      String stageId,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      Map<String, Object> requestedAttributes)
  {
    return buildComponentCountsTelemetryData(
        APPLICATION_EVALUATION_COMPONENT_COUNTS,
        scanId,
        applicationId,
        stageId,
        scanTriggerType,
        clientUserAgent,
        clientInstanceId,
        requestedAttributes);
  }

  @SuppressWarnings("unchecked")
  private TelemetryData buildComponentCountsTelemetryData(
      TelemetryPurpose telemetryPurpose,
      String scanId,
      String applicationId,
      String stageId,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      Map<String, Object> requestedAttributes)
  {
    TelemetryData telemetryData = new TelemetryData(telemetryPurpose);
    Map<String, Number> componentCounts = (Map<String, Number>) requestedAttributes.get("component_counts");

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("scan_id", scanId);
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    includeRealApplicationId(attributes, applicationId);
    attributes.put("stage_id", stageId);
    attributes.put("scan_trigger_type", scanTriggerType.getId());

    if (componentCounts != null) {
      for (String format : componentCounts.keySet()) {
        attributes.put("number_of_" + format.replace("-", "") + "_components",
            String.valueOf(componentCounts.get(format)));
      }
    }
    attributes.put("number_of_components", String.valueOf(getTotalComponentCounts(componentCounts)));

    ClientUserAgentUtil.UserAgent userAgent = ClientUserAgentUtil.parse(clientUserAgent);
    if (userAgent != null) {
      addUserAgentDataToAttributes(attributes, userAgent);
    }
    if (StringUtils.isNotBlank(clientInstanceId)) {
      attributes.put("client_instance_id", clientInstanceId);
    }

    final String hostSystem = UserAgentUtils.getHostSystem();
    if (StringUtils.isNotBlank(hostSystem)) {
      attributes.put("deployment_type", hostSystem);
    }

    Optional.of(requestedAttributes)
        .map(attr -> attr.get("ide_theme"))
        .ifPresent(val -> attributes.put("ide_theme", val));

    telemetryData.setAttributes(attributes);

    return telemetryData;
  }

  /**
   * This method adds a real_owner_id entry to the telemetry data attributes map. It has a dependency with the
   * Integrated Enterprise Reporting feature, it has to be enabled.
   */
  public void includeRealOwnerId(Map<String, Object> telemetryAttributes, String attributeValue) {
    telemetryAttributes.put(REAL_OWNER_ID, obfuscateIfAdvancedReportingDisabled(attributeValue));
  }

  /**
   * This method adds a real_application_id entry to the telemetry data attributes map. It has a dependency with the
   * Integrated Enterprise Reporting feature, it has to be enabled.
   */
  public void includeRealApplicationId(Map<String, Object> telemetryAttributes, String attributeValue) {
    telemetryAttributes.put(REAL_APPLICATION_ID, obfuscateIfAdvancedReportingDisabled(attributeValue));
  }

  public String obfuscate(String value) {
    return telemetryDataObfuscator.obfuscate(value);
  }

  public String obfuscateIfAdvancedReportingDisabled(String value) {
    return telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(value);
  }

  /**
   * Converts a Boolean golden status to its string representation for telemetry consistency.
   * Used by both pull request creation and lifecycle telemetry.
   *
   * @param isGolden the Boolean golden status (can be null)
   * @return "golden", "not_golden", or "unknown"
   */
  public String convertGoldenStatusToString(Boolean isGolden) {
    if (isGolden == null) {
      return "unknown";
    }
    return isGolden ? "golden" : "not_golden";
  }

  private static long getTotalComponentCounts(final Map<String, Number> componentCounts) {
    if (componentCounts == null) {
      return 0L;
    }
    return componentCounts.values()
        .stream()
        .map(Number::longValue)
        .reduce(Long::sum)
        .orElse(0L);
  }

  public static void addUserAgentDataToAttributes(Map<String, Object> attributes, UserAgent userAgent) {
    attributes.put("client_id", userAgent.client);
    attributes.put("client_version", userAgent.clientVersion);
    attributes.put("client_runtime", userAgent.runtime);
    attributes.put("client_runtime_version", userAgent.runtimeVersion);
    attributes.put("client_os_name", userAgent.os);
    attributes.put("client_os_version", userAgent.osVersion);
    attributes.put("client_other", userAgent.other);
  }

  public TelemetryData buildContinuousMonitoringMetricsAttributes(
      long appsEvaluatedCount,
      long totalExecutionTimeInSeconds,
      String stageIds)
  {
    TelemetryData telemetryData = new TelemetryData(
        TelemetryPurpose.CONTINUOUS_MONITORING_METRICS);
    HashMap<String, Object> continuousMonitoringMetrics = new HashMap<>();
    continuousMonitoringMetrics.put("appsEvaluatedCount", appsEvaluatedCount);
    continuousMonitoringMetrics.put("totalExecutionTimeInSeconds", totalExecutionTimeInSeconds);
    continuousMonitoringMetrics.put("stageIds", stageIds);
    telemetryData.put("continuous_monitoring_metrics", continuousMonitoringMetrics);
    return telemetryData;
  }

  public TelemetryData buildRepositoryComponentTelemetryData(
      String repositoryManagerId,
      String repositoryId,
      String componentFormat,
      String componentHash,
      RepositoryComponentTelemetryEventType eventType,
      Long quarantineTime,
      Long releaseQuarantineTime,
      String releaseQuarantineType,
      List<PolicyViolation> policyViolations)
  {
    RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(repositoryManagerId, repositoryId, componentFormat, componentHash, eventType,
            quarantineTime, releaseQuarantineTime, releaseQuarantineType);
    List<PolicyViolationTelemetry> policyViolationsTelemetry =
        policyViolations.stream().map(PolicyViolationTelemetry::new).toList();
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);
    telemetryData.put(REPOSITORY_COMPONENT_TELEMETRY, repositoryComponentTelemetry);
    telemetryData.put(POLICY_VIOLATION_TELEMETRY, policyViolationsTelemetry);
    return telemetryData;
  }
}
