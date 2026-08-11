/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.HygieneRating;
import com.sonatype.clm.dto.model.component.IntegrityRating;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresAutomaticQuarantineReleaseTest
{

  private IqTestContext ctx;

  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private AutomaticQuarantineRelease automaticQuarantineRelease;

  @BeforeEach
  void setup() {
    proxyRepositoryPolicyViolationDAO = ctx.lookup(ProxyRepositoryPolicyViolationDAO.class);
    proxyRepositoryComponentDAO = ctx.lookup(ProxyRepositoryComponentDAO.class);
    automaticQuarantineRelease = ctx.lookup(AutomaticQuarantineRelease.class);
  }

  @Test
  void testAutomaticQuarantineRelease_Licensed() throws Exception {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), null, new Date());
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository, policy, component.getPathname(),
            component.getComponentIdentifier(), component.getHash());
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    Date firstCompEvalTime = component.getLastEvaluationTime();

    mockFirewallResponse(getFirewallHdsResponse(component, component.getHash(), new IntegrityRating(2, "Pending")));
    automaticQuarantineRelease.run();
    Date secondCompEvalTime = proxyRepositoryComponentDAO.getById(component.getId()).getLastEvaluationTime();
    assertThat(assertThat(secondCompEvalTime).isAfter(firstCompEvalTime));
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();

    mockFirewallResponse(getFirewallHdsResponse(component, component.getHash(), new IntegrityRating(4, "Laggard")));
    automaticQuarantineRelease.run();
    assertThat(assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getLastEvaluationTime()).isAfter(
        secondCompEvalTime));
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isFalse();
  }

  @Test
  void testAutomaticQuarantineRelease_WithComponentsQuarantinedBeyondMaxDays() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    Date quarantineTime = Date.from(
        LocalDateTime.now()
            .minusDays(AutomaticQuarantineRelease.MAX_REEVALUATION_DAYS_FOR_AUTO_RELEASED + 1)
            .atZone(ZoneId.systemDefault())
            .toInstant());
    ProxyRepositoryComponent component = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), new Date(), quarantineTime);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_NotSupportedAutoUnquarantineEnabledConditionType() {
    Condition condition = new Condition(HygieneRatingConditionType.ID, "is", "4");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new HygieneRating(1, "Exemplar")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_WithoutFirewallAutoUnquarantineFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(component.getRepositoryId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_WithoutReleaseIntegrityFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.RELEASE_INTEGRITY);

    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(component.getRepositoryId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineNotEnabled() {
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineDataNotChanged() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(2, "Pending")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineDataChanged() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isFalse();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineDataChangedWithOtherFailViolation() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    constraint.addCondition(condition);
    Condition condition2 = new Condition(HygieneRatingConditionType.ID, "is", "4");
    constraint.addCondition(condition2);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineDataChangedWithOtherNonFailViolation() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Constraint constraint1 = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition1 = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    constraint1.addCondition(condition1);
    Policy policy1 = createPolicy("policy1", new Stage(ProxyStageType.ID), FailActionType.ID, constraint1);

    Constraint constraint2 = new Constraint("c1", "constraint1", LogicalOperator.OR);
    Condition condition2 = new Condition(HygieneRatingConditionType.ID, "is", "4");
    constraint2.addCondition(condition2);
    Policy policy2 = createPolicy("policy2", new Stage(ProxyStageType.ID), WarnActionType.ID, constraint2);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy1, component, FailActionType.ID);
    createPolicyViolation(policy2, component, WarnActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isFalse();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_AutoUnquarantineDataChangedWithViolationNotQuarantined() {
    ctx.tempEntity().newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);

    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), false);
    assertThat(component.isQuarantined()).isFalse();

    createPolicyViolation(policy, component, FailActionType.ID);
    ctx.tempEntity().newPolicyMonitoring(repository.getId(), ProxyStageType.ID);

    // if the component gets re-evaluated, it be will quarantined due matching policy condition
    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(2, "Pending")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isFalse();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  @Test
  void testAutomaticQuarantineRelease_RootOrgAndNonProxyStage() {
    ctx.tempEntity().newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, ReleaseStageType.ID);

    Condition condition = new Condition(IntegrityRatingConditionType.ID, "is", "2");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy = createPolicy("policy", new Stage(ProxyStageType.ID), FailActionType.ID, constraint);
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryComponent component =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname1",
                "hash1", ComponentIdentifier.createMavenCoordinates("g", "a1", "v"), true);
    assertThat(component.isQuarantined()).isTrue();

    createPolicyViolation(policy, component, FailActionType.ID);

    mockFirewallResponse(getFirewallHdsResponse(component, "hash1", new IntegrityRating(0, "Normal")));

    automaticQuarantineRelease.run();

    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).isQuarantined()).isTrue();
    assertThat(proxyRepositoryComponentDAO.getById(component.getId()).getAutoUnquarantined()).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(1);
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final ProxyRepositoryComponent component,
      final String hash,
      final IntegrityRating integrityRating)
  {
    return getFirewallHdsResponse(component, hash, integrityRating, new HygieneRating(4, "Laggard"));
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final ProxyRepositoryComponent component,
      final String hash,
      final HygieneRating hygieneRating)
  {
    return getFirewallHdsResponse(component, hash, new IntegrityRating(1, "Suspicious"), hygieneRating);
  }

  private ComponentEvaluationDataList getFirewallHdsResponse(
      final ProxyRepositoryComponent component,
      final String hash,
      final IntegrityRating integrityRating,
      final HygieneRating hygieneRating)
  {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = component.getComponentIdentifier();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.securityVulnerabilities.add(
        new SecurityVulnerability("refid", "source", 10F));
    componentEvaluationData.integrityRating = integrityRating;
    componentEvaluationData.hygieneRating = hygieneRating;
    hdsResult.components.add(componentEvaluationData);
    return hdsResult;
  }

  private ProxyRepositoryPolicyViolation createPolicyViolation(
      Policy policy,
      ProxyRepositoryComponent component,
      String action)
  {
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
    policyViolation.setActionTypeId(action);
    return ctx.tempEntity().newRepositoryPolicyViolation(policyViolation);
  }

  private List<ConstraintFact> createConstraintFacts(Policy policy) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
            constraint.getOperator().toString());
        constraintFact.addConditionFact(
            new ConditionFact(condition.getConditionTypeId(), 0, "", "random for condition "
                + condition.getConditionTypeId()));
        constraintFacts.add(constraintFact);
      }
    }

    return constraintFacts;
  }

  private void mockFirewallResponse(ComponentEvaluationDataList hdsResult) {
    ctx.hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private Policy createPolicy(
      String policyName,
      Stage stage,
      String action,
      Constraint constraint)
  {
    Policy policy = new Policy(null /* id */, policyName);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setThreatLevel(8);
    policy.addConstraint(constraint);
    policy.setAction(stage.getStageTypeId(), action);

    return ctx.tempEntity().newPolicy(policy);
  }
}
