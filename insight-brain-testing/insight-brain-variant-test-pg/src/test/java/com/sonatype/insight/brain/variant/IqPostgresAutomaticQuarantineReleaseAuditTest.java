/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineRelease;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL variant of the legacy
 * {@code com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseAuditTest}.
 */
@IqPostgresTest
class IqPostgresAutomaticQuarantineReleaseAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private AutomaticQuarantineRelease automaticQuarantineRelease;

  private Repository repository;

  private ProxyRepositoryComponent component;

  @BeforeEach
  void setup() {
    logOutput.before();
    logOutput.clear();

    automaticQuarantineRelease = ctx.lookup(AutomaticQuarantineRelease.class);
    repository = ctx.tempEntity().newRepository();
    component = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
            "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  @Test
  void testAutomaticQuarantineRelease() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), constraint);
    createPolicyViolationFail(policy, component);
    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));
    assertThat(component.isQuarantined()).isTrue();

    automaticQuarantineRelease.run();

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.RELEASE_QUARANTINE, 1);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    assertComponentUnquarantineData(auditDTOs.get(0), component.getHash(), component.getPathname());

    auditDTOs = awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 1);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    auditDTOs.sort(Comparator.comparing(dto -> (Integer) dto.data.get("componentCount")));
    assertRepositoryEvaluationData(auditDTOs.get(0), 1, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  void testAutomaticQuarantineRelease_RepositoryWithAutoUnquarantineDisabled() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), constraint);
    createPolicyViolationFail(policy, component);
    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    assertThat(component.isQuarantined()).isTrue();

    automaticQuarantineRelease.run();

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 0);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    auditDTOs.sort(Comparator.comparing(dto -> (Integer) dto.data.get("componentCount")));
    assertThat(auditDTOs).isEmpty();
  }

  @Test
  void testAutomaticQuarantineRelease_RepositoryWithNoComponents() {
    automaticQuarantineRelease.run();

    awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 0);
  }

  @Test
  void testAutomaticQuarantineRelease_WithNoRepositories() {
    automaticQuarantineRelease.run();

    awaitLogEntries(AuditEvent.EVALUATE_REPOSITORY, 0);
  }

  private void mockFirewallResponse(ComponentEvaluationDataList hdsResult) {
    ctx.hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private void assertComponentUnquarantineData(AuditDTO auditDTO, String componentHash, String componentPathname) {
    assertCustomData(auditDTO, "componentHash", componentHash);
    assertCustomData(auditDTO, "componentPathname", componentPathname);
  }

  private void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final ProxyRepositoryComponent component,
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

    return ctx.tempEntity().newPolicy(policy);
  }

  private ProxyRepositoryPolicyViolation createPolicyViolationFail(Policy policy, ProxyRepositoryComponent component) {
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation();
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
    return ctx.tempEntity().newRepositoryPolicyViolation(policyViolation);
  }

  private List<ConstraintFact> createConstraintFacts(Policy policy) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
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

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... n) {
      super(n);
    }

    void tearDown() {
      after();
    }
  }
}
