/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.Date;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.dto.ApiApplicationViolationDTO;
import com.sonatype.insight.brain.api.dto.ApiApplicationViolationListDTO;
import com.sonatype.insight.brain.api.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.dto.ApiPolicyViolationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class ApiPolicyViolationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String ORG_POLICY_NAME1 = "org-policy1";

  private static final String APP_POLICY_NAME1 = "app-policy1";

  private static final String ORG_POLICY_NAME2 = "org-policy2";

  private static final String APP_POLICY_NAME2 = "app-policy2";

  @Inject
  private ApiPolicyViolationService apiPolicyViolationService;

  private Policy orgPolicy;

  private PolicyEvaluation pe1App1;

  private PolicyViolation pv1App1;

  @Before
  public void setUpPolicyViolations() {
    orgPolicy = tempEntity.newPolicy(org.getId(), ORG_POLICY_NAME1);
    tempEntity.newPolicy(app.getId(), APP_POLICY_NAME1);

    // One policy violation for app1
    pe1App1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", new Date());
    pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");


    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    tempEntity.newPolicy(org2.getId(), ORG_POLICY_NAME2);
    Policy app2Policy = tempEntity.newPolicy(app2.getId(), APP_POLICY_NAME2);

    // One policy violation for app2
    PolicyEvaluation pe1App2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1App2",
        new Date());
    tempEntity.newPolicyViolation(pe1App2, app2Policy, "g2", "a2", "v2", "h2", "r2");
  }

  @Test
  public void testGetPolicyViolations_AuthorizedOneApp() {
    grantReadPermission(app.getId());

    Set<String> policyIds = Sets.newHashSet(orgPolicy.getId());
    ApiApplicationViolationListDTO apiApplicationViolationListDTO =
        apiPolicyViolationService.getPolicyViolations(policyIds);

    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(1));
    ApiApplicationViolationDTO apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations.get(0);
    assertThat(apiApplicationViolationDTO.application, notNullValue());
    assertThat(apiApplicationViolationDTO.application.id, is(app.getId()));
    assertThat(apiApplicationViolationDTO.application.name, is(app.getName()));
    assertThat(apiApplicationViolationDTO.application.publicId, is(app.getPublicId()));
    assertThat(apiApplicationViolationDTO.application.contactUserName, is(app.getContactInternalName()));
    assertThat(apiApplicationViolationDTO.application.organizationId, is(app.getOrganizationId()));

    assertThat(apiApplicationViolationDTO.policyViolations, hasSize(1));
    ApiPolicyViolationDTO apiPolicyViolationDTO = apiApplicationViolationDTO.policyViolations.get(0);
    assertThat(apiPolicyViolationDTO.policyId, is(pv1App1.getPolicyId()));
    assertThat(apiPolicyViolationDTO.policyName, is(pv1App1.getPolicyName()));
    assertThat(apiPolicyViolationDTO.reportUrl,
        is("ui/links/application/" + app.getPublicId() + "/report/" + pe1App1.getScanId()));
    assertThat(apiPolicyViolationDTO.stageId, is(pe1App1.getStageTypeId()));
    assertThat(apiPolicyViolationDTO.mavenComponent.hash, is(pv1App1.getHash()));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        apiPolicyViolationDTO.mavenComponent.groupId, apiPolicyViolationDTO.mavenComponent.artifactId,
        apiPolicyViolationDTO.mavenComponent.version);
    assertThat(componentIdentifier, is(pv1App1.getComponentIdentifier()));

    assertThat(apiPolicyViolationDTO.constraintViolations, hasSize(1));
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId, is(pv1App1.getConstraintFacts().get(0).getConstraintId()));
    assertThat(apiConstraintViolationDTO.constraintName, is(pv1App1.getConstraintFacts().get(0).getConstraintName()));

    assertThat(apiConstraintViolationDTO.reasons, hasSize(1));
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason,
        is(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason()));
  }

  @Test
  public void testGetPolicyViolations_Unauthenticated() {
    assertEmptyWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetPolicyViolations_UnauthorizedButAuthenticated() {
    login();
    assertEmptyWhenUnauthorizedOrAuthenticated();
  }

  private void assertEmptyWhenUnauthorizedOrAuthenticated() {
    Set<String> policyIds = Sets.newHashSet(orgPolicy.getId());
    ApiApplicationViolationListDTO apiApplicationViolationListDTO = apiPolicyViolationService.getPolicyViolations(
        policyIds);
    assertThat(apiApplicationViolationListDTO, notNullValue());
    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(0));
  }
}
