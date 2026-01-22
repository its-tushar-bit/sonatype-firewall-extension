/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleApplicationEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleRepositoryEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiStaleWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiStaleWaiverService apiStaleWaiverService;

  @Test
  public void testGetStaleWaivers_StaleRepositoryWaiversUnauthenticated() {
    assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetStaleWaivers_StaleRepositoryWaiversUnauthorized() {
    login();
    assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetStaleWaivers_StaleWaiversInReadableAndUnreadableRepos() {
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");

    // only has access to repo1
    grantPermission(repo1.getId(), Permission.READ);

    // create waivers, but they are not applied
    PolicyWaiver repo1PolicyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo1.getId(),
        null, "Some comments here");
    tempEntity.newWaiver("hash2", policy.getId(), repo2.getId(), null, "Some comments here2");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleRepositoryWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleRepositoryWaivers.get(0);
    assertThat(staleWaiver).isNotNull();
    assertThat(staleWaiver.waiverId).isEqualTo(repo1PolicyWaiver.getId());
    assertThat(staleWaiver.comment).isEqualTo(repo1PolicyWaiver.getComment());
    assertThat(staleWaiver.createTime).isEqualTo(repo1PolicyWaiver.getCreateTime());
  }

  @Test
  public void testGetStaleWaivers_WaiversUsedInUnreadableRepos() {
    Date date = new Date();
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    ConstraintFact constraintFact = new ConstraintFact("constraintFact1", "aa c", "OR");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // only has access to repo1
    grantPermission(repo1.getId(), Permission.READ);

    PolicyWaiver repo2PolicyWaiver = tempEntity.newWaiver("hash1", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        null, "Some comments here");
    tempEntity.newRepositoryPolicyViolation(
        repo2.getId(), 6, "pathName1", "hash1", Collections.singletonList(constraintFact), true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        repo2PolicyWaiver.getId(), repo2PolicyWaiver.getComment(), date);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).isEmpty();
  }

  private void assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo = tempEntity.newRepository("repo");

    // create waiver, but it is not applied
    tempEntity.newWaiver("hash", policy.getId(), repo.getId(), null, "Some comments here");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleRepositoryWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_StaleApplicationWaiversUnauthenticated() {
    assertEmptyStaleApplicationWaiversWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetStaleWaivers_StaleApplicationWaiversUnauthorized() {
    login();
    assertEmptyStaleApplicationWaiversWhenUnauthorizedOrAuthenticated();
  }

  private void assertEmptyStaleApplicationWaiversWhenUnauthorizedOrAuthenticated() {
    Policy policy = tempEntity.newPolicy(org);
    Date date = new Date(System.currentTimeMillis() - 1000);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)", date);

    // create waiver, but it is not applied
    tempEntity.newWaiver("h1", policy.getId(), app.getId(), "Some comments here");

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();
    assertThat(staleWaivers).isNotNull();
    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_DoesNotReturnStaleWaiversInUnreadableApps() {
    Policy policy = tempEntity.newPolicy(org);
    Application app2 = tempEntity.newApplication(org.getId());

    // only access to app1
    grantPermission(app.getId(), Permission.READ);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)");
    tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)");

    // create waivers, but they are not applied
    PolicyWaiver app1PolicyWaiver = tempEntity.newWaiver("h1", policy.getId(), app.getId(), "Some comments here");
    tempEntity.newWaiver("h2", policy.getId(), app2.getId(), "Some comments here2");

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver).isNotNull();
    assertThat(staleWaiver.waiverId).isEqualTo(app1PolicyWaiver.getId());
    assertThat(staleWaiver.comment).isEqualTo(app1PolicyWaiver.getComment());
    assertThat(staleWaiver.createTime).isEqualTo(app1PolicyWaiver.getCreateTime());
  }

  @Test
  public void testGetStaleWaivers_DoesNotReturnWaiversUsedInUnreadableApps() {
    Policy policy = tempEntity.newPolicy(org);
    Application app2 = tempEntity.newApplication(org.getId());

    // only access to app1
    grantPermission(app.getId(), Permission.READ);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app1 id (build)");
    PolicyEvaluation app2PolicyEvaluation =
        tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)");

    // only apply waiver to the app2 in which the user does not have access to
    PolicyWaiver app2PolicyWaiver = tempEntity.newWaiver("h1", policy.getId(), app2.getId(), "Some comments here");
    tempEntity.newWaivedPolicyViolation(app2PolicyEvaluation, policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", app2PolicyWaiver);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).isEmpty();
  }

  @Test
  public void testGetStaleWaivers_DoesNotReturnStaleEvaluationInUnreadableApps() {
    Date waiverCreateDate = new Date();
    Date staleEvalDate = new Date(waiverCreateDate.getTime() - 5);

    Policy policy = tempEntity.newPolicy(org);
    Application app2 = tempEntity.newApplication("app2", org.getId());

    // only access to app1
    grantPermission(app.getId(), Permission.READ);

    // unapplied waiver at root org - applies to 'app' and 'app2'
    tempEntity.newWaiver("hash", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        new ArrayList<>(), "unapplied waiver", waiverCreateDate);

    // stale evaluations for root org scope
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan app id (build)", staleEvalDate);
    tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)",
        staleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations).isNotNull();
    // user has read access for app1 so only that should be returned under stale evaluations
    assertThat(staleWaiver.staleEvaluations.applications).hasSize(1);
    ApiStaleApplicationEvaluationDTO staleAppEval = staleWaiver.staleEvaluations.applications.get(0);
    assertThat(staleAppEval.application.publicId).isEqualTo(app.getPublicId());
    assertThat(staleAppEval.application.id).isEqualTo(app.getId());
    assertThat(staleAppEval.application.name).isEqualTo(app.getName());
    assertThat(staleAppEval.application.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(staleAppEval.application.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(staleAppEval.stages).hasSize(1);
    assertThat(staleAppEval.stages.get(0).stageId).isEqualTo(BuildStageType.ID);
    assertThat(staleAppEval.stages.get(0).lastEvaluationDate).isEqualTo(staleEvalDate);
  }

  @Test
  public void testGetStaleWaivers_DoesNotReturnStaleEvaluationInUnreadableRepos() {
    Date waiverCreateDate = new Date();
    Date staleEvalDate = new Date(waiverCreateDate.getTime() - 5);
    Date componentCreateDate = new Date(waiverCreateDate.getTime() - 10);

    Policy policy = tempEntity.newPolicy(org);
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    // only has access to repo1
    grantPermission(repo1.getId(), Permission.READ);

    // stale policy waiver scoped at root org
    tempEntity.newWaiver("hash", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        null, "unapplied waiver", waiverCreateDate);

    // stale evaluations for root org scope
    tempEntity.newRepositoryComponent(repo1.getId(), "path", componentCreateDate, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), staleEvalDate);
    tempEntity.newRepositoryComponent(repo2.getId(), "path", componentCreateDate, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), staleEvalDate);

    List<ApiStaleWaiverDTO> staleWaivers = apiStaleWaiverService.getStaleWaivers();

    assertThat(staleWaivers).hasSize(1);
    ApiStaleWaiverDTO staleWaiver = staleWaivers.get(0);
    assertThat(staleWaiver.staleEvaluations).isNotNull();
    // user has read access for repo1 so only that should be returned under stale evaluations
    assertThat(staleWaiver.staleEvaluations.repositories).hasSize(1);
    ApiStaleRepositoryEvaluationDTO staleRepoEval = staleWaiver.staleEvaluations.repositories.get(0);
    assertThat(staleRepoEval.repository.repositoryId).isEqualTo(repo1.getId());
    assertThat(staleRepoEval.repository.publicId).isEqualTo(repo1.getPublicId());
    assertThat(staleRepoEval.repository.format).isEqualTo(repo1.getFormat());
    assertThat(staleRepoEval.stages).hasSize(1);
    assertThat(staleRepoEval.stages.get(0).stageId).isEqualTo(ProxyStageType.ID);
    assertThat(staleRepoEval.stages.get(0).lastEvaluationDate).isEqualTo(staleEvalDate);
  }
}
