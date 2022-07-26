/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

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

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void sendTelemetry(String userAgent, Map<String, String> telemetryValues) {
    String purpose = telemetryValues.get("telemetry_purpose");
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

    switch (telemetryPurpose) {
      case SSC_INTEGRATION_METRICS:
        sendSscIntegrationMetricsTelemetry(userAgent, telemetryValues);
        break;
      case JIRA_PLUGIN_CONFIGURATION_METRICS:
        sendJiraPluginConfigurationTelemetry(telemetryValues);
        break;
      case JIRA_PLUGIN_USAGE_METRICS:
        sendJiraPluginUsageTelemetry(telemetryValues);
        break;
      default:
        log.info("External telemetry endpoint called with an unsupported `telemetry_purpose`.");
        throw new BadRequestException("Telemetry purpose not supported.");
    }
  }

  private void sendSscIntegrationMetricsTelemetry(String userAgent, Map<String, String> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SSC_INTEGRATION_METRICS);

    String sscIntegrationServiceVersion = telemetryValues.get("ssc_integration_service_version");
    if (sscIntegrationServiceVersion != null && sscIntegrationServiceVersion.length() > 50) {
      sscIntegrationServiceVersion = sscIntegrationServiceVersion.substring(0, 50);
    }

    telemetryData.put("ssc_integration_service_version", sscIntegrationServiceVersion);
    telemetryData.put("application_id", HdsClientAnalytics.obfuscate(telemetryValues.get("application_id")));
    telemetryData.put("overwrite", Boolean.valueOf(telemetryValues.get("overwrite")));
    String forceUpload = telemetryValues.get("force_upload");
    if (forceUpload != null) {
      telemetryData.put("force_upload", Boolean.valueOf(forceUpload));
    }
    else {
      telemetryData.put("force_upload", null);
    }

    if (userAgent != null && userAgent.length() > 500) {
      userAgent = userAgent.substring(0, 500);
    }
    telemetryData.put("user_agent", userAgent);

    telemetrySender.send(telemetryData);
  }

  private void sendJiraPluginConfigurationTelemetry(Map<String, String> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.JIRA_PLUGIN_CONFIGURATION_METRICS);

    telemetryData.put("jira_plugin_aggregation_by_component_count",
        Integer.valueOf(telemetryValues.get("aggregation_by_component_count")));
    telemetryData.put("jira_plugin_aggregation_by_iq_evaluation_count",
        Integer.valueOf(telemetryValues.get("aggregation_by_iq_evaluation_count")));
    telemetryData.put("jira_plugin_automatic_workflow_transition_count",
        Integer.valueOf(telemetryValues.get("automatic_workflow_transition_count")));
    telemetryData.put("jira_plugin_total_project_count",
        Integer.valueOf(telemetryValues.get("total_project_count")));

    telemetrySender.send(telemetryData);
  }

  private void sendJiraPluginUsageTelemetry(Map<String, String> telemetryValues) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.JIRA_PLUGIN_USAGE_METRICS);

    telemetryData.put("jira_plugin_issue_count",
        Integer.valueOf(telemetryValues.get("issue_count")));
    telemetryData.put("jira_plugin_transitioned_issue_count",
        Integer.valueOf(telemetryValues.get("transitioned_issue_count")));

    for (Entry<String, String> entry : telemetryValues.entrySet()) {
      if (entry.getKey().startsWith("client_")) {
        telemetryData.put(entry.getKey(), entry.getValue());
      }
    }

    telemetrySender.send(telemetryData);
  }
}
