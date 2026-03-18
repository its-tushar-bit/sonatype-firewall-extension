/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.scan.matcher.firewall.RepositoryPathnameParser;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

public class CascadeReevaluationTaskTest
    extends AbstractComponentTest
{
  private CascadeReevaluationTask task;

  private Repository repository1;

  private Repository repository2;

  private Repository repository3;

  private RepositoryComponent component1InRepo1;

  private RepositoryComponent component2InRepo1;

  private RepositoryComponent component1InRepo2;

  private RepositoryComponent unrelatedComponent;

  @Inject
  private ReevaluateCascadeProgressDAO cascadeProgressDAO;

  @Inject
  private ReevaluateCascadeRequestDAO cascadeRequestDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private AsyncEventBus mockEventBus;

  @Mock
  private RepositoryPolicyEvaluator mockRepositoryPolicyEvaluator;

  @Inject
  private ComponentPolicyEvaluator componentPolicyEvaluator;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  private RepositoryComponentDeleteService repositoryComponentDeleteService;

  @Inject
  private RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private RepositoryPathnameParser repositoryPathnameParser;

  private String cascadeRequestId;

  private final String targetComponentHash = "target_hash_456";

  @Override
  public void configure(Binder binder) {
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(AsyncEventBus.class).toInstance(mockEventBus);
    binder.bind(RepositoryPolicyEvaluator.class).toInstance(mockRepositoryPolicyEvaluator);
    super.configure(binder);
  }

  /*
   * Setup:
   * - 3 repositories
   * - Repository 1: 2 components with target hash, 1 unrelated component
   * - Repository 2: 1 component with target hash
   * - Repository 3: no components with target hash
   */
  @Before
  public void setup() throws Exception {
    // Generate unique cascade request ID for this test execution
    cascadeRequestId = "main_cascade_test_" + System.currentTimeMillis();

    // Create repositories
    repository1 = tempEntity.newRepository("test-repo-1");
    repository2 = tempEntity.newRepository("test-repo-2");
    repository3 = tempEntity.newRepository("test-repo-3");

    Date now = new Date();

    // Create components with target hash in repository 1
    component1InRepo1 = tempEntity.newRepositoryComponent(repository1.getId(),
        MatchState.EXACT, "test/path/component1", targetComponentHash,
        ComponentIdentifier.createNpmCoordinates("test-pkg-1", "1.0.0"), now, now);

    component2InRepo1 = tempEntity.newRepositoryComponent(repository1.getId(),
        MatchState.EXACT, "test/path/component2", targetComponentHash,
        ComponentIdentifier.createNpmCoordinates("test-pkg-2", "1.0.0"), now, now);

    // Create component with target hash in repository 2
    component1InRepo2 = tempEntity.newRepositoryComponent(repository2.getId(),
        MatchState.EXACT, "test/path/component3", targetComponentHash,
        ComponentIdentifier.createNpmCoordinates("test-pkg-3", "1.0.0"), now, now);

    // Create unrelated component in repository 1 (different hash)
    unrelatedComponent = tempEntity.newRepositoryComponent(repository1.getId(),
        MatchState.EXACT, "test/path/unrelated", "different_hash_789",
        ComponentIdentifier.createNpmCoordinates("unrelated-pkg", "1.0.0"), now, now);

    // Create the cascade request record (required for foreign key constraint)
    tempEntity.newReevaluateCascadeRequest(cascadeRequestId, targetComponentHash, "testuser");

    createPolicy();

    task = new CascadeReevaluationTask(cascadeRequestId, targetComponentHash,
        cascadeProgressDAO, cascadeRequestDAO, repositoryComponentDAO, mockRepositoryPolicyEvaluator);
  }

  private void createHdsResponse() {
    createHdsResponse(false, false, false);
  }

  private void createHdsResponse(
      boolean quarantineComponent1InRepo1,
      boolean quarantineComponent2InRepo1,
      boolean quarantineComponent1InRepo2)
  {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    lenient().when(auditHdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    // Ensure the components list is initialized
    if (response.components == null) {
      response.components = new java.util.ArrayList<>();
    }
    response.components.add(createComponentResponse(component1InRepo1.getHash(),
        component1InRepo1.getComponentIdentifier(), MatchState.EXACT.getId(), 0));
    response.components.add(createComponentResponse(component2InRepo1.getHash(),
        component2InRepo1.getComponentIdentifier(), MatchState.EXACT.getId(), 1));
    response.components.add(createComponentResponse(component1InRepo2.getHash(),
        component1InRepo2.getComponentIdentifier(), MatchState.EXACT.getId(), 2));

    setupPolicyEvaluationMock(quarantineComponent1InRepo1, quarantineComponent2InRepo1,
        quarantineComponent1InRepo2);
  }

  private void setupPolicyEvaluationMock(
      boolean quarantineComponent1InRepo1,
      boolean quarantineComponent2InRepo1,
      boolean quarantineComponent1InRepo2)
  {
    lenient().when(mockRepositoryPolicyEvaluator.evaluate(any(Repository.class),
        any(RepositoryComponentEvaluationDataRequestList.class),
        anyBoolean(),
        any()))
        .thenAnswer(invocation -> {
          RepositoryComponentEvaluationDataRequestList request = invocation.getArgument(1);

          RepositoryComponentEvaluationDataList evaluationDataList = new RepositoryComponentEvaluationDataList();
          evaluationDataList.componentEvalResults = new java.util.ArrayList<>();

          // Return appropriate quarantine values based on request size
          if (request.components.size() == 2) {
            // This is repository1 with 2 components
            RepositoryComponentEvaluationData result1 = new RepositoryComponentEvaluationData();
            result1.requestIndex = 0;
            result1.quarantine = quarantineComponent1InRepo1;
            result1.catalogDate = new Date();
            evaluationDataList.componentEvalResults.add(result1);

            RepositoryComponentEvaluationData result2 = new RepositoryComponentEvaluationData();
            result2.requestIndex = 1;
            result2.quarantine = quarantineComponent2InRepo1;
            result2.catalogDate = new Date();
            evaluationDataList.componentEvalResults.add(result2);
          }
          else if (request.components.size() == 1) {
            // This is repository2 with 1 component
            RepositoryComponentEvaluationData result1 = new RepositoryComponentEvaluationData();
            result1.requestIndex = 0;
            result1.quarantine = quarantineComponent1InRepo2;
            result1.catalogDate = new Date();
            evaluationDataList.componentEvalResults.add(result1);
          }

          return evaluationDataList;
        });
  }

  private ComponentEvaluationData createComponentResponse(
      String hash,
      ComponentIdentifier identifier,
      String matchState,
      int index)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = identifier;
    componentEvaluationData.declaredLicenses = Collections.emptySet();
    componentEvaluationData.observedLicenses = Collections.emptySet();
    componentEvaluationData.matchState = matchState;
    componentEvaluationData.securityVulnerabilities = Collections.singletonList(
        new SecurityVulnerability("cve", "CVE-2023-1234", 7.5f));
    return componentEvaluationData;
  }

  private RepositoryPolicyEvaluator createRepositoryPolicyEvaluator() {
    return new RepositoryPolicyEvaluator(
        componentPolicyEvaluator, repositoryComponentDAO, repositoryPolicyViolationDAO,
        policyDAO, auditHdsClient, null, policyViolationLoggerFactory, firewallIgnorePatternService,
        componentDetailsLoaderFactory, repositoryComponentDeleteService, repositoryPolicyAlertEmailer,
        repositoryComponentTelemetryCreator, clusterLockManager, mockEventBus, firewallMetricsService,
        repositoryPathnameParser);
  }

  private Policy createPolicy() {
    return tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-cascade-policy", 7);
  }

  @Test
  public void testCascadeReevaluationTask_Success() {
    // Setup dynamic mock that returns appropriate results based on request size (like WithQuarantinedComponent test)
    lenient().when(mockRepositoryPolicyEvaluator.evaluate(any(Repository.class),
        any(RepositoryComponentEvaluationDataRequestList.class),
        anyBoolean(),
        any()))
        .thenAnswer(invocation -> {
          RepositoryComponentEvaluationDataRequestList request = invocation.getArgument(1);

          RepositoryComponentEvaluationDataList evaluationDataList = new RepositoryComponentEvaluationDataList();
          evaluationDataList.componentEvalResults = new java.util.ArrayList<>();

          // Return appropriate quarantine values based on request size (all false for Success test)
          for (int i = 0; i < request.components.size(); i++) {
            RepositoryComponentEvaluationData result = new RepositoryComponentEvaluationData();
            result.requestIndex = i;
            result.quarantine = false; // Success test expects no quarantine
            result.catalogDate = new Date();
            evaluationDataList.componentEvalResults.add(result);
          }

          return evaluationDataList;
        });

    // Act
    task.run();

    // Assert - Verify progress records were created for components with target hash
    List<ReevaluateCascadeProgress> allProgress = cascadeProgressDAO.getByRequestId(cascadeRequestId);
    assertThat(allProgress).hasSize(3); // 2 in repo1 + 1 in repo2 + 0 in repo3

    // Verify progress for repository 1 (2 components)
    List<ReevaluateCascadeProgress> repo1Progress = cascadeProgressDAO.getByRepositoryId(repository1.getId());
    List<ReevaluateCascadeProgress> repo1CascadeProgress = repo1Progress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .toList();
    assertThat(repo1CascadeProgress).hasSize(2);

    for (ReevaluateCascadeProgress progress : repo1CascadeProgress) {
      assertThat(progress.getReevaluateCascadeRequestId()).isEqualTo(cascadeRequestId);
      assertThat(progress.getRepositoryId()).isEqualTo(repository1.getId());
      assertThat(progress.getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);
      // Verify the component ID matches one of our test components
      assertThat(progress.getRepositoryComponentId())
          .isIn(component1InRepo1.getId(), component2InRepo1.getId());

      // Check quarantine status matches mock evaluation results (Success test expects no quarantine)
      if (progress.getRepositoryComponentId().equals(component1InRepo1.getId())) {
        assertThat(progress.isQuarantined()).isFalse(); // Mock returns quarantine=false
      }
      else if (progress.getRepositoryComponentId().equals(component2InRepo1.getId())) {
        assertThat(progress.isQuarantined()).isFalse(); // Mock returns quarantine=false
      }
    }

    // Verify progress for repository 2 (1 component)
    List<ReevaluateCascadeProgress> repo2Progress = cascadeProgressDAO.getByRepositoryId(repository2.getId());
    List<ReevaluateCascadeProgress> repo2CascadeProgress = repo2Progress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .toList();
    assertThat(repo2CascadeProgress).hasSize(1);

    ReevaluateCascadeProgress repo2ProgressRecord = repo2CascadeProgress.get(0);
    assertThat(repo2ProgressRecord.getReevaluateCascadeRequestId()).isEqualTo(cascadeRequestId);
    assertThat(repo2ProgressRecord.getRepositoryId()).isEqualTo(repository2.getId());
    assertThat(repo2ProgressRecord.getRepositoryComponentId()).isEqualTo(component1InRepo2.getId());
    assertThat(repo2ProgressRecord.getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);
    assertThat(repo2ProgressRecord.isQuarantined()).isFalse(); // Mock returns quarantine=false

    // Verify no progress records for repository 3 (no matching components)
    List<ReevaluateCascadeProgress> repo3Progress = cascadeProgressDAO.getByRepositoryId(repository3.getId());
    List<ReevaluateCascadeProgress> repo3CascadeProgress = repo3Progress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .toList();
    assertThat(repo3CascadeProgress).isEmpty();

    // Verify no progress record was created for the unrelated component
    List<ReevaluateCascadeProgress> unrelatedProgress = cascadeProgressDAO.getByRepositoryComponentId(
        unrelatedComponent.getId());
    List<ReevaluateCascadeProgress> unrelatedCascadeProgress = unrelatedProgress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .toList();
    assertThat(unrelatedCascadeProgress).isEmpty();
  }

  @Test
  public void testCascadeReevaluationTask_WithQuarantinedComponent() {
    // Create a simple mock that returns quarantined=true for all components in repository1
    RepositoryComponentEvaluationDataList repo1Results = new RepositoryComponentEvaluationDataList();
    repo1Results.componentEvalResults = new java.util.ArrayList<>();

    // Add results for both components in repository1 - make BOTH quarantined for simplicity
    RepositoryComponentEvaluationData result1 = new RepositoryComponentEvaluationData();
    result1.requestIndex = 0;
    result1.quarantine = true; // This should make component1InRepo1 quarantined
    result1.catalogDate = new Date();
    repo1Results.componentEvalResults.add(result1);

    RepositoryComponentEvaluationData result2 = new RepositoryComponentEvaluationData();
    result2.requestIndex = 1;
    result2.quarantine = true; // Make this one quarantined too for simplicity
    result2.catalogDate = new Date();
    repo1Results.componentEvalResults.add(result2);

    // Setup EXTREMELY permissive mock to catch ANY call to evaluate method
    lenient().when(mockRepositoryPolicyEvaluator.evaluate(any(Repository.class),
        any(RepositoryComponentEvaluationDataRequestList.class),
        anyBoolean(),
        any()))
        .thenReturn(repo1Results);

    // Arrange - Set one component as quarantined (this tests the end-to-end flow)
    component1InRepo1.setQuarantineTime(new Date());
    repositoryComponentDAO.update(component1InRepo1);

    // Act
    task.run();

    // Assert - Verify quarantined status is updated based on fresh evaluation results
    List<ReevaluateCascadeProgress> repo1Progress = cascadeProgressDAO.getByRepositoryId(repository1.getId());
    List<ReevaluateCascadeProgress> quarantinedProgress = repo1Progress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .filter(p -> component1InRepo1.getId().equals(p.getRepositoryComponentId()))
        .toList();

    assertThat(quarantinedProgress).hasSize(1);
    ReevaluateCascadeProgress progress = quarantinedProgress.get(0);
    assertThat(progress.isQuarantined()).isTrue(); // Should be true based on mock evaluation result
    assertThat(progress.getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);
  }

  @Test
  public void testCascadeReevaluationTask_NoRepositoryWithComponents() {
    String nonExistentHash = "empty_test_hash";
    String emptyCascadeId = "empty_test_cascade_" + System.currentTimeMillis();

    // Create the cascade request record (required for foreign key constraint)
    tempEntity.newReevaluateCascadeRequest(emptyCascadeId, nonExistentHash, "testuser");

    CascadeReevaluationTask emptyTask = new CascadeReevaluationTask(
        emptyCascadeId, nonExistentHash,
        cascadeProgressDAO, cascadeRequestDAO, repositoryComponentDAO, createRepositoryPolicyEvaluator());

    emptyTask.run();

    List<ReevaluateCascadeProgress> allProgress = cascadeProgressDAO.getByRequestId(emptyCascadeId);
    assertThat(allProgress).isEmpty();
  }

  @Test
  public void testGetByRequestId_ReturnsOnlyRequestSpecificProgress() {
    createHdsResponse();
    // Arrange - Run task to create progress records
    task.run();

    // Create another cascade request to ensure proper filtering
    String otherCascadeRequestId = "other_test_cascade_" + System.currentTimeMillis();

    tempEntity.newReevaluateCascadeRequest(otherCascadeRequestId, targetComponentHash, "testuser");

    CascadeReevaluationTask otherTask = new CascadeReevaluationTask(
        otherCascadeRequestId, targetComponentHash,
        cascadeProgressDAO, cascadeRequestDAO, repositoryComponentDAO, mockRepositoryPolicyEvaluator);
    otherTask.run();

    List<ReevaluateCascadeProgress> originalProgress = cascadeProgressDAO.getByRequestId(cascadeRequestId);
    List<ReevaluateCascadeProgress> otherProgress = cascadeProgressDAO.getByRequestId(otherCascadeRequestId);

    assertThat(originalProgress).hasSize(3); // 2 in repo1 + 1 in repo2
    assertThat(otherProgress).hasSize(3); // 2 in repo1 + 1 in repo2

    // Verify request IDs are correct
    for (ReevaluateCascadeProgress progress : originalProgress) {
      assertThat(progress.getReevaluateCascadeRequestId()).isEqualTo(cascadeRequestId);
    }
    for (ReevaluateCascadeProgress progress : otherProgress) {
      assertThat(progress.getReevaluateCascadeRequestId()).isEqualTo(otherCascadeRequestId);
    }
  }

  @Test
  public void testCascadeReevaluationTask_CompletedStatusCounts() {
    createHdsResponse();
    // Act
    task.run();

    // Assert - All components should be marked as completed
    long pendingCount = cascadeProgressDAO.countPendingByRequestId(cascadeRequestId);
    long completedCount = cascadeProgressDAO.countCompletedByRequestId(cascadeRequestId);
    long failedCount = cascadeProgressDAO.countFailedByRequestId(cascadeRequestId);

    assertThat(pendingCount).isEqualTo(0);
    assertThat(completedCount).isEqualTo(3); // 2 in repo1 + 1 in repo2
    assertThat(failedCount).isEqualTo(0);

    // Verify request is considered complete
    boolean isComplete = cascadeProgressDAO.isRequestComplete(cascadeRequestId);
    assertThat(isComplete).isTrue();
  }

  @Test
  public void testCascadeReevaluationTask_QuarantineStatusUpdatedFromEvaluationResults() {
    // Arrange - Reset components to NOT quarantined state
    component1InRepo1.setQuarantineTime(null);
    component2InRepo1.setQuarantineTime(null);
    component1InRepo2.setQuarantineTime(null);
    repositoryComponentDAO.update(component1InRepo1);
    repositoryComponentDAO.update(component2InRepo1);
    repositoryComponentDAO.update(component1InRepo2);

    // Verify initial state - refresh from DB to ensure changes are persisted
    component1InRepo1 = repositoryComponentDAO.getById(component1InRepo1.getId());
    component2InRepo1 = repositoryComponentDAO.getById(component2InRepo1.getId());
    component1InRepo2 = repositoryComponentDAO.getById(component1InRepo2.getId());

    assertThat(component1InRepo1.isQuarantined()).isFalse();
    assertThat(component2InRepo1.isQuarantined()).isFalse();
    assertThat(component1InRepo2.isQuarantined()).isFalse();

    // Mock evaluation results where component1InRepo1 becomes quarantined,
    // component2InRepo1 remains not quarantined, and component1InRepo2 becomes quarantined
    createHdsResponse(true, false, true);

    // Act
    task.run();

    // Assert - Verify quarantine status in progress records matches evaluation results
    List<ReevaluateCascadeProgress> allProgress = cascadeProgressDAO.getByRequestId(cascadeRequestId);
    assertThat(allProgress).hasSize(3);

    for (ReevaluateCascadeProgress progress : allProgress) {
      assertThat(progress.getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);

      if (progress.getRepositoryComponentId().equals(component1InRepo1.getId())) {
        // This component should now be quarantined based on evaluation result
        assertThat(progress.isQuarantined()).isTrue();
      }
      else if (progress.getRepositoryComponentId().equals(component2InRepo1.getId())) {
        // This component should remain not quarantined based on evaluation result
        assertThat(progress.isQuarantined()).isFalse();
      }
      else if (progress.getRepositoryComponentId().equals(component1InRepo2.getId())) {
        // This component should now be quarantined based on evaluation result
        assertThat(progress.isQuarantined()).isTrue();
      }
    }
  }

  @Test
  public void testCascadeReevaluationTask_QuarantineStatusUnchangedWhenNoViolations() {
    // Arrange - Set one component as initially quarantined
    component1InRepo1.setQuarantineTime(new Date());
    repositoryComponentDAO.update(component1InRepo1);

    // Mock evaluation results where all components are not quarantined (no violations)
    createHdsResponse(false, false, false);

    // Act
    task.run();

    // Assert - Verify quarantine status in progress records reflects evaluation results
    List<ReevaluateCascadeProgress> repo1Progress = cascadeProgressDAO.getByRepositoryId(repository1.getId());
    List<ReevaluateCascadeProgress> cascadeProgress = repo1Progress.stream()
        .filter(p -> cascadeRequestId.equals(p.getReevaluateCascadeRequestId()))
        .toList();

    assertThat(cascadeProgress).hasSize(2);

    for (ReevaluateCascadeProgress progress : cascadeProgress) {
      assertThat(progress.getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);
      // All components should now be not quarantined based on evaluation results
      assertThat(progress.isQuarantined()).isFalse();
    }
  }
}
