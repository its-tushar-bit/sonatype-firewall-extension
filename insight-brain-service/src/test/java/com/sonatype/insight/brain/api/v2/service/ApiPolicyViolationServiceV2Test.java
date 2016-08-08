/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

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
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class ApiPolicyViolationServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  private PolicyData appPolicyData1;

  private PolicyData appPolicyData2;

  private PolicyData appPolicyData3;

  @Before
  public void setUpPolicyViolations() {
    // Create three org/apps with policies and policy violations
    appPolicyData1 = createPolicyTestData("org-policy1", "scanId1App1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", "r1");
    appPolicyData2 = createPolicyTestData("org-policy2", "scanId1App2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), "h2", "r2");
    createPolicyTestData("org-policy3", "scanId1App3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"),
        "h3", "r3");
    appPolicyData3 = createPolicyTestData("org-policy4", "scanId1App4",
        ComponentIdentifier.createNugetCoordinates("nuget1", "v1"), "h3", "r4");
    createPolicyTestData("org-policy5", "scanId1App5", ComponentIdentifier.createNugetCoordinates("nuget2", "v1"),
        "h4", "r5");
  }

  @Test
  public void testGetPolicyViolations_noPolicyIds() {
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(Collections.<String> emptySet());
    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(0));
  }

  @Test
  public void testGetPolicyViolations_filteredByPolicyId() {
    // Get two of the three policy violations by policy ids
    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId(), appPolicyData2.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(2));
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
    // Get two of the three policy violations by policy ids
    Set<String> policyIds = Sets.newHashSet(appPolicyData3.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = apiPolicyViolationService
        .getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(1));
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO1 = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertPolicyViolation(apiApplicationViolationDTO1, appPolicyData3);
  }

  private void assertPolicyViolation(ApiApplicationViolationDTOV2 apiApplicationViolationDTO, PolicyData appPolicyData)
  {
    assertThat(apiApplicationViolationDTO.application, notNullValue());
    assertThat(apiApplicationViolationDTO.application.id, is(appPolicyData.application.getId()));
    assertThat(apiApplicationViolationDTO.application.name, is(appPolicyData.application.getName()));
    assertThat(apiApplicationViolationDTO.application.publicId, is(appPolicyData.application.getPublicId()));
    assertThat(apiApplicationViolationDTO.application.contactUserName,
        is(appPolicyData.application.getContactInternalName()));
    assertThat(apiApplicationViolationDTO.application.organizationId, is(appPolicyData.application.getOrganizationId()));

    assertThat(apiApplicationViolationDTO.policyViolations, hasSize(2));
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
    assertThat(apiPolicyViolationDTO.policyId, is(policyViolation.getPolicyId()));
    assertThat(apiPolicyViolationDTO.policyName, is(policyViolation.getPolicyName()));
    assertThat(apiPolicyViolationDTO.threatLevel, is(policyViolation.getThreatLevel()));
    assertThat(apiPolicyViolationDTO.reportUrl, is("ui/links/application/" + application.getPublicId() + "/report/"
        + policyEvaluation.getScanId()));
    assertThat(apiPolicyViolationDTO.stageId, is(policyEvaluation.getStageTypeId()));
    assertThat(apiPolicyViolationDTO.component.hash, is(policyViolation.getHash()));
    assertThat(apiPolicyViolationDTO.component.proprietary, is(appPolicyData.applicationComponent.isProprietary()));
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(
        apiPolicyViolationDTO.component.componentIdentifier.getFormat(),
        apiPolicyViolationDTO.component.componentIdentifier.getCoordinates());
    assertThat(componentIdentifier, is(policyViolation.getComponentIdentifier()));

    assertThat(apiPolicyViolationDTO.constraintViolations, hasSize(1));
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId,
        is(policyViolation.getConstraintFacts().get(0).getConstraintId()));
    assertThat(apiConstraintViolationDTO.constraintName, is(policyViolation.getConstraintFacts().get(0)
        .getConstraintName()));
    assertThat(apiConstraintViolationDTO.reasons, hasSize(1));
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason, is(policyViolation.getConstraintFacts().get(0)
        .getConditionFacts().get(0).getReason()));
  }

  private PolicyData createPolicyTestData(String orgPolicyNName,
                                          String scanId,
                                          ComponentIdentifier componentIdentifier,
                                          String hash,
                                          String reason)
  {
    PolicyData policyTestData = new PolicyData();
    policyTestData.organization = tempEntity.newOrganization();
    policyTestData.application = tempEntity.newApplication(policyTestData.organization.getId());
    policyTestData.orgPolicy = tempEntity.newPolicy(policyTestData.organization.getId(), orgPolicyNName);

    // Create one violation in the past for build stage
    long time = System.currentTimeMillis() - 1000;
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId + "1", new Date(time));
    tempEntity.newPolicyViolation(policyEvaluation, policyTestData.orgPolicy, componentIdentifier, hash, reason);

    // Create a current violation for build stage
    policyTestData.policyEvaluation1 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        BuildStageType.ID, scanId, new Date());
    policyTestData.policyViolation1 = tempEntity.newPolicyViolation(policyTestData.policyEvaluation1,
        policyTestData.orgPolicy, componentIdentifier, hash, reason);
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        BuildStageType.ID, hash, componentIdentifier, null, MatchState.EXACT, true, new Date(time));

    // Create a current violation for release stage
    policyTestData.policyEvaluation2 = tempEntity.newPolicyEvaluation(policyTestData.application.getId(),
        ReleaseStageType.ID, scanId, new Date());
    policyTestData.policyViolation2 = tempEntity.newPolicyViolation(policyTestData.policyEvaluation2,
        policyTestData.orgPolicy, componentIdentifier, hash, reason);
    policyTestData.applicationComponent = tempEntity.newApplicationComponent(policyTestData.application.getId(),
        ReleaseStageType.ID, hash, componentIdentifier, null, MatchState.EXACT, true, new Date(time));

    return policyTestData;
  }

  class PolicyData
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
