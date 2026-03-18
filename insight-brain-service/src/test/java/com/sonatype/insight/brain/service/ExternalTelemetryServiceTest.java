/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

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

  @Mock
  private HttpServletRequest httpServletRequest;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Test
  public void testSendTelemetry_telemetryPurposeNull() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("violation_count", 1);

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telemetry purpose is required.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_bogusTelemetryPurpose() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "ADVANCED_SEARCH");

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telemetry purpose not supported.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_telemetryPurposeNotSupported() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "BOGUS_PURPOSE");

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Unknown telemetry purpose.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_SyncServiceTelemetry() {
    Mockito.when(httpServletRequest.getHeader("X-CLM-Client-User-Agent"))
        .thenReturn("SSC_Sync_Service/5.0.2 (Java 1.8.0_352; Mac OS X 10.16)");
    Mockito.when(httpServletRequest.getHeader("X-CLM-Client-Instance-Id"))
        .thenReturn("bf1809f09dad40ec86f1e13daf96fe8c");

    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "SYNC_SERVICE_METRICS");
    telemetryValues.put("violation_count", 1);

    externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("client_id", "SSC_Sync_Service");
    expectedAttributes.put("client_version", "5.0.2");
    expectedAttributes.put("client_runtime", "Java");
    expectedAttributes.put("client_runtime_version", "1.8.0_352");
    expectedAttributes.put("client_os_name", "Mac");
    expectedAttributes.put("client_os_version", "OS X 10.16");
    expectedAttributes.put("client_other", null);
    expectedAttributes.put("client_instance_id", "bf1809f09dad40ec86f1e13daf96fe8c");
    expectedAttributes.put("violation_count", 1);

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SYNC_SERVICE_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testSendTelemetry_IdeUserInteractionMetrics() {
    Mockito.when(httpServletRequest.getHeader("X-CLM-Client-User-Agent"))
        .thenReturn("IQ_VS_Code_Plugin/1.0.5 (Node 18.18.2; Darwin 23.5.0; VS Code 1.89.1)");
    Mockito.when(httpServletRequest.getHeader("X-CLM-Client-Instance-Id"))
        .thenReturn("bf1809f09dad40ec86f1e13daf96fe8c");

    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "IDE_USER_INTERACTION_METRICS");
    telemetryValues.put("recommended_version_clicks", 1);
    telemetryValues.put("version_graph_clicks", 1);

    externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("client_id", "IQ_VS_Code_Plugin");
    expectedAttributes.put("client_version", "1.0.5");
    expectedAttributes.put("client_runtime", "Node");
    expectedAttributes.put("client_runtime_version", "18.18.2");
    expectedAttributes.put("client_os_name", "Darwin");
    expectedAttributes.put("client_os_version", "23.5.0");
    expectedAttributes.put("client_other", "VS Code 1.89.1");
    expectedAttributes.put("client_instance_id", "bf1809f09dad40ec86f1e13daf96fe8c");
    expectedAttributes.put("recommended_version_clicks", 1);
    expectedAttributes.put("version_graph_clicks", 1);

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.IDE_USER_INTERACTION_METRICS);
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testSendTelemetry_JiraPluginConfigurationTelemetry() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "JIRA_PLUGIN_CONFIGURATION_METRICS");
    telemetryValues.put("aggregation_by_component_count", "1");
    telemetryValues.put("aggregation_by_iq_evaluation_count", "2");
    telemetryValues.put("automatic_workflow_transition_count", "3");
    telemetryValues.put("total_project_count", "5");

    externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest);

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
    Map<String, Object> telemetryValues = new HashMap<>();

    telemetryValues.put("telemetry_purpose", "JIRA_PLUGIN_USAGE_METRICS");
    telemetryValues.put("issue_count", "10");
    telemetryValues.put("transitioned_issue_count", "3");
    telemetryValues.put("client_id", "iq_for_jira");
    telemetryValues.put("client_version", "1.7.0");
    telemetryValues.put("client_runtime", "Java");
    telemetryValues.put("client_runtime_version", "1.8.0");
    telemetryValues.put("client_os_name", "Linux");
    telemetryValues.put("client_os_version", "5.14.0");
    telemetryValues.put("client_other", "Jira Server 9.0.0");
    telemetryValues.put("client_instance_id", "instance_id");

    externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("jira_plugin_issue_count", 10);
    expectedAttributes.put("jira_plugin_transitioned_issue_count", 3);
    expectedAttributes.put("client_id", "iq_for_jira");
    expectedAttributes.put("client_version", "1.7.0");
    expectedAttributes.put("client_runtime", "Java");
    expectedAttributes.put("client_runtime_version", "1.8.0");
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

  @Test
  public void testSendTelemetry_NoTelemetryValues() {
    Map<String, Object> telemetryValues = new HashMap<>();

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telemetry values are required.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_NullTelemetryValues() {
    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(null, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telemetry values are required.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_IntegrationsFeatureUsageMetricsTelemetry_FeatureMissing() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "INTEGRATIONS_FEATURE_USAGE_METRICS");
    telemetryValues.put("telemetry_purpose_one", "telemetry_purpose_one_value");
    telemetryValues.put("telemetry_purpose_two", 2);

    assertThatThrownBy(() -> externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telemetry property 'feature' is required.");

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendTelemetry_IntegrationsFeatureUsageMetricsTelemetry() {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "INTEGRATIONS_FEATURE_USAGE_METRICS");
    telemetryValues.put("feature", "a_feature");
    telemetryValues.put("telemetry_purpose_one", "telemetry_purpose_one_value");
    telemetryValues.put("telemetry_purpose_two", 2);

    externalTelemetryService.sendTelemetry(telemetryValues, httpServletRequest);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());

    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("feature", "a_feature");
    expectedAttributes.put("telemetry_purpose_one", "telemetry_purpose_one_value");
    expectedAttributes.put("telemetry_purpose_two", 2);

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.INTEGRATIONS_FEATURE_USAGE_METRICS);
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }
}
