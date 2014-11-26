/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationViolationDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationViolationListDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiPolicyViolationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class ApiPolicyViolationResourceTest
    extends AbstractResourceTest
{
  private static final String ORG_POLICY_NAME1 = "org-policy1";

  @Test
  public void testGetPolicies() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org.getId(), ORG_POLICY_NAME1);
    PolicyEvaluation pe1App1 = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", new Date());
    PolicyViolation pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");


    String[] policyIds = {orgPolicy.getId()};
    Response response = AuthedRestAccess.get(getServiceURL(policyIds));

    assertResponseStatus(200, response);
    ApiApplicationViolationListDTO apiApplicationViolationListDTO = fromJson(response,
        ApiApplicationViolationListDTO.class);

    assertThat(apiApplicationViolationListDTO.applicationViolations, hasSize(1));
    ApiApplicationViolationDTO apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations.get(0);
    assertThat(apiApplicationViolationDTO.application, CoreMatchers.notNullValue());
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

  private String getServiceURL(final String[] policyIds) {
    StringBuilder builder = new StringBuilder(getRestBaseUrl());
    builder.append(PublicApiPaths.POLICY_VIOLATION_SERVICE_PATH);
    if (policyIds.length > 0) {
      builder.append("?p=").append(policyIds[0]);
      for (int i = 1; i < policyIds.length; i++) {
        builder.append("&p=").append(policyIds[i]);
      }
    }
    return builder.toString();
  }
}
