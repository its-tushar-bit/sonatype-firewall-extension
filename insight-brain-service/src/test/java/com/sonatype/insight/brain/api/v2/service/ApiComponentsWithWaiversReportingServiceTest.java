/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentsWithWaiversReportingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentsWithWaiversReportingService service;

  private Repository repo1;

  private Repository repo2;

  private Organization org1;

  private Organization org2;

  private Application app1;

  private Application app2;

  private Application app3;

  private Policy policy1;

  private Policy policy2;

  private PolicyEvaluation app1PolicyEvaluationBuild;

  private PolicyEvaluation app1PolicyEvaluationRelease;

  private PolicyEvaluation app2PolicyEvaluationOperate;

  private PolicyEvaluation app3PolicyEvaluationBuild;

  private PolicyWaiverDAO policyWaiverDAO;

  @Before
  public void setup() {
    repo1 = tempEntity.newRepository("repo1");
    repo2 = tempEntity.newRepository("repo2");

    org1 = tempEntity.newOrganization();
    org2 = tempEntity.newOrganization();

    app1 = tempEntity.newApplication("app1", org1.getId());
    app2 = tempEntity.newApplication("app2", org1.getId());
    app3 = tempEntity.newApplication("app3", org2.getId());

    policy1 = tempEntity.newPolicy(org1.getId());
    policy2 = tempEntity.newPolicy(org2.getId());

    Date date1 = new Date(System.currentTimeMillis() - 1000);
    Date date2 = new Date(System.currentTimeMillis());

    app1PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id (build)", date1);
    app1PolicyEvaluationRelease =
        tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "test scan app1 id (release)", date2);
    app2PolicyEvaluationOperate =
        tempEntity.newPolicyEvaluation(app2.getId(), OperateStageType.ID, "test scan app2 id (operate)", date2);
    app3PolicyEvaluationBuild =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan app3 id (build)", date2);

    policyWaiverDAO = new PolicyWaiverDAO();
  }

  @Test
  public void testGetComponentsWithWaivers_NoWaivers() {
    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(0);
    assertThat(result.repositoryWaivers).hasSize(0);
  }

  @Test
  public void testGetComponentsWithWaivers_Repositories() {
    Date date = new Date();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    ConstraintFact constraintFact1 =
        new ConstraintFact("constraintFact1", "aa c", "OR");
    constraintFact1.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);

    ConstraintFact constraintFact2 =
        new ConstraintFact("constraintFact2", "aa c", "OR");
    constraintFact2.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts2 = Arrays.asList(constraintFact2);

    // Waived active policy violations and their corresponding waivers
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("hash1", policy1.getId(), repo1.getId(),
        constraintFacts1, "Some comments here");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("hash1", policy1.getId(), repo1.getId(),
        constraintFacts2, "Some comments here2");
    PolicyWaiver policyWaiver3 = tempEntity
        .newWaiver("hash3", policy2.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, constraintFacts1,
            "Some comments here3");
    PolicyWaiver policyWaiver4 = tempEntity.newWaiver("hash4", policy1.getId(), repo1.getId(),
        constraintFacts1, "Some comments here4");

    RepositoryPolicyViolation waivedViolation1 = tempEntity.newRepositoryPolicyViolation(
        repo1.getId(), 6, "pathName1", "hash1", constraintFacts1, true, true,
        "actionId1", policy1.getId(), policy1.getName(), componentIdentifier1, date,
        policyWaiver1.getId(), policyWaiver1.getComment(), date);

    RepositoryPolicyViolation waivedViolation2 = tempEntity.newRepositoryPolicyViolation(
        repo1.getId(), 7, "pathName2", "hash1", constraintFacts2, true, true,
        "actionId2", policy1.getId(), policy1.getName(), componentIdentifier1, date,
        policyWaiver2.getId(), policyWaiver2.getComment(), date);

    RepositoryPolicyViolation waivedViolation3 = tempEntity.newRepositoryPolicyViolation(
        repo2.getId(), 8, "pathName3", "hash3", constraintFacts1, true, true,
        "actionId3", policy2.getId(), policy2.getName(), componentIdentifier1, date,
        policyWaiver3.getId(), policyWaiver3.getComment(), date);

    RepositoryPolicyViolation waivedViolation4 = tempEntity.newRepositoryPolicyViolation(
        repo1.getId(), 9, "pathName4", "hash4", constraintFacts1, true, true,
        "actionId4", policy1.getId(), policy1.getName(), componentIdentifier2, date,
        policyWaiver4.getId(), policyWaiver4.getComment(), date);

    // Non-waived active violation - should not be returned
    tempEntity.newRepositoryPolicyViolation(repo1.getId(), 2, "pathName5", "hash5", constraintFacts1,
        false, true, "actionId5", policy1.getId(), policy1.getName(), componentIdentifier1, date, null, null, null);

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(0);
    assertThat(result.repositoryWaivers).hasSize(2);

    // Validate Repo1 Component Waivers
    ApiRepositoryWaiverDTO apiRepositoryWaiverDTO =
        result.repositoryWaivers.stream().filter(x -> x.repository.repositoryId == repo1.getId()).findFirst().get();
    assertApiRepositoryWaiverDTO(apiRepositoryWaiverDTO, repo1);
    assertThat(apiRepositoryWaiverDTO.stages).hasSize(1);

    ApiPolicyViolationStageDTO apiPolicyViolationStageDTO = apiRepositoryWaiverDTO.stages.get(0);
    assertThat(apiPolicyViolationStageDTO.stageId).isEqualTo(Stage.ID_PROXY);

    List<ApiComponentPolicyViolationDTO> apiComponentPolicyViolationDTOs =
        apiPolicyViolationStageDTO.componentPolicyViolations;
    assertThat(apiComponentPolicyViolationDTOs).hasSize(2);

    ApiComponentPolicyViolationDTO apiComponentPolicyViolationDTO1 = apiComponentPolicyViolationDTOs.get(0);
    ApiComponentPolicyViolationDTO apiComponentPolicyViolationDTO2 = apiComponentPolicyViolationDTOs.get(1);

    // Repo1-Component1 should have 2 violations
    assertComponentDTOV2(apiComponentPolicyViolationDTO1.component, waivedViolation1);
    List<ApiWaivedPolicyViolationDTO> waivedPolicyViolationDTOs =
        apiComponentPolicyViolationDTO1.waivedPolicyViolations;
    assertThat(waivedPolicyViolationDTOs).hasSize(2);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(0), waivedViolation1);
    assertPolicyWaiverDTO(waivedPolicyViolationDTOs.get(0).policyWaiver, policyWaiver1, OwnerType.REPOSITORY.toString(),
        repo1.getName());

    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(1), waivedViolation2);
    assertPolicyWaiverDTO(waivedPolicyViolationDTOs.get(1).policyWaiver, policyWaiver2, OwnerType.REPOSITORY.toString(),
        repo1.getName());

    // Repo1-Component2 should have 1 violation
    assertComponentDTOV2(apiComponentPolicyViolationDTO2.component, waivedViolation4);
    waivedPolicyViolationDTOs = apiComponentPolicyViolationDTO2.waivedPolicyViolations;
    assertThat(waivedPolicyViolationDTOs).hasSize(1);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(0), waivedViolation4);
    assertPolicyWaiverDTO(waivedPolicyViolationDTOs.get(0).policyWaiver, policyWaiver4, OwnerType.REPOSITORY.toString(),
        repo1.getName());

    // Validate Repo2 Component Waivers
    apiRepositoryWaiverDTO =
        result.repositoryWaivers.stream().filter(x -> x.repository.repositoryId == repo2.getId()).findFirst().get();
    assertApiRepositoryWaiverDTO(apiRepositoryWaiverDTO, repo2);
    assertThat(apiRepositoryWaiverDTO.stages).hasSize(1);

    apiPolicyViolationStageDTO = apiRepositoryWaiverDTO.stages.get(0);
    assertThat(apiPolicyViolationStageDTO.stageId).isEqualTo(Stage.ID_PROXY);

    apiComponentPolicyViolationDTOs = apiPolicyViolationStageDTO.componentPolicyViolations;
    assertThat(apiComponentPolicyViolationDTOs).hasSize(1);

    apiComponentPolicyViolationDTO1 = apiComponentPolicyViolationDTOs.get(0);

    // Repo2-Component1 should have 1 violation
    assertComponentDTOV2(apiComponentPolicyViolationDTO1.component, waivedViolation3);
    waivedPolicyViolationDTOs = apiComponentPolicyViolationDTO1.waivedPolicyViolations;
    assertThat(waivedPolicyViolationDTOs).hasSize(1);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(0), waivedViolation3);
    assertPolicyWaiverDTO(waivedPolicyViolationDTOs.get(0).policyWaiver, policyWaiver3,
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_REPOSITORY_CONTAINER, "All Repositories");
  }

  @Test
  public void testGetComponentsWithWaivers_Repositories_MissingWaiver() {
    Date date = new Date();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ConstraintFact constraintFact1 =
        new ConstraintFact("constraintFact1", "aa c", "OR");
    constraintFact1.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);

    // Waived active policy violations and their corresponding waivers
    RepositoryPolicyViolation waivedViolation1 =
        tempEntity.newRepositoryPolicyViolation(repo1.getId(), 6, "pathName1", "hash1", constraintFacts1,
            true, true, "actionId1", policy1.getId(), policy1.getName(), componentIdentifier1, date,
            "deletedPolicyWaiverId", "test waive", date);

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(0);
    assertThat(result.repositoryWaivers).hasSize(1);

    // Validate Repo1 Component Waiver
    ApiRepositoryWaiverDTO apiRepositoryWaiverDTO = result.repositoryWaivers.get(0);
    assertApiRepositoryWaiverDTO(apiRepositoryWaiverDTO, repo1);
    assertThat(apiRepositoryWaiverDTO.stages).hasSize(1);

    ApiPolicyViolationStageDTO apiPolicyViolationStageDTO = apiRepositoryWaiverDTO.stages.get(0);
    assertThat(apiPolicyViolationStageDTO.stageId).isEqualTo(Stage.ID_PROXY);

    List<ApiComponentPolicyViolationDTO> apiComponentPolicyViolationDTOs =
        apiPolicyViolationStageDTO.componentPolicyViolations;
    assertThat(apiComponentPolicyViolationDTOs).hasSize(1);

    ApiComponentPolicyViolationDTO apiComponentPolicyViolationDTO1 = apiComponentPolicyViolationDTOs.get(0);

    // Repo1-Component1 should have 1 violation but not found comment for the related waiver
    assertComponentDTOV2(apiComponentPolicyViolationDTO1.component, waivedViolation1);
    List<ApiWaivedPolicyViolationDTO> waivedPolicyViolationDTOs =
        apiComponentPolicyViolationDTO1.waivedPolicyViolations;
    assertThat(waivedPolicyViolationDTOs).hasSize(1);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(0), waivedViolation1);
    assertThat(waivedPolicyViolationDTOs.get(0).policyWaiver.policyWaiverId).isNull();
    assertThat(waivedPolicyViolationDTOs.get(0).policyWaiver.createTime).isNull();
    assertThat(waivedPolicyViolationDTOs.get(0).policyWaiver.comment)
        .isEqualTo("Related policy waiver not found. Please re-evaluate.");
  }

  @Test
  public void testGetComponentsWithWaivers_Repositories_NullComponentIdentifier() {
    Date date = new Date();
    ConstraintFact constraintFact1 =
        new ConstraintFact("constraintFact1", "aa c", "OR");
    constraintFact1.addConditionFact(new ConditionFact("MatchState", 0,
        "Match State is exact", "Match State was exact"));
    List<ConstraintFact> constraintFacts1 = Arrays.asList(constraintFact1);

    // Waived active policy violations and their corresponding waivers
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", policy1.getId(), repo1.getId(),
        constraintFacts1, "Some comments here");
    RepositoryPolicyViolation waivedViolation =
        tempEntity.newRepositoryPolicyViolation(repo1.getId(), 6, "tomcat/catalina/5.5.15/catalina-5.5.15.jar",
            "hash1", constraintFacts1, true, true, "actionId1", policy1.getId(), policy1.getName(),
            null, date, policyWaiver.getId(), "test waive", date);

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(0);
    assertThat(result.repositoryWaivers).hasSize(1);

    // Validate Repo1 Component Waiver
    ApiRepositoryWaiverDTO apiRepositoryWaiverDTO = result.repositoryWaivers.get(0);
    assertApiRepositoryWaiverDTO(apiRepositoryWaiverDTO, repo1);
    assertThat(apiRepositoryWaiverDTO.stages).hasSize(1);

    ApiPolicyViolationStageDTO apiPolicyViolationStageDTO = apiRepositoryWaiverDTO.stages.get(0);
    assertThat(apiPolicyViolationStageDTO.stageId).isEqualTo(Stage.ID_PROXY);

    List<ApiComponentPolicyViolationDTO> apiComponentPolicyViolationDTOs =
        apiPolicyViolationStageDTO.componentPolicyViolations;
    assertThat(apiComponentPolicyViolationDTOs).hasSize(1);

    ApiComponentPolicyViolationDTO apiComponentPolicyViolationDTO = apiComponentPolicyViolationDTOs.get(0);

    assertThat(apiComponentPolicyViolationDTO.component.componentIdentifier).isNull();
    assertThat(apiComponentPolicyViolationDTO.component.packageUrl).isNull();
    assertThat(apiComponentPolicyViolationDTO.component.proprietary).isNull();
    assertThat(apiComponentPolicyViolationDTO.component.hash).isEqualTo("hash1");

    List<ApiWaivedPolicyViolationDTO> waivedPolicyViolationDTOs =
        apiComponentPolicyViolationDTO.waivedPolicyViolations;
    assertThat(waivedPolicyViolationDTOs).hasSize(1);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTOs.get(0), waivedViolation);
    assertPolicyWaiverDTO(waivedPolicyViolationDTOs.get(0).policyWaiver, policyWaiver, OwnerType.REPOSITORY.toString(),
        repo1.getName());
  }

  @Test
  public void testGetComponentsWithWaivers_Applications() {
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy1.getId(), app1.getId(), "Some comments here");
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("h2", policy1.getId(), app1.getId(), "Some comments here2");
    PolicyWaiver policyWaiver3 = tempEntity.newWaiver("h3", policy1.getId(), app1.getId(), "Some comments here3");
    PolicyWaiver policyWaiver4 =
        tempEntity.newWaiver("h4", policy2.getId(), Organization.ROOT_ORGANIZATION_ID, "Some comments here4");

    PolicyViolation waivedViolation1 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy1,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", policyWaiver1);
    PolicyViolation waivedViolation2 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy1,
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", policyWaiver2);
    PolicyViolation waivedViolation3 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationRelease, policy1,
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", policyWaiver3);
    PolicyViolation waivedViolation4 = tempEntity.newWaivedPolicyViolation(app3PolicyEvaluationBuild, policy2,
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), "h4", policyWaiver4);

    // add a policy violation that we should not pickup
    tempEntity.newPolicyViolation(app2PolicyEvaluationOperate, policy1,
        ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"), "h5");

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(2); // note applications are ordered by public id
    assertThat(result.repositoryWaivers).hasSize(0);
    ApiApplicationWaiverDTO applicationWaiverDTO = result.applicationWaivers.get(0);
    assertThat(applicationWaiverDTO.stages).hasSize(2);

    // first waived violation app 1 build stage
    ApiPolicyViolationStageDTO policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);

    assertThat(policyViolationStageDTO.componentPolicyViolations).hasSize(2);

    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
        policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation1);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation1);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver1, OwnerType.APPLICATION.toString(),
        app1.getName());

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // second waived violation app1 build stage
    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(1);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation2);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation2);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver2, OwnerType.APPLICATION.toString(),
        app1.getName());

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // third waived violation app1 release stage
    policyViolationStageDTO = applicationWaiverDTO.stages.get(1);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(ReleaseStageType.ID);

    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation3);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation3);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver3, OwnerType.APPLICATION.toString(),
        app1.getName());

    assertApplicationWaiverDTO(applicationWaiverDTO, app1);

    // fourth waived violation app3 release build
    applicationWaiverDTO = result.applicationWaivers.get(1);
    policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);

    componentPolicyViolationDTO = policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation4);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation4);

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver4,
        ScopeOwnerUtils.SCOPE_OWNER_TYPE_ROOT_ORGANIZATION, "Root Organization");

    assertApplicationWaiverDTO(applicationWaiverDTO, app3);
  }

  @Test
  public void testGetComponentsWithWaivers_Applications_MissingWaiver() {
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy1.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation1 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy1,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", policyWaiver1);

    policyWaiverDAO.delete(policyWaiver1);

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(1); // note applications are ordered by public id
    assertThat(result.repositoryWaivers).hasSize(0);
    ApiApplicationWaiverDTO applicationWaiverDTO = result.applicationWaivers.get(0);

    assertThat(applicationWaiverDTO.stages).hasSize(1);

    ApiPolicyViolationStageDTO policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);

    assertThat(policyViolationStageDTO.componentPolicyViolations).hasSize(1);
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
        policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation1);

    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation1);

    assertThat(waivedPolicyViolationDTO.policyWaiver.policyWaiverId).isNull();
    assertThat(waivedPolicyViolationDTO.policyWaiver.createTime).isNull();
    assertThat(waivedPolicyViolationDTO.policyWaiver.isObsolete).isTrue();
    assertThat(waivedPolicyViolationDTO.policyWaiver.comment)
        .isEqualTo("Related policy waiver not found. Please re-evaluate.");
  }

  @Test
  public void testGetComponentsWithWaivers_Applications_MissingWaiverOwner() {
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy1.getId(), org2.getId(), "Some comments here");
    PolicyViolation waivedViolation1 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy1,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", policyWaiver1);

    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(1); // note applications are ordered by public id
    assertThat(result.repositoryWaivers).hasSize(0);
    ApiApplicationWaiverDTO applicationWaiverDTO = result.applicationWaivers.get(0);
    assertThat(applicationWaiverDTO.stages).hasSize(1);
    ApiPolicyViolationStageDTO policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);
    assertThat(policyViolationStageDTO.componentPolicyViolations).hasSize(1);
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
        policyViolationStageDTO.componentPolicyViolations.get(0);
    assertComponentDTOV2(componentPolicyViolationDTO.component, waivedViolation1);
    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation1);
    assertThat(waivedPolicyViolationDTO.policyWaiver.policyWaiverId).isEqualTo(policyWaiver1.getId());
    assertThat(waivedPolicyViolationDTO.policyWaiver.createTime).isEqualTo(policyWaiver1.getCreateTime());
    assertThat(waivedPolicyViolationDTO.policyWaiver.isObsolete).isTrue();
    assertThat(waivedPolicyViolationDTO.policyWaiver.comment)
        .isEqualTo("Related policy waiver owner is not in scope. Please re-evaluate.");
    assertThat(waivedPolicyViolationDTO.policyWaiver.scopeOwnerId).isNull();
    assertThat(waivedPolicyViolationDTO.policyWaiver.scopeOwnerName).isNull();
    assertThat(waivedPolicyViolationDTO.policyWaiver.scopeOwnerType).isNull();
  }

  @Test
  public void testGetComponentsWithWaivers_Applications_NullComponentIdentifier() {
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver("h1", policy1.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation1 = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluationBuild, policy1,
        null, "h1", policyWaiver1);
    ApiComponentWaiversDTO result = service.getComponentsWithWaivers();
    assertThat(result.applicationWaivers).hasSize(1);
    assertThat(result.repositoryWaivers).hasSize(0);
    ApiApplicationWaiverDTO applicationWaiverDTO = result.applicationWaivers.get(0);
    assertThat(applicationWaiverDTO.stages).hasSize(1);
    // first waived violation app 1 build stage
    ApiPolicyViolationStageDTO policyViolationStageDTO = applicationWaiverDTO.stages.get(0);
    assertThat(policyViolationStageDTO.stageId).isEqualTo(BuildStageType.ID);
    assertThat(policyViolationStageDTO.componentPolicyViolations).hasSize(1);
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
        policyViolationStageDTO.componentPolicyViolations.get(0);

    assertThat(componentPolicyViolationDTO.component.componentIdentifier).isNull();
    assertThat(componentPolicyViolationDTO.component.packageUrl).isNull();
    assertThat(componentPolicyViolationDTO.component.proprietary).isNull();
    assertThat(componentPolicyViolationDTO.component.hash).isEqualTo("h1");
    assertThat(componentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);

    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = componentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(waivedPolicyViolationDTO, waivedViolation1);
    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver1, OwnerType.APPLICATION.toString(),
        app1.getName());
    assertApplicationWaiverDTO(applicationWaiverDTO, app1);
  }

  private void assertComponentDTOV2(ApiComponentDTOV2 componentDTOV2, AbstractPolicyViolation policyViolation) {
    assertThat(componentDTOV2.hash).isEqualTo(policyViolation.getHash());
    assertThat(componentDTOV2.componentIdentifier.toComponentIdentifier())
        .isEqualTo(policyViolation.getComponentIdentifier());
    assertThat(componentDTOV2.packageUrl)
        .isEqualTo(PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier()));
    assertThat(componentDTOV2.proprietary).isNull();
  }

  private void assertWaivedPolicyViolationDTO(
      ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO,
      AbstractPolicyViolation policyViolation)
  {
    assertThat(waivedPolicyViolationDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivedPolicyViolationDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(waivedPolicyViolationDTO.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(waivedPolicyViolationDTO.threatLevel).isEqualTo(policyViolation.getThreatLevel());

    assertThat(waivedPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = waivedPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }

  private void assertPolicyWaiverDTO(
      ApiPolicyWaiverDTO policyWaiverDTO,
      PolicyWaiver waiver,
      String waiverOwnerType,
      String waiverOwnerName)
  {
    assertThat(policyWaiverDTO.comment).isEqualTo(waiver.getComment());
    assertThat(policyWaiverDTO.createTime).isEqualTo(waiver.getCreateTime());
    assertThat(policyWaiverDTO.policyWaiverId).isEqualTo(waiver.getId());
    assertThat(policyWaiverDTO.scopeOwnerType).isEqualTo(waiverOwnerType);
    assertThat(policyWaiverDTO.scopeOwnerId).isEqualTo(waiver.getOwnerId());
    assertThat(policyWaiverDTO.scopeOwnerName).isEqualTo(waiverOwnerName);
  }

  private void assertApplicationWaiverDTO(ApiApplicationWaiverDTO actual, Application app) {
    ApiApplicationBaseDTO applicationDTO = actual.application;
    assertThat(applicationDTO.id).isEqualTo(app.getId());
    assertThat(applicationDTO.contactUserName).isNull();
    assertThat(applicationDTO.name).isEqualTo(app.getName());
    assertThat(applicationDTO.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(applicationDTO.publicId).isEqualTo(app.getPublicId());
  }

  private void assertApiRepositoryWaiverDTO(ApiRepositoryWaiverDTO apiRepositoryWaiverDTO, Repository repository) {
    assertThat(apiRepositoryWaiverDTO.repository.repositoryId).isEqualTo(repository.getId());
    assertThat(apiRepositoryWaiverDTO.repository.publicId).isEqualTo(repository.getPublicId());
    assertThat(apiRepositoryWaiverDTO.repository.format).isEqualTo(repository.getFormat());
  }
}
