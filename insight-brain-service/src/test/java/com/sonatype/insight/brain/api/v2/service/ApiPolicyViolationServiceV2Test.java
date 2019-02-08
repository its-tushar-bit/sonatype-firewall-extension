/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  @Test
  public void testGetPolicyViolations_noPolicyIds() {
    createPolicyTestData("scanId1App1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1");

    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(Collections.emptySet());
    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).isEmpty();
  }

  @Test
  public void testGetPolicyViolations_filteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1");
    PolicyData appPolicyData2 = createPolicyTestData("scanId1App2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", "r2");
    createPolicyTestData("scanId1App3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), "h3", "r3");

    // Get the policy violations for two (out of three) policies
    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId(), appPolicyData2.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(2);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO2 = apiApplicationViolationListDTO.applicationViolations
        .get(1);
    if (appPolicyData1.application.getId().equals(apiApplicationViolationDTO1.application.id)) {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData2);
    }
    else {
      assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData2);
      assertPolicyViolation(apiApplicationViolationDTO2, appPolicyData1);
    }
  }

  @Test
  public void testGetPolicyViolations_nuGetFilteredByPolicyId() {
    PolicyData appPolicyData1 = createPolicyTestData("scanId1App1",
        ComponentIdentifier.createNugetCoordinates("nuget1", "v1"), "h3", "r4");
    createPolicyTestData("scanId1App2", ComponentIdentifier.createNugetCoordinates("nuget2", "v1"), "h4", "r5");

    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO).isNotNull();
    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData1);
  }

  @Test
  public void testGetPolicyViolations_unknownComponent() {
    PolicyData appPolicyData = createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason");

    Set<String> policyIds = Collections.singleton(appPolicyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, appPolicyData);
  }

  @Test
  public void testGetPolicyViolations_ExcludeWaivedViolations() {
    PolicyData policyData = createPolicyTestData("scanId", null /* componentIdentifier */, "testhash", "testreason");
    tempEntity.newWaivedPolicyViolation(policyData.policyEvaluation1, policyData.orgPolicy,
        tempEntity.newWaiver(policyData.orgPolicy.getId(), policyData.organization.getId()));

    Set<String> policyIds = Collections.singleton(policyData.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO, policyData);
  }

  private void assertPolicyViolation(ApiApplicationViolationDTOV2 apiApplicationViolationDTO, PolicyData appPolicyData)
  {
    assertThat(apiApplicationViolationDTO.application).isNotNull();
    assertThat(apiApplicationViolationDTO.application.id).isEqualTo(appPolicyData.application.getId());
    assertThat(apiApplicationViolationDTO.application.name).isEqualTo(appPolicyData.application.getName());
    assertThat(apiApplicationViolationDTO.application.publicId).isEqualTo(appPolicyData.application.getPublicId());
    assertThat(apiApplicationViolationDTO.application.contactUserName)
        .isEqualTo(appPolicyData.application.getContactInternalName());
    assertThat(apiApplicationViolationDTO.application.organizationId)
        .isEqualTo(appPolicyData.application.getOrganizationId());

    assertThat(apiApplicationViolationDTO.policyViolations).hasSize(2);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO1 = apiApplicationViolationDTO.policyViolations.get(0);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO2 = apiApplicationViolationDTO.policyViolations.get(1);

    if (apiPolicyViolationDTO1.policyId.equals(appPolicyData.orgPolicy.getId())) {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1, appPolicyData);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2, appPolicyData);
    }
    else {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2, appPolicyData);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1, appPolicyData);
    }
  }

  private void assertPolicyViolation(ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO,
                                     Application application,
                                     PolicyEvaluation policyEvaluation,
                                     PolicyViolation policyViolation,
                                     PolicyData appPolicyData)
  {
    assertThat(apiPolicyViolationDTO.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(apiPolicyViolationDTO.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + policyEvaluation.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(policyViolation.getHash());
    assertThat(apiPolicyViolationDTO.component.proprietary)
        .isEqualTo(appPolicyData.applicationComponent.isProprietary());
    if (policyViolation.getComponentIdentifier() != null) {
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(
          apiPolicyViolationDTO.component.componentIdentifier.getFormat(),
          apiPolicyViolationDTO.component.componentIdentifier.getCoordinates());
      assertThat(componentIdentifier).isEqualTo(policyViolation.getComponentIdentifier());
    }
    else {
      assertThat(apiPolicyViolationDTO.component.componentIdentifier).isNull();
    }

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(policyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }

  private PolicyData createPolicyTestData(String scanId,
                                          ComponentIdentifier componentIdentifier,
                                          String hash,
                                          String reason)
  {
    PolicyData policyTestData = new PolicyData();
    policyTestData.organization = tempEntity.newOrganization();
    policyTestData.application = tempEntity.newApplication(policyTestData.organization.getId());
    policyTestData.orgPolicy = tempEntity.newPolicy(policyTestData.organization);

    // Create one violation in the past for build stage
    long time = System.currentTimeMillis() - 1000;
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId + "1", new Date(time));
    policyTestData.policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policyTestData.orgPolicy,
        componentIdentifier, hash, reason);

    MatchState matchState = componentIdentifier != null ? MatchState.EXACT : MatchState.UNKNOWN;
    // Create a new evaluation for build stage, retaining the previous violation
    policyTestData.policyEvaluation1 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId, new Date());
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        BuildStageType.ID, hash, componentIdentifier, null, matchState, true, new Date(time));

    // Create a current violation for release stage
    policyTestData.policyEvaluation2 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        ReleaseStageType.ID, scanId, new Date());
    policyTestData.policyViolation2 = tempEntity.newPolicyViolation(policyTestData.policyEvaluation2,
        policyTestData.orgPolicy, componentIdentifier, hash, reason);
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        ReleaseStageType.ID, hash, componentIdentifier, null, matchState, true, new Date(time));

    return policyTestData;
  }

  private static class PolicyData
  {
    public Organization organization;

    public Application application;

    public Policy orgPolicy;

    public PolicyEvaluation policyEvaluation1;

    public PolicyViolation policyViolation1;

    public PolicyEvaluation policyEvaluation2;

    public PolicyViolation policyViolation2;

    public ApplicationComponent applicationComponent;
  }
}
