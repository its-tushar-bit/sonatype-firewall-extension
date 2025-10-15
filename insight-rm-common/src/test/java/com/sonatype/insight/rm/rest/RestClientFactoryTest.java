/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.FirewallClient;
import com.sonatype.insight.brain.client.FirewallMigrationClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.rm.rest.RestClient.FirewallMigration;
import com.sonatype.insight.rm.rest.RestClient.Repository;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RestClientFactoryTest
{
  @Test
  public void testGetProprietaryConfigForApplicationEvaluation() throws Exception {
    ProprietaryConfig config = new ProprietaryConfig();
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getProprietaryConfigForApplicationEvaluation(eq("appId"))).thenReturn(config);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    assertThat(client.getProprietaryConfigForApplicationEvaluation("appId")).isSameAs(config);
  }

  @Test
  public void testGetProprietaryConfigForApplicationEvaluation_OldBrain() throws Exception {
    HttpResponseException hre = new HttpResponseException(404, "old brain");
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getProprietaryConfigForApplicationEvaluation(eq("appId"))).thenThrow(hre);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    assertThatExceptionOfType(HttpException.class)
        .isThrownBy(() -> client.getProprietaryConfigForApplicationEvaluation("appId")).satisfies(e -> {
          assertThat(e.getReason()).isEqualTo(hre.getMessage());
          assertThat(e.getStatus()).isEqualTo(hre.getStatusCode());
        });
  }

  @Test
  public void testGetApplicationSummaryList() throws Exception {
    ApplicationSummary summary = new ApplicationSummary();
    summary.setId("test-id");
    summary.setPublicId("test-public-id");
    summary.setName("test-name");

    List<ApplicationSummary> summaries = new ArrayList<>();
    summaries.add(summary);
    ApplicationSummaryList applicationSummaryList = new ApplicationSummaryList();
    applicationSummaryList.setApplicationSummaries(summaries);

    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getApplicationsForApplicationEvaluation()).thenReturn(applicationSummaryList);

    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());

    assertThat(client.getApplicationsForApplicationEvaluation()).isSameAs(applicationSummaryList);
  }

  @Test
  public void testRestClientRepository() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.setEnabled(true);

    verify(firewallClient).setEnabled(true);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_SetQuarantine() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.setQuarantine(true);

    verify(firewallClient).setQuarantine(true);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_RemoveComponent() throws Exception {
    FirewallClient firewallClient = mock(FirewallClient.class);

    String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    String repositoryPublicId = "repositoryPublicId";

    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.removeComponent("somepath");

    verify(firewallClient).removeComponent("somepath");
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_EvaluateComponents() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.evaluateComponents(null);

    verify(firewallClient).evaluateComponents(null);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_EvaluateComponentsAdhoc() throws Exception {
    FirewallClient firewallClient = mock(FirewallClient.class);

    String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    String repositoryPublicId = "repositoryPublicId";

    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.evaluateComponentsAdhoc(null);

    verify(firewallClient).evaluateComponentsAdhoc(null);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_EvaluateComponentWithQuarantine() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.evaluateComponentWithQuarantine(null);

    verify(firewallClient).evaluateComponentWithQuarantine(null);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_EvaluateComponentMetadata() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.evaluateComponentMetadata(null);

    verify(firewallClient).evaluateComponentMetadata(null);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_RemoveExtraComponents() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.removeExtraComponents(null);

    verify(firewallClient).removeExtraComponents(null);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_GetUnquarantinedComponents() throws Exception {
    final FirewallClient firewallClient = mock(FirewallClient.class);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.getUnquarantinedComponents(0L);

    verify(firewallClient).getUnquarantinedComponents(0L);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_GetUnquarantinedComponents_UnknownRepository() throws Exception {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    HttpResponseException httpResponseException = new HttpResponseException(404,
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManagerInstanceId +
            " and publicId=" + repositoryPublicId + ".");
    when(firewallClient.getUnquarantinedComponents(any(Long.class)))
        .thenThrow(httpResponseException);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> repository.getUnquarantinedComponents(0L))
        .isSameAs(httpResponseException);
    verify(firewallClient).getUnquarantinedComponents(0L);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_GetUnquarantinedComponents_MethodNotAllowedOlderIQServer() throws Exception {
    HttpResponseException httpResponseException = new HttpResponseException(405, "Method Not Allowed.");
    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.getUnquarantinedComponents(any(Long.class)))
        .thenThrow(httpResponseException);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> repository.getUnquarantinedComponents(0L)).withCause(httpResponseException);
    verify(firewallClient).getUnquarantinedComponents(0L);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_GetPolicyEvaluationSummary() throws Exception {
    RepositoryPolicyEvaluationSummary policyEvaluationSummary = new RepositoryPolicyEvaluationSummary();
    policyEvaluationSummary.setAffectedComponentCount(3);
    policyEvaluationSummary.setCriticalComponentCount(1);
    policyEvaluationSummary.setSevereComponentCount(1);
    policyEvaluationSummary.setModerateComponentCount(1);
    policyEvaluationSummary.setQuarantinedComponentCount(2);

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.getPolicyEvaluationSummary()).thenReturn(policyEvaluationSummary);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    assertThat(repository.getPolicyEvaluationSummary()).isSameAs(policyEvaluationSummary);
  }

  @Test
  public void testRestClientRepository_GetRepositoryResultsUrl() throws Exception {
    String repositoryResultsUrl = "https://example.com/iqReport";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.getRepositoryResultsUrl()).thenReturn(repositoryResultsUrl);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    assertThat(repository.getRepositoryResultsUrl()).isSameAs(repositoryResultsUrl);
  }

  @Test
  public void testRestClientRepository_GetQuarantinedComponentReport() throws Exception {
    QuarantinedComponentReport quarantinedComponentReport = new QuarantinedComponentReport();
    quarantinedComponentReport.setReportUrl("components/quarantinedComponentReportUrl");

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";
    final String pathname = "pathname";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.getQuarantinedComponentReport(pathname)).thenReturn(quarantinedComponentReport);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    assertThat(repository.getQuarantinedComponentReport(pathname)).isSameAs(quarantinedComponentReport);
  }

  @Test
  public void testRestClientRepository_isContainerImageQuarantined() throws Exception {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";
    final String containerImagePublicId = "containerImagePublicId";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.isContainerImageQuarantined(containerImagePublicId)).thenReturn(true);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);

    assertThat(repository.isContainerImageQuarantined(containerImagePublicId)).isTrue();
  }

  @Test
  public void testRestClientRepository_evaluateContainerImageWithPolling() throws Exception {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";
    final String bomJson = "{\"bomFormat\": \"CycloneDX\"}";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.evaluateContainerImageWithPolling(bomJson)).thenReturn(null);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
    repository.evaluateContainerImageWithPolling(bomJson);

    verify(firewallClient).evaluateContainerImageWithPolling(bomJson);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientRepository_getContainerImageReportUrl() throws Exception {
    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();
    String reportUrl = "ui/links/repository/containerImage/test-public-id/report";
    summary.setReportUrl(reportUrl);

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";
    final String containerImagePublicId = "test-public-id";

    final FirewallClient firewallClient = mock(FirewallClient.class);
    when(firewallClient.getContainerImageReportUrl(containerImagePublicId)).thenReturn(summary);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallClient(any(Configuration.class), eq(repositoryManagerInstanceId),
        eq(repositoryPublicId), eq(RepositoryManagerType.NEXUS));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final Repository repository =
        client.forRepository(repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);

    assertThat(repository.getContainerImageReportUrl(containerImagePublicId)).isSameAs(summary);
    verify(firewallClient).getContainerImageReportUrl(containerImagePublicId);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientFirewallMigration_verifyMigrationSupport() throws Exception {
    final FirewallMigrationClient firewallClient = mock(FirewallMigrationClient.class);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallMigrationClient(any(Configuration.class));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final FirewallMigration repository = client.forFirewallMigration();
    repository.verifyMigrationSupport("v1");

    verify(firewallClient).verifyMigrationSupport("v1");
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientFirewallMigration_migrateRepositoryHistory() throws Exception {
    final FirewallMigrationClient firewallClient = mock(FirewallMigrationClient.class);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallMigrationClient(any(Configuration.class));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final FirewallMigration repository = client.forFirewallMigration();

    final String targetRepositoryManagerInstanceId = "targetRepositoryManagerInstanceId";
    final String targetRepositoryPublicId = "targetRepositoryPublicId";
    final String sourceRepositoryManagerInstanceId = "sourceRepositoryManagerInstanceId";
    final String sourceRepositoryPublicId = "sourceRepositoryPublicId";

    repository.migrateRepositoryHistory(sourceRepositoryManagerInstanceId, sourceRepositoryPublicId,
        targetRepositoryManagerInstanceId, targetRepositoryPublicId);

    verify(firewallClient).migrateRepositoryHistory(sourceRepositoryManagerInstanceId, sourceRepositoryPublicId,
        targetRepositoryManagerInstanceId, targetRepositoryPublicId);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testRestClientFirewallMigration_getRepositoryMigrationState() throws Exception {
    final FirewallMigrationClient firewallClient = mock(FirewallMigrationClient.class);

    final RestClientFactory factory = spy(new RestClientFactory());
    doReturn(firewallClient).when(factory).newFirewallMigrationClient(any(Configuration.class));

    final RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    final FirewallMigration repository = client.forFirewallMigration();

    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String repositoryPublicId = "repositoryPublicId";

    repository.getRepositoryMigrationState(repositoryManagerInstanceId, repositoryPublicId);

    verify(firewallClient).getRepositoryMigrationState(repositoryManagerInstanceId, repositoryPublicId);
    verifyNoMoreInteractions(firewallClient);
  }

  @Test
  public void testGetFirewallIgnorePatterns() throws Exception {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getFirewallIgnorePatterns()).thenReturn(firewallIgnorePatterns);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    assertThat(client.getFirewallIgnorePatterns()).isSameAs(firewallIgnorePatterns);
  }

  @Test
  public void testGetFirewallIgnorePatterns_OlderIQServer() throws Exception {
    HttpResponseException httpResponseException = new HttpResponseException(404, "Resource not found");
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getFirewallIgnorePatterns()).thenThrow(httpResponseException);

    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));

    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(client::getFirewallIgnorePatterns)
        .withCause(httpResponseException).withMessage("IQ Server doesn't support firewall ignore patterns, "
        + "upgrade it to version 1.35, or newer, to support it.");
    verify(configClient).getFirewallIgnorePatterns();
    verifyNoMoreInteractions(configClient);
  }

  @Test
  public void testValidateServerVersion() throws Exception {
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());

    // test that the interals are properly called - exceptions are re-thrown otherwise it falls through
    doThrow(IOException.class).when(configClient).validateServerVersion("throw");
    doNothing().when(configClient).validateServerVersion("do-not-throw");
    assertThatThrownBy(() -> client.validateServerVersion("throw")).isInstanceOf(IOException.class);
    client.validateServerVersion("do-not-throw");
  }
}
