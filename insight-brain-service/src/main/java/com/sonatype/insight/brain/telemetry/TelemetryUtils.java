/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.innersource.InnerSourceConsumerTelemetry;
import com.sonatype.insight.brain.innersource.InnerSourceProducerComponentTelemetry;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;

public final class TelemetryUtils
{
  private TelemetryUtils() {
  }

  public static TelemetryData buildThirdPartyScanTelemetryData(
      final String applicationPublicId,
      final Stage stage,
      final String thirdPartyScanType, final String userAgent)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", applicationPublicId);
    attributes.put("stage_id", stage.getStageTypeId());
    attributes.put("source", thirdPartyScanType);
    if (userAgent != null) {
      attributes.put("user_agent", userAgent);
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }

  public static TelemetryData buildInnerSourceTelemetryData(
      final String consumerId,
      final Set<InnerSourceProducerComponentTelemetry> producers)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.INNER_SOURCE_REPORT_USAGE);
    telemetryData.put(InnerSourceConsumerTelemetry.ATTRIBUTE_NAME,
        new InnerSourceConsumerTelemetry(consumerId, producers));
    return telemetryData;
  }

  @SuppressWarnings("unchecked")
  public static TelemetryData buildApplicationEvaluationTelemetryData(
      String applicationId,
      String stageId,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      Map<String, Object> requestedAttributes)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
    Map<String, Number> componentCounts = (Map<String, Number>) requestedAttributes.get("component_counts");

    Map<String, Object> attributes = new HashMap<>();
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
      attributes.put("client_id", userAgent.client);
      attributes.put("client_version", userAgent.clientVersion);
      attributes.put("client_runtime", userAgent.runtime);
      attributes.put("client_runtime_version", userAgent.runtimeVersion);
      attributes.put("client_os_name", userAgent.os);
      attributes.put("client_os_version", userAgent.osVersion);
      attributes.put("client_other", userAgent.other);
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
  public static void includeRealOwnerId(Map<String, Object> telemetryAttributes, String attributeValue) {
    telemetryAttributes.put("real_owner_id", attributeValue);
  }

  /**
   * This method adds a real_application_id entry to the telemetry data attributes map. It has a dependency with the
   * Integrated Enterprise Reporting feature, it has to be enabled.
   */
  public static void includeRealApplicationId(Map<String, Object> telemetryAttributes, String attributeValue) {
    telemetryAttributes.put("real_application_id", attributeValue);
  }

  private static long getTotalComponentCounts(final Map<String, Number> componentCounts) {
    if (componentCounts == null) {
      return 0L;
    }
    return componentCounts.values().stream().map(Number::longValue).reduce(Long::sum)
        .orElse(0L);
  }
}
