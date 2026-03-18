/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConditionFactReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintFactDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleApplicationEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleEvaluationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleRepositoryEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.PolicyEvaluationRequiredException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.*;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiStaleWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiStaleWaiverService apiStaleWaiverService;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private OwnerDAO ownerDAO;

  private Policy policy;

  private Organization org;

  private ConstraintFact constraintFact1;

  private ConstraintFact constraintFact2;

  private ComponentIdentifier componentIdentifier;

  @Before
  public void setupData() {
    org = tempEntity.newOrganization();
    policy = tempEntity.newPolicy(org.getId());
    componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    constraintFact1 = new ConstraintFact("constraintFact1", "aa c", "OR");
    constraintFact1.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    constraintFact2 = new ConstraintFact("constraintFact2", "aa c", "OR");
    constraintFact2.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
  }

  @Test
  public void testGetStaleWaivers_Repository() {
    Date date = new Date();
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    List<ConstraintFact> constraintFacts2 = Collections.singletonList(constraintFact2);
    Repository repo = tempEntity.newRepository("repo");

    // active waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("hash1", policy.getId(), repo.getId(),
        constraintFacts1, "Some comments here1");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("hash2", policy.getId(), repo.getId(),
        constraintFacts2, "Some comments here2");
    PolicyWaiver policyWaiver3 = tempEntity.newWaiver("hash3", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        constraintFacts1, "Some comments here3");

    // waived policy violations
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver1.getId(), policyWaiver1.getComment(), date);
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 7, "pathName2", "hash2", constraintFacts2, true,
        "actionId2", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver2.getId(), policyWaiver2.getComment(), date);
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 7, "pathName3", "hash3", constraintFacts2, true,
        "actionId2", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver3.getId(), policyWaiver3.getComment(), date);

    // stale waivers
    PolicyWaiver policyWaiver4 = tempEntity.newWaiverWithReason("hash4", policy.getId(), repo.getId(),
        constraintFacts2, "stale waiver comment1", "system", "Some reason");
    PolicyWaiver policyWaiver5 = tempEntity.newWaiver("hash5", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        constraintFacts1, "stale waiver comment2");
    PolicyWaiver policyWaiver6 = tempEntity.newWaiver("hash6", policy.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, constraintFacts1, "stale waiver comment3");
    PolicyWaiver policyWaiver7 = tempEntity.newWaiver("hash7", policy.getId(), repo.getId(),
        null, "stale waiver comment4");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(4);
    staleRepositoryWaivers.sort(Comparator.comparing(o -> o.comment));

    // stale waiver at repo scope
    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    assertStalePolicyWaiver(staleWaiver, policyWaiver4, policy, repo.getPublicId(), OwnerType.REPOSITORY.toString(),
        true, constraintFact2, "Some reason");

    // stale waiver at root organization scope
    staleWaiver = staleRepositoryWaivers.get(1);
    assertStalePolicyWaiver(staleWaiver, policyWaiver5, policy, "Root Organization",
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_ROOT_ORGANIZATION, true, constraintFact1);

    // stale waiver at repo container scope
    staleWaiver = staleRepositoryWaivers.get(2);
    assertStalePolicyWaiver(staleWaiver, policyWaiver6, policy, "Repository Managers",
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_REPOSITORY_CONTAINER, true, constraintFact1);

    // stale waiver with null constraint facts
    staleWaiver = staleRepositoryWaivers.get(3);
    assertStalePolicyWaiver(staleWaiver, policyWaiver7, policy, repo.getPublicId(), OwnerType.REPOSITORY.toString(),
        false, null);
  }

  @Test
  public void testGetStaleWaivers_RepositoryWithDeletedPolicyViolation() {
    Date date = new Date();
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo.getId(),
        constraintFacts1, "Some comments here1");

    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver.getId(), policyWaiver.getComment(), date);
    repositoryPolicyViolationDAO.delete(repositoryPolicyViolation);

    // should return the waiver as stale since the policy violation it waived is now deleted
    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);

    // stale waiver at repo scope
    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    assertStalePolicyWaiver(staleWaiver, policyWaiver, policy, repo.getPublicId(), OwnerType.REPOSITORY.toString(),
        true, constraintFact1);
  }

  @Test
  public void testGetStaleWaivers_RepositoryWithDeletedWaiverAndActivePolicyViolation() {
    Date date = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo.getId(),
        constraintFacts1, "Some comments here1");
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver.getId(), policyWaiver.getComment(), date);

    policyWaiverDAO.delete(policyWaiver);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(0);
  }

  private void assertStalePolicyWaiver(
      ApiStaleWaiverDTO staleWaiver,
      PolicyWaiver policyWaiver,
      Policy policy,
      String ownerName,
      String ownerType,
      boolean hasConstraintFacts,
      ConstraintFact constraintFact)
  {
    assertStalePolicyWaiver(staleWaiver, policyWaiver, policy, ownerName, ownerType, hasConstraintFacts,
        constraintFact, null);
  }

  private void assertStalePolicyWaiver(
      ApiStaleWaiverDTO staleWaiver,
      PolicyWaiver policyWaiver,
      Policy policy,
      String ownerName,
      String ownerType,
      boolean hasConstraintFacts,
      ConstraintFact constraintFact,
      String resonText)
  {
    assertThat(staleWaiver).isNotNull();
    assertThat(staleWaiver.waiverId).isEqualTo(policyWaiver.getId());
    assertThat(staleWaiver.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(staleWaiver.policyName).isEqualTo(policy.getName());
    assertThat(staleWaiver.comment).isEqualTo(policyWaiver.getComment());
    assertThat(staleWaiver.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(staleWaiver.scopeOwnerId).isEqualTo(policyWaiver.getOwnerId());
    assertThat(staleWaiver.scopeOwnerName).isEqualTo(ownerName);
    assertThat(staleWaiver.scopeOwnerType).isEqualTo(ownerType);
    assertThat(staleWaiver.creatorId).isEqualTo("testuser");
    assertThat(staleWaiver.creatorName).isEqualTo("Test User");
    if (resonText != null) {
      assertThat(staleWaiver.policyWaiverReasonId).isNotNull();
      assertThat(staleWaiver.reasonText).isEqualTo(resonText);
    }
    else {
      assertThat(staleWaiver.policyWaiverReasonId).isNull();
      assertThat(staleWaiver.reasonText).isNull();
    }

    if (hasConstraintFacts) {
      assertThat(staleWaiver.constraintFacts).hasSize(1);
      assertThat(staleWaiver.constraintFacts.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
      assertThat(staleWaiver.constraintFacts.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
      List<ApiConditionFactReasonDTO> reasons = staleWaiver.constraintFacts.get(0).reasons;
      assertThat(reasons).hasSize(1);
      assertThat(reasons.get(0).reason).isEqualTo(constraintFact.getConditionFacts().get(0).getReason());
    }
    else {
      assertThat(staleWaiver.constraintFacts).isEmpty();
    }
  }

  @Test
  public void testGetStaleWaivers_ApplicationWaivers() {
    Policy policy2 = tempEntity.newPolicy(org);
    Application app1 = tempEntity.newApplication("app1", org.getId());
    Application app2 = tempEntity.newApplication("app2", org.getId());
    Application app3InWaiverScope = tempEntity.newApplication("app3", org.getId());
    Owner waiverOwnerApp3 = app3InWaiverScope;

    // unapplied waiver
    PolicyWaiver unappliedAppWaiver = addUnappliedWaiver(policy2, waiverOwnerApp3.getId());

    // apply 2 other waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy.getId(), app1.getId(), "applied waiver1");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", policy.getId(), app2.getId(), "applied waiver2");
    evaluateAndApplyWaiver(app1, BuildStageType.ID, policyWaiver1);
    evaluateAndApplyWaiver(app2, OperateStageType.ID, policyWaiver2);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy2, unappliedAppWaiver, "application", waiverOwnerApp3.getId(),
        waiverOwnerApp3.getName());
    assertConstraintFacts(staleWaiver.constraintFacts);
    assertThat(staleWaiver.staleEvaluations).isNull();

    // now apply the stale waiver
    evaluateAndApplyWaiver(app3InWaiverScope, BuildStageType.ID, unappliedAppWaiver);
    staleWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithOrganizationWaivers() {
    Application app1 = tempEntity.newApplication("app1", org.getId());
    Organization org2 = tempEntity.newOrganization();
    Owner waiverOwnerOrg2 = org2;
    Application app2InWaiverScope = tempEntity.newApplication("app2", waiverOwnerOrg2.getId());
    Application app3InWaiverScope = tempEntity.newApplication("app3", waiverOwnerOrg2.getId());

    // unapplied waiver
    PolicyWaiver unappliedOrgWaiver = addUnappliedWaiver(policy, waiverOwnerOrg2.getId());

    // apply 2 other waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy.getId(), org.getId(), "applied waiver1");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", policy.getId(), org2.getId(), "applied waiver2");
    evaluateAndApplyWaiver(app1, BuildStageType.ID, policyWaiver1);
    evaluateAndApplyWaiver(app2InWaiverScope, OperateStageType.ID, policyWaiver2);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedOrgWaiver, "organization", waiverOwnerOrg2.getId(),
        waiverOwnerOrg2.getName());
    assertConstraintFacts(staleWaiver.constraintFacts);
    assertThat(staleWaiver.staleEvaluations).isNull();

    // now apply the stale waiver
    evaluateAndApplyWaiver(app3InWaiverScope, BuildStageType.ID, unappliedOrgWaiver);
    staleWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithRootOrganizationWaivers() {
    Owner waiverOwnerRootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Application app1InWaiverScope = tempEntity.newApplication("app1", org.getId());
    Application app2InWaiverScope = tempEntity.newApplication("app2", org.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app3InWaiverScope = tempEntity.newApplication("app3", org2.getId());

    // unapplied waiver
    PolicyWaiver unappliedRootOrgWaiver = addUnappliedWaiver(policy, waiverOwnerRootOrg.getId());

    // apply 2 other waivers
    PolicyWaiver policyWaiver1 =
        tempEntity.newWaiver("h1", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "applied waiver1");
    PolicyWaiver policyWaiver2 =
        tempEntity.newWaiver("h2", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "applied waiver2");
    Date nonStaleEvalDate = new Date(unappliedRootOrgWaiver.getCreateTime().getTime() + 1);
    evaluateAndApplyWaiver(app1InWaiverScope, BuildStageType.ID, policyWaiver1, nonStaleEvalDate);
    evaluateAndApplyWaiver(app2InWaiverScope, OperateStageType.ID, policyWaiver2, nonStaleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedRootOrgWaiver, "root_organization", waiverOwnerRootOrg.getId(),
        waiverOwnerRootOrg.getName());
    assertConstraintFacts(staleWaiver.constraintFacts);
    assertThat(staleWaiver.staleEvaluations).isNull();

    // now apply the stale waiver
    evaluateAndApplyWaiver(app3InWaiverScope, BuildStageType.ID, unappliedRootOrgWaiver);
    staleWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_ApplicationWaiversAndStaleEvaluations() {
    Application app1 = tempEntity.newApplication("app1", org.getId());
    Application app2 = tempEntity.newApplication("app2", org.getId());
    Application app3InWaiverScope = tempEntity.newApplication("app3", org.getId());
    Owner waiverOwnerApp3 = app3InWaiverScope;

    // unapplied waiver
    PolicyWaiver unappliedAppWaiver = addUnappliedWaiver(policy, waiverOwnerApp3.getId());
    Date staleEvalDate = new Date(0);
    Date nonStaleEvalDate = unappliedAppWaiver.getCreateTime();

    // stale evaluations out of scope - not at app3 waiver scope
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id (build)", staleEvalDate);
    tempEntity.newPolicyEvaluation(app2.getId(), StageReleaseStageType.ID, "test scan app2 id (stage)", staleEvalDate);

    // stale evaluations for app3 waiver
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), ReleaseStageType.ID, "test scan app3 id (release)",
        staleEvalDate);
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), OperateStageType.ID, "test scan app3 id (operate)",
        staleEvalDate);

    // non-stale evaluation
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), DevelopStageType.ID, "test scan app3 id (develop)",
        nonStaleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations).isNotNull();
    assertThat(staleWaiver.staleEvaluations.applications).hasSize(1);
    ApiStaleApplicationEvaluationDTO staleAppEval = staleWaiver.staleEvaluations.applications.get(0);
    assertApplication(staleAppEval.application, app3InWaiverScope);
    assertThat(staleAppEval.stages).hasSize(2);
    assertStage(staleAppEval.stages.get(0), ReleaseStageType.ID, staleEvalDate);
    assertStage(staleAppEval.stages.get(1), OperateStageType.ID, staleEvalDate);
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithOrganizationWaiversAndStaleEvaluations() {
    Application app1 = tempEntity.newApplication("app1", org.getId());
    Organization org2 = tempEntity.newOrganization();
    Owner waiverOwnerOrg2 = org2;
    Application app2InWaiverScope = tempEntity.newApplication("app2", waiverOwnerOrg2.getId());
    Application app3InWaiverScope = tempEntity.newApplication("app3", waiverOwnerOrg2.getId());

    // unapplied waiver
    PolicyWaiver unappliedOrgWaiver = addUnappliedWaiver(policy, waiverOwnerOrg2.getId());
    Date staleEvalDate = new Date(0);
    Date nonStaleEvalDate = unappliedOrgWaiver.getCreateTime();

    // stale evaluation out of scope under org1
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id (build)", staleEvalDate);

    // stale evaluations for org2 waiver
    tempEntity.newPolicyEvaluation(app2InWaiverScope.getId(), ReleaseStageType.ID, "test scan app2 id (release)",
        staleEvalDate);
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), OperateStageType.ID, "test scan app3 id (operate)",
        staleEvalDate);

    // non-stale evaluation
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), DevelopStageType.ID, "test scan app3 id (develop)",
        nonStaleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations).isNotNull();
    assertThat(staleWaiver.staleEvaluations.applications).hasSize(2);
    staleWaiver.staleEvaluations.applications.sort(Comparator.comparing(item -> item.application.publicId));

    ApiStaleApplicationEvaluationDTO staleAppEval = staleWaiver.staleEvaluations.applications.get(0);
    assertApplication(staleAppEval.application, app2InWaiverScope);
    assertThat(staleAppEval.stages).hasSize(1);
    assertStage(staleAppEval.stages.get(0), ReleaseStageType.ID, staleEvalDate);

    staleAppEval = staleWaiver.staleEvaluations.applications.get(1);
    assertApplication(staleAppEval.application, app3InWaiverScope);
    assertThat(staleAppEval.stages).hasSize(1);
    assertStage(staleAppEval.stages.get(0), OperateStageType.ID, staleEvalDate);
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithRootOrganizationWaiversAndStaleEvaluations() {
    Owner waiverOwnerRootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Application app1InWaiverScope = tempEntity.newApplication("app1", org.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app2InWaiverScope = tempEntity.newApplication("app2", org2.getId());
    Application app3InWaiverScope = tempEntity.newApplication("app3", org2.getId());

    // unapplied waiver
    PolicyWaiver unappliedRootOrgWaiver = addUnappliedWaiver(policy, waiverOwnerRootOrg.getId());
    Date staleEvalDate = new Date(0);
    Date nonStaleEvalDate = new Date(unappliedRootOrgWaiver.getCreateTime().getTime() + 1);

    // stale evaluations for root org waiver
    tempEntity
        .newPolicyEvaluation(app1InWaiverScope.getId(), BuildStageType.ID, "test scan app1 id (build)", staleEvalDate);
    tempEntity.newPolicyEvaluation(app2InWaiverScope.getId(), OperateStageType.ID, "test scan app2 id (operate)",
        staleEvalDate);

    // non-stale evaluation
    tempEntity.newPolicyEvaluation(app3InWaiverScope.getId(), DevelopStageType.ID, "test scan app3 id (develop)",
        nonStaleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations).isNotNull();
    assertThat(staleWaiver.staleEvaluations.applications).hasSize(2);
    staleWaiver.staleEvaluations.applications.sort(Comparator.comparing(item -> item.application.publicId));

    ApiStaleApplicationEvaluationDTO staleAppEval = staleWaiver.staleEvaluations.applications.get(0);
    assertApplication(staleAppEval.application, app1InWaiverScope);
    assertThat(staleAppEval.stages).hasSize(1);
    assertStage(staleAppEval.stages.get(0), BuildStageType.ID, staleEvalDate);

    staleAppEval = staleWaiver.staleEvaluations.applications.get(1);
    assertApplication(staleAppEval.application, app2InWaiverScope);
    assertThat(staleAppEval.stages).hasSize(1);
    assertStage(staleAppEval.stages.get(0), OperateStageType.ID, staleEvalDate);
  }

  private PolicyWaiver addUnappliedWaiver(final Policy policy, final String ownerId) {
    ConstraintFact waiverConstraintFact = new ConstraintFact("constraintFact1", "aa c", "OR");
    waiverConstraintFact
        .addConditionFact(new ConditionFact("MatchState", 0, "Match State is exact", "Match State was exact"));
    return tempEntity
        .newWaiver("h4", policy.getId(), ownerId, Collections.singletonList(waiverConstraintFact), "unapplied waiver");
  }

  private void evaluateAndApplyWaiver(final Application app, final String stage, final PolicyWaiver policyWaiver) {
    evaluateAndApplyWaiver(app, stage, policyWaiver, null);
  }

  private void evaluateAndApplyWaiver(
      final Application app,
      final String stage,
      final PolicyWaiver policyWaiver,
      final Date evalDate)
  {
    String appPublicId = app.getPublicId();
    PolicyEvaluation appPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), stage, "test scan " + appPublicId + " id (" + stage + ")",
            evalDate);

    tempEntity.newWaivedPolicyViolation(appPolicyEvaluation, policy,
        ComponentIdentifier.createMavenCoordinates("g-" + appPublicId, "a-" + appPublicId, "v-" + appPublicId),
        "h-" + appPublicId, policyWaiver);
  }

  private void assertStaleWaiver(
      final ApiStaleWaiverDTO staleWaiverDTO,
      final Policy expectedPolicy,
      final PolicyWaiver expectedWaiver,
      final String expectedOwnerType,
      final String expectedOwnerId,
      final String expectedOwnerName)
  {
    assertStaleWaiver(staleWaiverDTO, expectedPolicy, expectedWaiver, expectedOwnerType, expectedOwnerId,
        expectedOwnerName, null);
  }

  private void assertStaleWaiver(
      final ApiStaleWaiverDTO staleWaiverDTO,
      final Policy expectedPolicy,
      final PolicyWaiver expectedWaiver,
      final String expectedOwnerType,
      final String expectedOwnerId,
      final String expectedOwnerName,
      final String reasonText)
  {
    assertThat(staleWaiverDTO).isNotNull();
    assertThat(staleWaiverDTO.waiverId).isEqualTo(expectedWaiver.getId());
    assertThat(staleWaiverDTO.policyId).isEqualTo(expectedPolicy.getId());
    assertThat(staleWaiverDTO.policyName).isEqualTo(expectedPolicy.getName());
    assertThat(staleWaiverDTO.comment).isEqualTo(expectedWaiver.getComment());
    assertThat(staleWaiverDTO.createTime).isEqualTo(expectedWaiver.getCreateTime());
    assertThat(staleWaiverDTO.expiryTime).isEqualTo(expectedWaiver.getExpiryTime());
    assertThat(staleWaiverDTO.scopeOwnerType).isEqualTo(expectedOwnerType);
    assertThat(staleWaiverDTO.scopeOwnerId).isEqualTo(expectedOwnerId);
    assertThat(staleWaiverDTO.scopeOwnerName).isEqualTo(expectedOwnerName);
    assertThat(staleWaiverDTO.creatorId).isNotNull();
    assertThat(staleWaiverDTO.creatorId).isEqualTo(expectedWaiver.getCreatorId());
    assertThat(staleWaiverDTO.creatorName).isEqualTo(expectedWaiver.getCreatorName());
    if (reasonText != null) {
      assertThat(staleWaiverDTO.policyWaiverReasonId).isNotNull();
      assertThat(staleWaiverDTO.reasonText).isEqualTo(reasonText);
    }
    else {
      assertThat(staleWaiverDTO.policyWaiverReasonId).isNull();
      assertThat(staleWaiverDTO.reasonText).isNull();
    }
  }

  private void assertConstraintFacts(List<ApiConstraintFactDTO> constraintFacts) {
    assertThat(constraintFacts).hasSize(1);
    ApiConstraintFactDTO constraintFactDTO = constraintFacts.get(0);
    assertThat(constraintFactDTO.constraintId).isNotNull();
    assertThat(constraintFactDTO.constraintName).isNotNull();
    assertThat(constraintFactDTO.reasons).hasSize(1);
    assertThat(constraintFactDTO.reasons.get(0).reason).isEqualTo("Match State was exact");
  }

  private void assertApplication(final ApiApplicationBaseDTO applicationDTO, final Application expectedApplication) {
    assertThat(applicationDTO.publicId).isEqualTo(expectedApplication.getPublicId());
    assertThat(applicationDTO.id).isEqualTo(expectedApplication.getId());
    assertThat(applicationDTO.name).isEqualTo(expectedApplication.getName());
    assertThat(applicationDTO.organizationId).isEqualTo(expectedApplication.getOrganizationId());
    assertThat(applicationDTO.contactUserName).isEqualTo(expectedApplication.getContactInternalName());
  }

  private void assertStage(
      final ApiStaleEvaluationStageDTO stageDTO,
      final String expectedStageId,
      final Date expectedStaleEvalDate)
  {
    assertThat(stageDTO.stageId).isEqualTo(expectedStageId);
    assertThat(stageDTO.lastEvaluationDate).isEqualTo(expectedStaleEvalDate);
  }

  @Test
  public void testGetStaleWaivers_LegacyApplicationWaiverWithoutConstraintFacts() {
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)");

    // unapplied waiver
    PolicyWaiver unappliedWaiverWithoutConstraintFacts =
        tempEntity.newWaiverWithReason("h4", policy.getId(), app.getId(), null, "Some comments here",
            "system", "Some reason");

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedWaiverWithoutConstraintFacts, "application", app.getId(),
        app.getName(), "Some reason");
    assertThat(staleWaiver.constraintFacts).isEmpty();

    // now apply the stale waiver
    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1",
        unappliedWaiverWithoutConstraintFacts);

    staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_RepositoryAndApplicationWaivers() {
    Date date = new Date();
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    Application app = tempEntity.newApplication(org.getId());
    Repository repo = tempEntity.newRepository("repo");

    // unapplied waiver
    PolicyWaiver unappliedRootOrgWaiver = addUnappliedWaiver(policy, Organization.ROOT_ORGANIZATION_ID);

    // apply app and repo waiver
    PolicyWaiver appWaiver =
        tempEntity.newWaiver("h1", policy.getId(), org.getId(), "app waiver");
    PolicyWaiver repoWaiver =
        tempEntity.newWaiver("h2", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, "repo waiver");

    PolicyEvaluation app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)");
    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", appWaiver);

    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        repoWaiver.getId(), repoWaiver.getComment(), date);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedRootOrgWaiver, "root_organization",
        Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertConstraintFacts(staleWaiver.constraintFacts);
  }

  @Test
  public void testGetStaleWaivers_LegacyRepositoryWaiver() {
    Date date = new Date();
    List<ConstraintFact> constraintFacts1 = Collections.singletonList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");
    // legacy waived repo violation does not have these pieces of information
    String legacyWaiverId = null;
    String legacyWaiverComment = null;
    Date legacyWaiverDate = null;

    tempEntity.newWaiver("h2", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, "repo waiver");

    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        legacyWaiverId, legacyWaiverComment, legacyWaiverDate);

    assertThatExceptionOfType(PolicyEvaluationRequiredException.class)
        .isThrownBy(() -> apiStaleWaiverService.getStaleWaivers())
        .withMessage("All repositories must be re-evaluated to capture current waiver information.");
  }

  @Test
  public void testGetStaleWaivers_StaleRepositoryEvaluationWithRepositoryScopedWaivers() {
    Date now = new Date();
    Date componentCreateTime = new Date(now.getTime() - 10);
    Date staleEvaluationTime = new Date(now.getTime() - 5);
    Date evaluationTime = new Date(now.getTime() + 1);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    // stale waivers
    tempEntity.newWaiver("hash", policy.getId(), repo1.getId(), constraintFacts, "waiver comment1", now);
    tempEntity.newWaiver("hash", policy.getId(), repo2.getId(), constraintFacts, "waiver comment2", now);

    // repo1 stale evaluation
    tempEntity.newRepositoryComponent(repo1.getId(), "path", componentCreateTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), staleEvaluationTime);

    // repo2 non-stale evaluation
    tempEntity.newRepositoryComponent(repo2.getId(), "path", componentCreateTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), evaluationTime);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(2);
    staleRepositoryWaivers.sort(Comparator.comparing(o -> o.comment));

    // assert that repo1 waiver has a stale evaluation
    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations.repositories).hasSize(1);
    ApiStaleRepositoryEvaluationDTO staleRepositoryEvaluation = staleWaiver.staleEvaluations.repositories.get(0);
    assertApiRepositoryDTO(staleRepositoryEvaluation.repository, repo1);
    assertThat(staleRepositoryEvaluation.stages).hasSize(1);
    assertApiStaleEvaluationStageDTO(staleRepositoryEvaluation.stages.get(0), staleEvaluationTime);

    // assert that repo2 waiver has no stale evaluations
    assertThat(staleRepositoryWaivers.get(1).staleEvaluations).isNull();
  }

  @Test
  public void testGetStaleWaivers_StaleRepositoryEvaluationsWithRootOrganizationScopedWaiver() {
    Date now = new Date();
    Date evaluationTime = new Date(now.getTime() - 5);
    Date componentCreateTime = new Date(now.getTime() - 10);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    Repository repo3 = tempEntity.newRepository("repo3");

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    // stale waiver
    tempEntity.newWaiver("hash", policy.getId(), Organization.ROOT_ORGANIZATION_ID, constraintFacts,
        "stale waiver comment", now);

    // repo1 & repo2 have stale evaluations
    tempEntity.newRepositoryComponent(repo1.getId(), "path", componentCreateTime,
        "hash", componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), evaluationTime);
    tempEntity.newRepositoryComponent(repo2.getId(), "path", componentCreateTime,
        "hash", componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), now);

    // repo3 has non-stale evaluation
    tempEntity.newRepositoryComponent(repo3.getId(), "path", componentCreateTime,
        "hash", componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(),
        new Date(now.getTime() + 1));

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);

    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    staleWaiver.staleEvaluations.repositories.sort(Comparator.comparing(o -> o.repository.publicId));

    // assert that both repo1 and repo2 evaluations are returned as stale
    assertThat(staleWaiver.staleEvaluations.repositories).hasSize(2);
    ApiStaleRepositoryEvaluationDTO staleRepositoryEvaluation1 = staleWaiver.staleEvaluations.repositories.get(0);
    assertApiRepositoryDTO(staleRepositoryEvaluation1.repository, repo1);
    assertThat(staleRepositoryEvaluation1.stages).hasSize(1);
    assertApiStaleEvaluationStageDTO(staleRepositoryEvaluation1.stages.get(0), evaluationTime);

    ApiStaleRepositoryEvaluationDTO staleRepositoryEvaluation2 = staleWaiver.staleEvaluations.repositories.get(1);
    assertApiRepositoryDTO(staleRepositoryEvaluation2.repository, repo2);
    assertThat(staleRepositoryEvaluation2.stages).hasSize(1);
    assertApiStaleEvaluationStageDTO(staleRepositoryEvaluation2.stages.get(0), now);
  }

  @Test
  public void testGetStaleWaivers_StaleRepositoryEvaluationWithAllReposScopedWaiver() {
    Date now = new Date();
    Date staleEvaluationTime = new Date(now.getTime() - 5);
    Date evaluationTime = new Date(now.getTime() + 1);
    Date componentCreateTime = new Date(now.getTime() - 10);

    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    // stale waiver
    tempEntity.newWaiver("hash", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, constraintFacts,
        "stale waiver comment", now);

    // repo1 stale evaluation
    tempEntity.newRepositoryComponent(repo1.getId(), "path", componentCreateTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), staleEvaluationTime);

    // repo2 non-stale evaluation
    tempEntity.newRepositoryComponent(repo2.getId(), "path", componentCreateTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), evaluationTime);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);

    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);

    // assert that repo1 evaluation shows up as stale
    assertThat(staleWaiver.staleEvaluations.repositories).hasSize(1);
    ApiStaleRepositoryEvaluationDTO staleRepositoryEvaluation = staleWaiver.staleEvaluations.repositories.get(0);
    assertApiRepositoryDTO(staleRepositoryEvaluation.repository, repo1);
    assertThat(staleRepositoryEvaluation.stages).hasSize(1);
    assertApiStaleEvaluationStageDTO(staleRepositoryEvaluation.stages.get(0), staleEvaluationTime);
  }

  @Test
  public void testGetStaleWaivers_NoStaleRepositoryEvaluations() {
    Date now = new Date();
    Date waiverCreateTime = new Date(now.getTime() - 2);
    Date evaluationTime = new Date();
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    // stale waiver
    tempEntity.newWaiver("hash", policy.getId(), repo.getId(), constraintFacts, "stale waiver comment",
        waiverCreateTime);

    tempEntity.newRepositoryComponent(repo.getId(), "path", evaluationTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), evaluationTime);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);
    assertThat(staleRepositoryWaivers.get(0).staleEvaluations).isNull();
  }

  @Test
  public void testGetStaleWaivers_NoStaleRepositoryEvaluationsWhenRepositoryAlwaysEmpty() {
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository emptyRepo = tempEntity.newRepository("repo");

    // stale waiver
    tempEntity.newWaiver("hash", policy.getId(), emptyRepo.getId(), constraintFacts, "stale waiver comment",
        new Date());

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);
    assertThat(staleRepositoryWaivers.get(0).staleEvaluations).isNull();
  }

  @Test
  public void testGetStaleWaivers_NoStaleRepositoryEvaluationsWhenRepositoryNowEmpty() {
    Date now = new Date();
    Date staleEvaluationTime = new Date(now.getTime() - 5);
    Date componentCreateTime = new Date(now.getTime() - 10);
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");

    // stale waiver
    tempEntity.newWaiver("hash", policy.getId(), repo.getId(), constraintFacts, "stale waiver comment",
        now);

    // stale evaluation
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repo.getId(), "path", componentCreateTime, "hash", componentIdentifier,
            MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), staleEvaluationTime);

    // remove component from repo
    repositoryComponentDAO.delete(repositoryComponent);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(1);
    assertThat(staleRepositoryWaivers.get(0).staleEvaluations).isNull();
  }

  @Test
  public void testGetLatestFoundIndex() {
    long before = 1L;
    long match = 2L;
    long after = 3L;

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();
    policyEvaluations.add(createPolicyEvaluationWithDate(new Date(before)));

    policyEvaluations.add(createPolicyEvaluationWithDate(new Date(match)));
    policyEvaluations.add(createPolicyEvaluationWithDate(new Date(match)));
    policyEvaluations.add(createPolicyEvaluationWithDate(new Date(match)));

    int latestFoundIndex = apiStaleWaiverService.getLatestFoundIndex(1, policyEvaluations);
    assertThat(latestFoundIndex).isEqualTo(3);

    // now add the after
    policyEvaluations.add(createPolicyEvaluationWithDate(new Date(after)));
    latestFoundIndex = apiStaleWaiverService.getLatestFoundIndex(1, policyEvaluations);
    assertThat(latestFoundIndex).isEqualTo(3);

    // test just one item
    policyEvaluations.remove(1);
    policyEvaluations.remove(1);
    latestFoundIndex = apiStaleWaiverService.getLatestFoundIndex(1, policyEvaluations);
    assertThat(latestFoundIndex).isEqualTo(1);
  }

  @Test
  public void testGetStaleWaivers_applicationWithExpiringAndExpiredWaivers() {
    Policy expiredWaiverPolicy = tempEntity.newPolicy(org);
    Application app1 = tempEntity.newApplication("app1", org.getId());
    Owner waiversOwner = app1;

    Date staleEvaluationDate = new Date();
    tempEntity.newPolicyEvaluation(waiversOwner.getId(), OperateStageType.ID, "test scan app1 id (operate)",
        staleEvaluationDate);

    // stale waiver
    List<ConstraintFact> constraintFacts = Collections.singletonList(constraintFact1);

    Date expiringDate = Date.from(ZonedDateTime.now().plus(Period.ofMonths(2)).toInstant());
    PolicyWaiver staleWaiver =
        tempEntity.newWaiver("hash", policy.getId(), app1.getId(), constraintFacts, "stale waiver comment",
            staleEvaluationDate, expiringDate);
    addExpiredWaiver(expiredWaiverPolicy, waiversOwner);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO returnedWaiver = staleWaivers.get(0);
    assertStaleWaiver(returnedWaiver, policy, staleWaiver, "application", waiversOwner.getId(),
        waiversOwner.getName());
    assertConstraintFacts(returnedWaiver.constraintFacts);
  }

  private PolicyEvaluation createPolicyEvaluationWithDate(Date date) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setTime(date);
    return policyEvaluation;
  }

  private PolicyWaiver addExpiredWaiver(final Policy expiredWaiverPolicy, final Owner expiredWaiverOwner) {
    ConstraintFact waiverConstraintFact = new ConstraintFact("constraintFact1", "aa c", "OR");
    waiverConstraintFact
        .addConditionFact(new ConditionFact("MatchState", 0, "Match State is exact", "Match State was exact"));

    Date createTime = Date.from(ZonedDateTime.now().minus(Period.ofMonths(2)).toInstant());
    Date expiryTime = Date.from(ZonedDateTime.now().minus(Period.ofMonths(1)).toInstant());
    return tempEntity
        .newWaiver("h4", expiredWaiverPolicy.getId(), expiredWaiverOwner.getId(),
            Collections.singletonList(waiverConstraintFact),
            "unapplied waiver", createTime, expiryTime);
  }

  private void assertApiRepositoryDTO(
      ApiRepositoryDTO repositoryDTO,
      Repository repository)
  {
    assertThat(repositoryDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(repositoryDTO.publicId).isEqualTo(repository.getPublicId());
    assertThat(repositoryDTO.format).isEqualTo(repository.getFormat());
  }

  private void assertApiStaleEvaluationStageDTO(ApiStaleEvaluationStageDTO staleRepoStage, Date evaluationDate) {
    assertThat(staleRepoStage.stageId).isEqualTo("proxy");
    assertThat(staleRepoStage.lastEvaluationDate).isEqualTo(evaluationDate);
  }
}
