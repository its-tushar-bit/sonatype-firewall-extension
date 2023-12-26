/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ExternalTelemetryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ExternalTelemetryService externalTelemetryService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Test
  public void testSendTelemetry_telemetryPurposeNull() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("ssc_integration_service_version", "1");
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", "false");

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry("user-agent", telemetryValues))
        .isInstanceOf(BadRequestException.class).hasMessage("Telemetry purpose is required.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_bogusTelemetryPurpose() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "ADVANCED_SEARCH");
    telemetryValues.put("ssc_integration_service_version", "1");
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", "false");

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry("user-agent", telemetryValues))
        .isInstanceOf(BadRequestException.class).hasMessage("Telemetry purpose not supported.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_telemetryPurposeNotSupported() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "BOGUS_PURPOSE");
    telemetryValues.put("ssc_integration_service_version", "1");
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", "false");

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry("user-agent", telemetryValues))
        .isInstanceOf(BadRequestException.class).hasMessage("Unknown telemetry purpose.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_SscIntegrationTelemetry() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "SSC_INTEGRATION_METRICS");
    telemetryValues.put("ssc_integration_service_version", "1");
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", null);

    externalTelemetryService.sendTelemetry("user-agent", telemetryValues);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("ssc_integration_service_version", "1");
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("1234-foo"));
    expectedAttributes.put("real_application_id", "1234-foo");
    expectedAttributes.put("overwrite", true);
    expectedAttributes.put("force_upload", null);
    expectedAttributes.put("user_agent", "user-agent");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SSC_INTEGRATION_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testSendTelemetry_SscIntegrationTelemetry_Limits() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "SSC_INTEGRATION_METRICS");
    telemetryValues.put("ssc_integration_service_version", RandomStringUtils.random(60, "a"));
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", "false");

    externalTelemetryService.sendTelemetry(RandomStringUtils.random(600, "b"), telemetryValues);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("ssc_integration_service_version", RandomStringUtils.random(50, "a"));
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("1234-foo"));
    expectedAttributes.put("overwrite", true);
    expectedAttributes.put("force_upload", false);
    expectedAttributes.put("user_agent", RandomStringUtils.random(500, "b"));
    expectedAttributes.put("real_application_id", "1234-foo");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SSC_INTEGRATION_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testSendTelemetry_JiraPluginConfigurationTelemetry() {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "JIRA_PLUGIN_CONFIGURATION_METRICS");
    telemetryValues.put("aggregation_by_component_count", "1");
    telemetryValues.put("aggregation_by_iq_evaluation_count", "2");
    telemetryValues.put("automatic_workflow_transition_count", "3");
    telemetryValues.put("total_project_count", "5");

    externalTelemetryService.sendTelemetry("user-agent", telemetryValues);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("jira_plugin_aggregation_by_component_count", 1);
    expectedAttributes.put("jira_plugin_aggregation_by_iq_evaluation_count", 2);
    expectedAttributes.put("jira_plugin_automatic_workflow_transition_count", 3);
    expectedAttributes.put("jira_plugin_total_project_count", 5);

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.JIRA_PLUGIN_CONFIGURATION_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testSendTelemetry_JiraPluginUsageTelemetry() {
    Map<String, String> telemetryValues = new HashMap<>();

    telemetryValues.put("telemetry_purpose", "JIRA_PLUGIN_USAGE_METRICS");
    telemetryValues.put("issue_count", "10");
    telemetryValues.put("transitioned_issue_count", "3");
    telemetryValues.put("client_id", "iq_for_jira");
    telemetryValues.put("client_version", "1.7.0");
    telemetryValues.put("client_runtime", "Java");
    telemetryValues.put("client_runtime_version","1.8.0");
    telemetryValues.put("client_os_name", "Linux");
    telemetryValues.put("client_os_version", "5.14.0");
    telemetryValues.put("client_other", "Jira Server 9.0.0");
    telemetryValues.put("client_instance_id", "instance_id");

    externalTelemetryService.sendTelemetry("user-agent", telemetryValues);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("jira_plugin_issue_count", 10);
    expectedAttributes.put("jira_plugin_transitioned_issue_count", 3);
    expectedAttributes.put("client_id", "iq_for_jira");
    expectedAttributes.put("client_version", "1.7.0");
    expectedAttributes.put("client_runtime", "Java");
    expectedAttributes.put("client_runtime_version","1.8.0");
    expectedAttributes.put("client_os_name", "Linux");
    expectedAttributes.put("client_os_version", "5.14.0");
    expectedAttributes.put("client_other", "Jira Server 9.0.0");
    expectedAttributes.put("client_instance_id", "instance_id");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.JIRA_PLUGIN_USAGE_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }
}
