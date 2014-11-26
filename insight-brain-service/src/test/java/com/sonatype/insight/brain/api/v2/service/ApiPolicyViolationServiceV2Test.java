/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
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

  @Before
  public void setUpPolicyViolations() {
    // Create three org/apps with policies and policy violations
    appPolicyData1 = createPolicyTestData("org-policy1", "scanId1App1", "g1", "a1", "v1", "h1", "r1");
    appPolicyData2 = createPolicyTestData("org-policy2", "scanId1App2", "g2", "a2", "v2", "h2", "r2");
    createPolicyTestData("org-policy3", "scanId1App3", "g3", "a3", "v3", "h3", "r3");
  }

  @Test
  public void testGetPolicyViolations_noPolicyIds() {
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(Collections.<String>emptySet());
    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(0));
  }

  @Test
  public void testGetPolicyViolations_filteredByPolicyId() {
    // Get two of the three policy violations by policy ids
    Set<String> policyIds = Sets.newHashSet(appPolicyData1.orgPolicy.getId(), appPolicyData2.orgPolicy.getId());
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(policyIds);

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

  private void assertPolicyViolation(ApiApplicationViolationDTOV2 apiApplicationViolationDTO, PolicyData appPolicyData) {
    assertThat(apiApplicationViolationDTO.application, notNullValue());
    assertThat(apiApplicationViolationDTO.application.id, is(appPolicyData.application.getId()));
    assertThat(apiApplicationViolationDTO.application.name, is(appPolicyData.application.getName()));
    assertThat(apiApplicationViolationDTO.application.publicId, is(appPolicyData.application.getPublicId()));
    assertThat(apiApplicationViolationDTO.application.contactUserName,
        is(appPolicyData.application.getContactInternalName()));
    assertThat(apiApplicationViolationDTO.application.organizationId,
        is(appPolicyData.application.getOrganizationId()));

    assertThat(apiApplicationViolationDTO.policyViolations, hasSize(2));
    ApiPolicyViolationDTOV2 apiPolicyViolationDTO1 = apiApplicationViolationDTO.policyViolations.get(0);
    ApiPolicyViolationDTOV2 apiPolicyViolationDTO2 = apiApplicationViolationDTO.policyViolations.get(1);

    if (apiPolicyViolationDTO1.policyId.equals(appPolicyData.orgPolicy.getId())) {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2);
    }
    else {
      assertPolicyViolation(apiPolicyViolationDTO1, appPolicyData.application, appPolicyData.policyEvaluation2,
          appPolicyData.policyViolation2);
      assertPolicyViolation(apiPolicyViolationDTO2, appPolicyData.application, appPolicyData.policyEvaluation1,
          appPolicyData.policyViolation1);
    }
  }

  private void assertPolicyViolation(ApiPolicyViolationDTOV2 apiPolicyViolationDTO, Application application,
      PolicyEvaluation policyEvaluation, PolicyViolation policyViolation)
  {
    assertThat(apiPolicyViolationDTO.policyId, is(policyViolation.getPolicyId()));
    assertThat(apiPolicyViolationDTO.policyName, is(policyViolation.getPolicyName()));
    assertThat(apiPolicyViolationDTO.reportUrl,
        is("ui/links/application/" + application.getPublicId() + "/report/" + policyEvaluation.getScanId()));
    assertThat(apiPolicyViolationDTO.stageId, is(policyEvaluation.getStageTypeId()));
    assertThat(apiPolicyViolationDTO.component.hash, is(policyViolation.getHash()));
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(
        apiPolicyViolationDTO.component.componentIdentifier.getFormat(),
        apiPolicyViolationDTO.component.componentIdentifier.getCoordinates()
        );
    assertThat(componentIdentifier, is(policyViolation.getComponentIdentifier()));

    assertThat(apiPolicyViolationDTO.constraintViolations, hasSize(1));
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId,
        is(policyViolation.getConstraintFacts().get(0).getConstraintId()));
    assertThat(apiConstraintViolationDTO.constraintName,
        is(policyViolation.getConstraintFacts().get(0).getConstraintName()));
    assertThat(apiConstraintViolationDTO.reasons, hasSize(1));
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason,
        is(policyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason()));
  }

  private PolicyData createPolicyTestData(String orgPolicyNName, String scanId, String groupId, String artifactId,
      String version, String hash, String reason)
  {
    PolicyData policyTestData = new PolicyData();
    policyTestData.organization = tempEntity.newOrganization();
    policyTestData.application = tempEntity.newApplication(policyTestData.organization.getId());
    policyTestData.orgPolicy = tempEntity.newPolicy(policyTestData.organization.getId(), orgPolicyNName);

    // Create one violation in the past for build stage
    long time = System.currentTimeMillis() - 1000;
    PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(policyTestData.application.getId(), BuildStageType.ID, scanId + "1", new Date(time));
    tempEntity.newPolicyViolation(policyEvaluation, policyTestData.orgPolicy, groupId, artifactId,
        version, hash, reason);

    // Create a current violation for build stage
    policyTestData.policyEvaluation1 = tempEntity
        .newPolicyEvaluation(policyTestData.application.getId(), BuildStageType.ID, scanId, new Date());
    policyTestData.policyViolation1 = tempEntity
        .newPolicyViolation(policyTestData.policyEvaluation1, policyTestData.orgPolicy, groupId, artifactId, version,
            hash, reason);

    // Create a current violation for release stage
    policyTestData.policyEvaluation2 = tempEntity
        .newPolicyEvaluation(policyTestData.application.getId(), ReleaseStageType.ID, scanId, new Date());
    policyTestData.policyViolation2 = tempEntity
        .newPolicyViolation(policyTestData.policyEvaluation2, policyTestData.orgPolicy, groupId, artifactId, version,
            hash, reason);

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
  }
}
