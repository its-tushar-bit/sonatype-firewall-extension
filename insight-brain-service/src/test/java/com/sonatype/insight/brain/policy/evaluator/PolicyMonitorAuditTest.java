/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.HygieneRating;
import com.sonatype.clm.dto.model.component.IntegrityRating;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.mock.hds.HdsMockServer.RestHandler;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyMonitorAuditTest
    extends AbstractAuditTest
{
  private PolicyMonitor policyMonitor;

  private Application app;

  private Stage stage;

  @Before
  public void setup() {
    policyMonitor = getCLMServer().getInstance(PolicyMonitor.class);
    app = tempEntity.newApplicationWithParent("MonitoredApp");
    stage = new Stage(ReleaseStageType.ID);
    tempEntity.newPolicyMonitoring(app.getId(), stage.getStageTypeId());
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring() {
    createScanFile(app.getId(), RestHandler.SCAN_ID);
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestHandler.SCAN_ID);

    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), null, app.getId(),
        app.getPublicId(), app.getName(), ReleaseStageType.ID, scanId2, false, SYSTEM_USER);
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring_WithNoLastPrimaryEvaluation() {
    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 0);
  }

  @Test
  public void testRunEvaluation_AppWithMonitoring_WhenNoScanFileFound() {
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestHandler.SCAN_ID);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "server-error", app.getId(),
        app.getPublicId(), app.getName(), null, null, null, SYSTEM_USER);
  }

  @Test
  public void testRunEvaluation_RepositoryWithMonitoring() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
        "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), constraint);

    createPolicyViolationFail(policy, component);
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    policyMonitor.run();

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 1);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    auditDTOs.sort(Comparator.comparing(dto -> (Integer) dto.data.get("componentCount")));
    assertRepositoryEvaluationData(auditDTOs.get(0), 1, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  public void testRunEvaluation_RepositoryWithMonitoring_WithNoComponents() {
    Repository repository = tempEntity.newRepository();
    tempEntity.newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 0);
  }

  @Test
  public void testRunEvaluation_RepositoryWithMonitoring_WithNoRepositories() {
    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 0);
  }

  private void mockScanReceiptAndReport(String scanId) {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(scanId, "/PolicyMonitorTest/report");
  }

  private Policy createPolicy(
      String policyName,
      Stage stage,
      Constraint constraint)
  {
    Policy policy = new Policy(null /* id */, policyName);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.setAction(stage.getStageTypeId(), FailActionType.ID);

    return tempEntity.newPolicy(policy);
  }

  private RepositoryPolicyViolation createPolicyViolationFail(Policy policy, RepositoryComponent component) {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();
    policyViolation.setRepositoryId(component.getRepositoryId());
    policyViolation.setPathname(component.getPathname());
    policyViolation.setTime(new Date());
    policyViolation.setHash(component.getHash());
    policyViolation.setComponentIdentifier(component.getComponentIdentifier());
    policyViolation.setPolicyId(policy.getId());
    policyViolation.setPolicyName(policy.getName());
    policyViolation.setThreatLevel(policy.getThreatLevel());
    policyViolation.setThreatCategory(policy.getThreatCategory());
    policyViolation.setConstraintFacts(createConstraintFacts(policy));
    policyViolation.setActionTypeId(Action.ID_FAIL);
    return tempEntity.newRepositoryPolicyViolation(policyViolation);
  }

  private List<ConstraintFact> createConstraintFacts(Policy policy) {
    List<ConstraintFact> constraintFacts = new ArrayList();
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
            constraint.getOperator().toString());
        constraintFact.addConditionFact(new ConditionFact(condition.getConditionTypeId(), 0, "", "random for condition "
            + condition.getConditionTypeId()));
        constraintFacts.add(constraintFact);
      }
    }

    return constraintFacts;
  }

  private void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final RepositoryComponent component,
      final String hash,
      final IntegrityRating integrityRating)
  {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = component.getComponentIdentifier();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.securityVulnerabilities.add(new SecurityVulnerability("refid", "source", 10F));
    componentEvaluationData.integrityRating = integrityRating;
    componentEvaluationData.hygieneRating = new HygieneRating(4, "Laggard");
    hdsResult.components.add(componentEvaluationData);
    return hdsResult;
  }

  private void mockFirewallResponse(ComponentEvaluationDataList hdsResult) {
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }
}
