/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.joda.time.DateTime;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverDTOTestUtils.assertApiPolicyWaiverDTO;
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
    assertThat(apiPolicyViolationDTO.policyViolationId).isEqualTo(pv1App1.getId());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(pv1App1.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + pe1App1.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(pe1App1.getStageTypeId());
    assertThat(apiPolicyViolationDTO.reportId).isEqualTo("scanId1App1");
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(pv1App1.getHash());
    assertThat(apiPolicyViolationDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(pv1App1.getComponentIdentifier());
    assertThat(apiPolicyViolationDTO.component.packageUrl).isEqualTo("pkg:maven/g1/a1@v1");
    assertThat(apiPolicyViolationDTO.component.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(pv1App1.getComponentIdentifier()).toString());

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId).isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintName());

    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
    assertThat(apiConstraintViolationReasonDTO.reference).isNotNull();
    assertThat(apiConstraintViolationReasonDTO.reference.value)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
    assertThat(apiConstraintViolationReasonDTO.reference.type).isEqualTo("SECURITY_VULNERABILITY_REFID");
  }

  @Test
  public void testGetCrossStagePolicyViolationById() throws Exception {
    Date date = new Date();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe1App1 = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", false, false, date);
    PolicyViolation pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    String fullPath = DefaultApiPolicyViolationResourceV2.CROSS_STAGE_POLICY_VIOLATION_SUBPATH
        + DefaultApiPolicyViolationResourceV2.VIOLATIONID;

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(fullPath)
        .parameter(pv1App1.getId())
        .get();

    assertResponseStatus(200, response);
    ApiCrossStageViolationDTOV2 resultDTO = response.getBody(ApiCrossStageViolationDTOV2.class);
    assertThat(resultDTO.policyViolationId).isEqualTo(pv1App1.getId());
    assertThat(resultDTO.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(resultDTO.applicationName).isEqualTo(app.getName());
    assertThat(resultDTO.stageData).containsOnlyKeys(BuildStageType.ID);
    assertThat(resultDTO.displayName.toString()).isEqualTo("g1 : a1 : v1");
    assertThat(resultDTO.stageData).hasEntrySatisfying(BuildStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(date);
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId1App1");
    });
  }

  @Test
  public void testGetCrossStagePolicyViolationByConstituentId() throws Exception {
    Date baseDate = new Date();
    Date later = new Date(baseDate.getTime() + 1);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", false,
        false, baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    // Equivalent, opened while violation1 was still open
    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(app.getId(), DevelopStageType.ID, "scanId2App1", false, false, later);
    tempEntity.newPolicyViolation(evaluation2, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(DefaultApiPolicyViolationResourceV2.CROSS_STAGE_POLICY_VIOLATION_SUBPATH)
        .query("constituentId", violation1.getId())
        .get();

    assertResponseStatus(200, response);
    ApiCrossStageViolationDTOV2 resultDTO = response.getBody(ApiCrossStageViolationDTOV2.class);
    assertThat(resultDTO.policyViolationId).isEqualTo(violation1.getId());
    assertThat(resultDTO.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(resultDTO.applicationName).isEqualTo(app.getName());
    assertThat(resultDTO.stageData).hasSize(2);
    assertThat(resultDTO.stageData).containsOnlyKeys(BuildStageType.ID, DevelopStageType.ID);
    assertThat(resultDTO.displayName.toString()).isEqualTo("g1 : a1 : v1");
    assertThat(resultDTO.stageData).hasEntrySatisfying(BuildStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(baseDate);
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId1App1");
    });
    assertThat(resultDTO.stageData).hasEntrySatisfying(DevelopStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(new Date(baseDate.getTime() + 1));
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId2App1");
    });
  }

  @Test
  public void testGetApplicableWaivers() throws Exception {
    DateTime now = DateTime.now();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    Policy policy2 = tempEntity.newPolicy(newApp);
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    new PolicyViolationDAO().update(violation);

    String policyId = policy.getId();
    String policy2Id = policy2.getId();
    String orgId = newOrg.getId();
    String appId = newApp.getId();

    Date expiredExpiryTime = now.minusMillis(1).toDate();
    Date expiringInFutureExpiryTime = now.plusMinutes(1).toDate();

    tempEntity.newWaiver("hash", policyId, orgId, constraintFacts, "", now.minusDays(10).toDate());
    tempEntity.newWaiver(null, policyId, orgId, constraintFacts, "", now.minusDays(9).toDate(), null);
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts, "", now.minusDays(8).toDate(),
        expiredExpiryTime); // expired
    tempEntity.newWaiver(null, policyId, appId, constraintFacts, "A comment", now.minusDays(7).toDate(),
        expiringInFutureExpiryTime); // expiring in the future
    tempEntity.newWaiver("hash2", policy2Id, appId, null, "", now.minusDays(2).toDate(), null);
    tempEntity.newWaiver(null, policy2Id, appId, null, "", now.minusDays(1).toDate(), now.plusMinutes(1).toDate());
    tempEntity.newWaiver("hash", policy2Id, appId, constraintFacts, "", now.toDate(), now.minusMillis(1).toDate());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(DefaultApiPolicyViolationResourceV2.VIOLATIONID +
            DefaultApiPolicyViolationResourceV2.APPLICABLE_WAIVERS_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(200, response);
    ApiPolicyWaiversApplicableToViolationDTO apiPolicyWaivers =
        response.getBody(ApiPolicyWaiversApplicableToViolationDTO.class);

    List<ApiPolicyWaiverDTO> activeApplicableWaivers = apiPolicyWaivers.activeWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(activeApplicableWaivers.size()).isEqualTo(3);
    assertApiPolicyWaiverDTO("hash", policyId, orgId, "NewOrg", "", null, activeApplicableWaivers.get(0));
    assertApiPolicyWaiverDTO(null, policyId, orgId, "NewOrg", "", null, activeApplicableWaivers.get(1));
    assertApiPolicyWaiverDTO(null, policyId, appId, "NewApp", "A comment", expiringInFutureExpiryTime,
        activeApplicableWaivers.get(2));

    List<ApiPolicyWaiverDTO> expiredApplicableWaivers = apiPolicyWaivers.expiredWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(expiredApplicableWaivers.size()).isEqualTo(1);
    assertApiPolicyWaiverDTO("hash", policyId, appId, "NewApp", "", expiredExpiryTime, expiredApplicableWaivers.get(0));
  }
}
