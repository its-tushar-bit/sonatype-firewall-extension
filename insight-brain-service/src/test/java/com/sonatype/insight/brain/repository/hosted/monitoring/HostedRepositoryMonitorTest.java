/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.dataaccess.TransactionContext;

import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

public class HostedRepositoryMonitorTest
    extends AbstractComponentTest
{
  @Inject
  private HostedRepositoryMonitor hostedRepositoryMonitor;

  @Mock
  private RepositoryDAO repositoryDAOMock;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAOMock;

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluatorMock;

  @Mock
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAOMock;

  @Mock
  private TransactionContext transactionContextMock;

  @Before
  public void applyOverrides() {
    applyBeanFieldOverride(HostedRepositoryMonitor.class, "repositoryDAO", repositoryDAOMock);
    applyBeanFieldOverride(HostedRepositoryMonitor.class, "repositoryComponentDAO", repositoryComponentDAOMock);
    applyBeanFieldOverride(HostedRepositoryMonitor.class, "repositoryPolicyEvaluator", repositoryPolicyEvaluatorMock);
    applyBeanFieldOverride(HostedRepositoryMonitor.class, "repositoryPolicyViolationDAO",
        repositoryPolicyViolationDAOMock);
  }

  @Before
  public void enableFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    lenient().when(repositoryPolicyViolationDAOMock.createTransactionContext()).thenReturn(transactionContextMock);
    lenient().when(repositoryComponentDAOMock.createTransactionContext()).thenReturn(transactionContextMock);
  }

  @After
  public void resetFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testRun_featureFlagDisabled_skips() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    hostedRepositoryMonitor.run();

    verifyNoInteractions(repositoryDAOMock);
    verifyNoInteractions(repositoryPolicyEvaluatorMock);
  }

  @Test
  public void testRun_noMonitoredRepos_skips() {
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of());

    hostedRepositoryMonitor.run();

    verifyNoInteractions(repositoryComponentDAOMock);
    verifyNoInteractions(repositoryPolicyEvaluatorMock);
  }

  @Test
  public void testRun_repoWithNoComponents_skips() {
    Repository repo = repoWithId("repo1", "maven2");
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of());

    hostedRepositoryMonitor.run();

    verifyNoInteractions(repositoryPolicyEvaluatorMock);
  }

  @Test
  public void testRun_repoWithComponents_evaluatesWithCorrectRequest() {
    Repository repo = repoWithId("repo1", "maven2");
    RepositoryComponent component = componentWithHashPathnameAndStage("abc123", "/com/example/foo-1.0.jar",
        ComplianceStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));

    hostedRepositoryMonitor.run();

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> requestCaptor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), requestCaptor.capture(),
        eq(ComplianceStageType.ID));

    RepositoryComponentEvaluationDataRequestList request = requestCaptor.getValue();
    assertThat(request.components).hasSize(1);
    assertThat(request.components.get(0).format).isEqualTo("maven2");
    assertThat(request.components.get(0).pathname).isEqualTo("/com/example/foo-1.0.jar");
    assertThat(request.components.get(0).hash).isEqualTo("abc123");
  }

  @Test
  public void testRun_componentMissingHash_skipped() {
    Repository repo = repoWithId("repo1", "npm");
    RepositoryComponent noHash = componentWithHashPathnameAndStage(null, "/package/foo-1.0.tgz",
        ComplianceStageType.ID);
    RepositoryComponent valid = componentWithHashPathnameAndStage("def456", "/package/bar-2.0.tgz",
        ComplianceStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(noHash, valid));

    hostedRepositoryMonitor.run();

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> requestCaptor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), requestCaptor.capture(),
        eq(ComplianceStageType.ID));

    assertThat(requestCaptor.getValue().components).hasSize(1);
    assertThat(requestCaptor.getValue().components.get(0).hash).isEqualTo("def456");
  }

  @Test
  public void testRun_componentMissingPathname_skipped() {
    Repository repo = repoWithId("repo1", "npm");
    RepositoryComponent noPathname = componentWithHashPathnameAndStage("abc123", null, ComplianceStageType.ID);
    RepositoryComponent valid = componentWithHashPathnameAndStage("def456", "/package/bar-2.0.tgz",
        ComplianceStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(noPathname, valid));

    hostedRepositoryMonitor.run();

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> requestCaptor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), requestCaptor.capture(),
        eq(ComplianceStageType.ID));

    assertThat(requestCaptor.getValue().components).hasSize(1);
    assertThat(requestCaptor.getValue().components.get(0).hash).isEqualTo("def456");
  }

  @Test
  public void testRun_allComponentsMissingHashOrPathname_skipsEvaluation() {
    Repository repo = repoWithId("repo1", "maven2");
    RepositoryComponent noHash = componentWithHashPathnameAndStage(null, "/path/foo.jar", ComplianceStageType.ID);
    RepositoryComponent noPathname = componentWithHashPathnameAndStage("abc123", null, ComplianceStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(noHash, noPathname));

    hostedRepositoryMonitor.run();

    verifyNoInteractions(repositoryPolicyEvaluatorMock);
  }

  @Test
  public void testRun_exceptionInOneRepo_continuesOtherRepos() {
    Repository repo1 = repoWithId("repo1", "maven2");
    Repository repo2 = repoWithId("repo2", "npm");
    RepositoryComponent component = componentWithHashPathnameAndStage("abc123", "/path/foo.jar",
        ComplianceStageType.ID);

    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo1, repo2));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo2"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));
    when(repositoryPolicyEvaluatorMock.evaluateForMonitoring(eq(repo1), any(), any()))
        .thenThrow(new RuntimeException("HDS unavailable"));

    hostedRepositoryMonitor.run();

    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo2), any(), any());
  }

  @Test
  public void testRun_repoWithNullFormat_skipsEvaluation() {
    Repository repo = repoWithId("repo1", null);
    RepositoryComponent component = componentWithHashPathnameAndStage("abc123", "/path/foo.jar",
        ComplianceStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));

    hostedRepositoryMonitor.run();

    verifyNoInteractions(repositoryPolicyEvaluatorMock);
  }

  @Test
  public void testRun_componentWithRecordedStage_usesThatStage() {
    Repository repo = repoWithId("repo1", "maven2");
    RepositoryComponent component = componentWithHashPathnameAndStage("abc123", "/path/foo.jar",
        ProxyStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));

    hostedRepositoryMonitor.run();

    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), any(), eq(ProxyStageType.ID));
  }

  @Test
  public void testRun_componentWithNullStage_fallsBackToComplianceStage() {
    Repository repo = repoWithId("repo1", "maven2");
    RepositoryComponent component = componentWithHashPathnameAndStage("abc123", "/path/foo.jar", null);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(component));

    hostedRepositoryMonitor.run();

    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), any(), eq(ComplianceStageType.ID));
  }

  @Test
  public void testRun_stageReadFromFirstComponentWithNonNullStage() {
    Repository repo = repoWithId("repo1", "maven2");
    RepositoryComponent noStage = componentWithHashPathnameAndStage("abc123", "/path/foo.jar", null);
    RepositoryComponent withStage = componentWithHashPathnameAndStage("def456", "/path/bar.jar", ProxyStageType.ID);
    when(repositoryDAOMock.getHostedRepositoriesWithMonitoringEnabled()).thenReturn(List.of(repo));
    when(repositoryComponentDAOMock.getByRepositoryId(any(), eq("repo1"),
        eq(HostedRepositoryMonitor.COMPONENT_PAGE_SIZE), eq(0))).thenReturn(List.of(noStage, withStage));

    hostedRepositoryMonitor.run();

    verify(repositoryPolicyEvaluatorMock).evaluateForMonitoring(eq(repo), any(), eq(ProxyStageType.ID));
  }

  // --- helpers ---

  private Repository repoWithId(String id, String format) {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setFormat(format);
    return repo;
  }

  private RepositoryComponent componentWithHashPathnameAndStage(String hash, String pathname, String stage) {
    RepositoryComponent component = new RepositoryComponent();
    component.setHash(hash);
    component.setPathname(pathname);
    component.setLastEvaluationStage(stage);
    return component;
  }
}
