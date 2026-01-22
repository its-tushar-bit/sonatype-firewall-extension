/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.telemetry.ClientUserAgentUtil;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ExternalTelemetryService
{
  private static final Logger log = LoggerFactory.getLogger(ExternalTelemetryService.class);

  private final TelemetrySender telemetrySender;

  @Inject
  public ExternalTelemetryService(TelemetrySender telemetryValues) {
    this.telemetrySender = telemetryValues;
  }

  public void sendTelemetry(Map<String, Object> telemetryValues, HttpServletRequest req) {
    if (telemetryValues == null || telemetryValues.isEmpty()) {
      log.info("External telemetry endpoint called without any telemetry values.");
      throw new BadRequestException("Telemetry values are required.");
    }

    String purpose = (String) telemetryValues.get("telemetry_purpose");
    if (purpose == null) {
      log.info("External telemetry endpoint called without the required field `telemetry_purpose`.");
      throw new BadRequestException("Telemetry purpose is required.");
    }

    TelemetryPurpose telemetryPurpose;
    try {
      telemetryPurpose = TelemetryPurpose.valueOf(purpose);
    }
    catch (IllegalArgumentException e) {
      log.info("External telemetry endpoint called with an unknown `telemetry_purpose`.");
      throw new BadRequestException("Unknown telemetry purpose.");
    }

    String clientUserAgent = HdsClient.getClientUserAgent(req);
    if (clientUserAgent != null) {
      ClientUserAgentUtil.UserAgent parsed = ClientUserAgentUtil.parse(clientUserAgent);
      if (parsed != null) {
        TelemetryUtils.addUserAgentDataToAttributes(telemetryValues, parsed);
      }
      else {
        telemetryValues.put("user_agent", clientUserAgent);
      }
    }

    String clientInstanceId = HdsClient.getClientInstanceId(req);
    if (StringUtils.isNotBlank(clientInstanceId)) {
      telemetryValues.put("client_instance_id", clientInstanceId);
    }

    switch (telemetryPurpose) {
      case SYNC_SERVICE_METRICS:
        sendSyncServiceMetricsTelemetry(telemetryValues);
        break;
      case JIRA_PLUGIN_CONFIGURATION_METRICS:
        sendJiraPluginConfigurationTelemetry(telemetryValues);
        break;
      case JIRA_PLUGIN_USAGE_METRICS:
        sendJiraPluginUsageTelemetry(telemetryValues);
        break;
      case IDE_USER_INTERACTION_METRICS:
        sendIdeUserInteractionMetrics(telemetryValues);
        break;
      case INTEGRATIONS_FEATURE_USAGE_METRICS:
        sendIntegrationsFeatureUsageMetricsTelemetry(telemetryValues);
        break;
      default:
        log.info("External telemetry endpoint called with an unsupported `telemetry_purpose`.");
        throw new BadRequestException("Telemetry purpose not supported.");
    }
  }

  private void sendSyncServiceMetricsTelemetry(Map<String, Object> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SYNC_SERVICE_METRICS);
    populateTelemetryData(telemetryValues, telemetryData);
    telemetrySender.send(telemetryData);
  }

  private void sendJiraPluginConfigurationTelemetry(Map<String, Object> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.JIRA_PLUGIN_CONFIGURATION_METRICS);

    telemetryData.put("jira_plugin_aggregation_by_component_count",
        Integer.valueOf((String) telemetryValues.get("aggregation_by_component_count")));
    telemetryData.put("jira_plugin_aggregation_by_iq_evaluation_count",
        Integer.valueOf((String) telemetryValues.get("aggregation_by_iq_evaluation_count")));
    telemetryData.put("jira_plugin_automatic_workflow_transition_count",
        Integer.valueOf((String) telemetryValues.get("automatic_workflow_transition_count")));
    telemetryData.put("jira_plugin_total_project_count",
        Integer.valueOf((String) telemetryValues.get("total_project_count")));

    telemetrySender.send(telemetryData);
  }

  private void sendJiraPluginUsageTelemetry(Map<String, Object> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.JIRA_PLUGIN_USAGE_METRICS);

    telemetryData.put("jira_plugin_issue_count",
        Integer.valueOf((String) telemetryValues.get("issue_count")));
    telemetryData.put("jira_plugin_transitioned_issue_count",
        Integer.valueOf((String) telemetryValues.get("transitioned_issue_count")));

    for (Entry<String, Object> entry : telemetryValues.entrySet()) {
      if (entry.getKey().startsWith("client_")) {
        telemetryData.put(entry.getKey(), entry.getValue());
      }
    }

    telemetrySender.send(telemetryData);
  }

  private void sendIdeUserInteractionMetrics(Map<String, Object> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.IDE_USER_INTERACTION_METRICS);
    populateTelemetryData(telemetryValues, telemetryData);
    telemetrySender.send(telemetryData);
  }

  private void sendIntegrationsFeatureUsageMetricsTelemetry(Map<String, Object> telemetryValues) {
    if (!telemetryValues.containsKey("feature")) {
      throw new BadRequestException("Telemetry property 'feature' is required.");
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.INTEGRATIONS_FEATURE_USAGE_METRICS);
    populateTelemetryData(telemetryValues, telemetryData);
    telemetrySender.send(telemetryData);
  }

  private void populateTelemetryData(Map<String, Object> telemetryValues, TelemetryData telemetryData) {
    for (Entry<String, Object> entry : telemetryValues.entrySet()) {
      if ("telemetry_purpose".equals(entry.getKey())) {
        continue;
      }
      telemetryData.put(entry.getKey(), entry.getValue());
    }
  }
}
