/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiConditionFactReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintFactDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiStaleWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiStaleWaiverService apiStaleWaiverService;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

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
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);
    List<ConstraintFact> constraintFacts2 = Arrays.asList(constraintFact2);
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
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver1.getId(), policyWaiver1.getComment(), date);
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 7, "pathName2", "hash2", constraintFacts2, true, true,
        "actionId2", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver2.getId(), policyWaiver2.getComment(), date);
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 7, "pathName3", "hash3", constraintFacts2, true, true,
        "actionId2", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver3.getId(), policyWaiver3.getComment(), date);

    // stale waivers
    PolicyWaiver policyWaiver4 = tempEntity.newWaiver("hash4", policy.getId(), repo.getId(),
        constraintFacts2, "stale waiver comment1");
    PolicyWaiver policyWaiver5 = tempEntity.newWaiver("hash5", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        constraintFacts1, "stale waiver comment2");
    PolicyWaiver policyWaiver6 = tempEntity.newWaiver("hash6", policy.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, constraintFacts1,"stale waiver comment3");
    PolicyWaiver policyWaiver7 = tempEntity.newWaiver("hash7", policy.getId(), repo.getId(),
        null, "stale waiver comment4");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(4);
    staleRepositoryWaivers.sort(Comparator.comparing(o -> o.comment));

    // stale waiver at repo scope
    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    assertStalePolicyWaiver(staleWaiver, policyWaiver4, policy, repo.getPublicId(), OwnerType.REPOSITORY.toString(),
        true, constraintFact2);

    // stale waiver at root organization scope
    staleWaiver = staleRepositoryWaivers.get(1);
    assertStalePolicyWaiver(staleWaiver, policyWaiver5, policy, "Root Organization",
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_ROOT_ORGANIZATION.toString(), true, constraintFact1);

    // stale waiver at repo container scope
    staleWaiver = staleRepositoryWaivers.get(2);
    assertStalePolicyWaiver(staleWaiver, policyWaiver6, policy, "All Repositories",
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_REPOSITORY_CONTAINER.toString(), true, constraintFact1);

    // stale waiver with null constraint facts
    staleWaiver = staleRepositoryWaivers.get(3);
    assertStalePolicyWaiver(staleWaiver, policyWaiver7, policy, repo.getPublicId(), OwnerType.REPOSITORY.toString(),
        false, null);
  }

  @Test
  public void testGetStaleWaivers_RepositoryWithDeletedPolicyViolation() {
    Date date = new Date();
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo.getId(),
        constraintFacts1, "Some comments here1");

    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true, true,
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
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);
    Repository repo = tempEntity.newRepository("repo");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo.getId(),
        constraintFacts1, "Some comments here1");
    tempEntity.newRepositoryPolicyViolation(
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        policyWaiver.getId(), policyWaiver.getComment(), date);

    policyWaiverDAO.delete(policyWaiver);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).hasSize(0);
  }

  private void assertStalePolicyWaiver(ApiStaleWaiverDTO staleWaiver, 
        PolicyWaiver policyWaiver, 
        Policy policy,
        String ownerName,
        String ownerType,
        boolean hasConstraintFacts,
        ConstraintFact constraintFact)
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
    assertThat(staleWaiver.isObsolete).isTrue();

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
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(org.getId());

    // unapplied waiver
    PolicyWaiver unappliedAppWaiver = addUnappliedWaiver(policy2, app3.getId());

    // apply 3 other waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy.getId(), app1.getId(), "applied waiver1");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", policy.getId(), app2.getId(), "applied waiver2");
    PolicyWaiver policyWaiver3 = tempEntity.newWaiver("h3", policy.getId(), app3.getId(), "applied waiver3");
    evaluateAndApplyWaivers(app1, app2, policyWaiver1, policyWaiver2, policyWaiver3);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy2, unappliedAppWaiver, "application", app3.getId(), app3.getName());
    assertContraintFacts(staleWaiver.constraintFacts);

    // now apply the stale waiver
    PolicyEvaluation app3PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan app3 id (build)");

    tempEntity.newWaivedPolicyViolation(app3PolicyEvaluationBuild, policy2,
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), "h4", unappliedAppWaiver);

    staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithOrganizationWaivers() {
    Organization org2 = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org2.getId());
    Application app3 = tempEntity.newApplication(org2.getId());

    // unapplied waiver
    PolicyWaiver unappliedOrgWaiver = addUnappliedWaiver(policy, org2.getId());

    // apply 3 other waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy.getId(), org.getId(), "applied waiver1");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", policy.getId(), org2.getId(), "applied waiver2");
    PolicyWaiver policyWaiver3 = tempEntity.newWaiver("h3", policy.getId(), org2.getId(), "applied waiver3");
    evaluateAndApplyWaivers(app1, app2, policyWaiver1, policyWaiver2, policyWaiver3);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedOrgWaiver, "organization", org2.getId(), org2.getName());
    assertContraintFacts(staleWaiver.constraintFacts);

    // now apply the stale waiver
    PolicyEvaluation app3PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan app3 id (build)");
    tempEntity.newWaivedPolicyViolation(app3PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), "h4", unappliedOrgWaiver);

    staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_ApplicationWithRootOrganizationWaivers() {
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(org.getId());

    // unapplied waiver
    PolicyWaiver unappliedRootOrgWaiver = addUnappliedWaiver(policy, Organization.ROOT_ORGANIZATION_ID);

    // apply 3 other waivers
    PolicyWaiver policyWaiver1 =
        tempEntity.newWaiver("h1", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "applied waiver1");
    PolicyWaiver policyWaiver2 =
        tempEntity.newWaiver("h2", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "applied waiver2");
    PolicyWaiver policyWaiver3 =
        tempEntity.newWaiver("h3", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "applied waiver3");
    evaluateAndApplyWaivers(app1, app2, policyWaiver1, policyWaiver2, policyWaiver3);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedRootOrgWaiver, "root_organization",
        Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertContraintFacts(staleWaiver.constraintFacts);

    // now apply the stale waiver
    PolicyEvaluation app3PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan app3 id (build)");
    tempEntity.newWaivedPolicyViolation(app3PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), "h4", unappliedRootOrgWaiver);

    staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).isEmpty();
  }

  private PolicyWaiver addUnappliedWaiver(final Policy policy, final String ownerId) {
    ConstraintFact waiverConstraintFact = new ConstraintFact("constraintFact1", "aa c", "OR");
    waiverConstraintFact
        .addConditionFact(new ConditionFact("MatchState", 0, "Match State is exact", "Match State was exact"));
    return tempEntity
        .newWaiver("h4", policy.getId(), ownerId, Arrays.asList(waiverConstraintFact), "unapplied waiver");
  }

  private void evaluateAndApplyWaivers(
      final Application app1,
      final Application app2,
      final PolicyWaiver app1EvalPolicyWaiver1,
      final PolicyWaiver app1EvalPolicyWaiver2,
      final PolicyWaiver app2EvalPolicyWaiver3)
  {
    PolicyEvaluation app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id (build)");
    PolicyEvaluation app2PolicyEvaluationOperate =
        tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)");

    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", app1EvalPolicyWaiver1);
    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy,
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", app1EvalPolicyWaiver2);
    tempEntity.newWaivedPolicyViolation(app2PolicyEvaluationOperate, policy,
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", app2EvalPolicyWaiver3);
  }

  private void assertStaleWaiver(
      final ApiStaleWaiverDTO policyWaiverDTO,
      final Policy expectedPolicy,
      final PolicyWaiver expectedWaiver,
      final String expectedOwnerType,
      final String expectedOwnerId,
      final String expectedOwnerName)
  {
    assertThat(policyWaiverDTO).isNotNull();
    assertThat(policyWaiverDTO.waiverId).isEqualTo(expectedWaiver.getId());
    assertThat(policyWaiverDTO.policyId).isEqualTo(expectedPolicy.getId());
    assertThat(policyWaiverDTO.policyName).isEqualTo(expectedPolicy.getName());
    assertThat(policyWaiverDTO.comment).isEqualTo(expectedWaiver.getComment());
    assertThat(policyWaiverDTO.createTime).isEqualTo(expectedWaiver.getCreateTime());
    assertThat(policyWaiverDTO.isObsolete).isEqualTo(true);
    assertThat(policyWaiverDTO.scopeOwnerType).isEqualTo(expectedOwnerType);
    assertThat(policyWaiverDTO.scopeOwnerId).isEqualTo(expectedOwnerId);
    assertThat(policyWaiverDTO.scopeOwnerName).isEqualTo(expectedOwnerName);
  }

  private void assertContraintFacts(List<ApiConstraintFactDTO> constraintFacts) {
    assertThat(constraintFacts).hasSize(1);
    ApiConstraintFactDTO constraintFactDTO = constraintFacts.get(0);
    assertThat(constraintFactDTO.constraintId).isNotNull();
    assertThat(constraintFactDTO.constraintName).isNotNull();
    assertThat(constraintFactDTO.reasons).hasSize(1);
    assertThat(constraintFactDTO.reasons.get(0).reason).isEqualTo("Match State was exact");
  }

  @Test
  public void testGetStaleWaivers_LegacyApplicationWaiverWithoutConstraintFacts() {
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)");

    // unapplied waiver
    PolicyWaiver unappliedWaiverWithoutConstraintFacts =
        tempEntity.newWaiver("h4", policy.getId(), app.getId(), "Some comments here");

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedWaiverWithoutConstraintFacts, "application", app.getId(),
        app.getName());
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
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);
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
        repo.getId(), 6, "pathName1", "hash1", constraintFacts1, true, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        repoWaiver.getId(), repoWaiver.getComment(), date);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertStaleWaiver(staleWaiver, policy, unappliedRootOrgWaiver, "root_organization",
        Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertContraintFacts(staleWaiver.constraintFacts);
  }
}
