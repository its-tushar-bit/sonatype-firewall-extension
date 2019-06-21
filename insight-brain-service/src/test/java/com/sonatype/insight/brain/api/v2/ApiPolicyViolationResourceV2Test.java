/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetPolicyViolations() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe1App1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest().path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId()).get();

    assertResponseStatus(200, response);
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = response
        .getBody(ApiApplicationViolationListDTOV2.class);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertThat(apiApplicationViolationDTO.application).isNotNull();
    assertThat(apiApplicationViolationDTO.application.id).isEqualTo(app.getId());
    assertThat(apiApplicationViolationDTO.application.name).isEqualTo(app.getName());
    assertThat(apiApplicationViolationDTO.application.publicId).isEqualTo(app.getPublicId());
    assertThat(apiApplicationViolationDTO.application.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(apiApplicationViolationDTO.application.organizationId).isEqualTo(app.getOrganizationId());

    assertThat(apiApplicationViolationDTO.policyViolations).hasSize(1);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = apiApplicationViolationDTO.policyViolations.get(0);
    assertThat(apiPolicyViolationDTO.policyId).isEqualTo(pv1App1.getPolicyId());
    assertThat(apiPolicyViolationDTO.policyName).isEqualTo(pv1App1.getPolicyName());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(pv1App1.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + pe1App1.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(pe1App1.getStageTypeId());
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(pv1App1.getHash());
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(
        apiPolicyViolationDTO.component.componentIdentifier.getFormat(),
        apiPolicyViolationDTO.component.componentIdentifier.getCoordinates());
    assertThat(componentIdentifier).isEqualTo(pv1App1.getComponentIdentifier());
    assertThat(apiPolicyViolationDTO.component.packageUrl).isEqualTo("pkg:maven/g1/a1@v1");

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId).isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintName());

    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
  }
}
