/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO.ReasonDTO;
import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class ComponentDetailServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentDetailService componentDetailService;

  @Test
  public void testGetApplicationDetailsByHash() {
    String hash = "ababababab";

    // app1 has the component without any policy violations
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");

    // app2 has the component with policy violations
    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, hash, "groupId", "artifactId", "version");
    // add two policy violations for a stage
    Policy policy1 = tempEntity.newPolicy(app2.getId(), "policy1", 1);
    Policy policy2 = tempEntity.newPolicy(app2.getId(), "policy2", 1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy2, "groupId", "artifactId", "version", hash, "reason2");
    // add another policy violation for a different stage and with a different threat level
    policy1.setThreatLevel(2);
    new PolicyDAO().update(policy1);
    while (System.currentTimeMillis() <= policyEvaluation1.getTime().getTime()) {
      // just spinning until next policy eval time is guaranteed to be greater than time for the eval created above
    }
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "scanId2");
    tempEntity.newPolicyViolation(policyEvaluation2, policy1, "groupId", "artifactId", "version", hash, "reason3");

    // app3 does not have the component
    tempEntity.newApplicationWithParent("app3");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(2));
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId(), is(app1.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(0));
    appComponentDetailsDTO = appComponentDetailsDTOs.get(1);
    assertThat(appComponentDetailsDTO.application.getId(), is(app2.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(2));
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy1.getId(),
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is(policy1.getName()));
    assertThat(policyViolationSummaryDTO.threatLevel, is(2));
    assertThat(policyViolationSummaryDTO.stageDetails, hasSize(4));
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.BUILD, null,
        policyEvaluation1.getScanId(), policyEvaluation1.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.RELEASE, null,
        policyEvaluation2.getScanId(), policyEvaluation2.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.OPERATE, null, null, null);

    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    ReasonDTO reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason3"));
    policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy2.getId(), appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is(policy2.getName()));
    assertThat(policyViolationSummaryDTO.threatLevel, is(1));
    assertThat(policyViolationSummaryDTO.stageDetails, hasSize(4));
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.BUILD, null,
        policyEvaluation1.getScanId(), policyEvaluation1.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.OPERATE, null, null, null);
    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason2"));
  }

  @Test
  public void testGetApplicationDetailsByHash_MissingPolicy() {
    String hash = "ababababab";

    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, "groupId", "artifactId", "version");
    Policy policy = tempEntity.newPolicy(app.getId(), "policy", 1);
    String policyId = policy.getId();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "groupId", "artifactId", "version", hash, "reason");
    new PolicyDAO().delete(policy);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId(), is(app.getId()));
    assertThat(appComponentDetailsDTO.policyViolations, notNullValue());
    assertThat(appComponentDetailsDTO.policyViolations, hasSize(1));
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policyId,
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO, notNullValue());
    assertThat(policyViolationSummaryDTO.policyName, is("policy"));
    assertThat(policyViolationSummaryDTO.threatLevel, is(1));
    assertThat(policyViolationSummaryDTO.stageDetails, hasSize(4));
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.BUILD, null,
        policyEvaluation.getScanId(), policyEvaluation.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.OPERATE, null, null, null);
    assertThat(policyViolationSummaryDTO.reasons, hasSize(1));
    ReasonDTO reasonDTO = policyViolationSummaryDTO.reasons.get(0);
    assertThat(reasonDTO.constraintName, is("Test Constraint"));
    assertThat(reasonDTO.reasons, containsInAnyOrder("reason"));
  }

  @Test
  public void testGetApplicationDetailsByHash_ExcludesDevelopStage() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), DevelopStageType.ID, hash, "groupId", "artifactId", "version");
    Policy policy1 = tempEntity.newPolicy(app1.getId(), "policy1", 1);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");

    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), DevelopStageType.ID, hash, "groupId", "artifactId", "version");
    Policy policy2 = tempEntity.newPolicy(app2.getId(), "policy2", 1);
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation2, policy2, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");
    PolicyEvaluation evaluation3 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation3, policy2, "groupId", "artifactId", "version", hash, "reason1");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId(), is(app2.getId()));
    assertThat(dto.stageDetails, hasSize(4));
    assertStageDetails(dto.stageDetails.get(0), StageTypes.BUILD, null, null, null);
    assertStageDetails(dto.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(2), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(3), StageTypes.OPERATE, null, null, null);
    assertThat(dto.policyViolations, hasSize(1));
    assertThat(dto.policyViolations.get(0).stageDetails, hasSize(4));
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(0), StageTypes.BUILD, null,
        evaluation3.getScanId(), evaluation3.getTime().getTime());
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(2), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(3), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHash_FirstViolationOccurrence_LatestReportAndAction() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    ApplicationComponent component = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        "groupId", "artifactId", "version");
    Policy policy1 = tempEntity.newPolicy(app1.getId(), "policy1");
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", new Date(
        System.currentTimeMillis() - 1000));
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, policy1, policy1.getThreatLevel(),
        policy1.getThreatCategory(), component.getGroupId(), component.getArtifactId(), component.getVersion(), hash,
        WarnActionType.ID);
    tempEntity.newNewestPolicyViolation(violation1.getId(), evaluation1.getApplicationId(),
        evaluation1.getStageTypeId());

    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2");
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation2, policy1, policy1.getThreatLevel(),
        policy1.getThreatCategory(), component.getGroupId(), component.getArtifactId(), component.getVersion(), hash,
        FailActionType.ID);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId(), is(app1.getId()));
    assertThat(dto.policyViolations, hasSize(1));
    assertThat(dto.policyViolations.get(0).stageDetails, hasSize(4));
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(0), StageTypes.BUILD, violation2.getActionTypeId(),
        evaluation2.getScanId(), evaluation1.getTime().getTime());
  }

  @Test
  public void testGetApplicationDetailsByHash_MostSevereActionForAppLevel() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    ApplicationComponent component = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        "groupId", "artifactId", "version");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, component.getHash(), component.getGroupId(),
        component.getArtifactId(), component.getVersion());

    Policy policy1 = tempEntity.newPolicy(app1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(app1.getId(), "policy2");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation1, policy1, policy1.getThreatLevel(), policy1.getThreatCategory(),
        component.getGroupId(), component.getArtifactId(), component.getVersion(), hash, WarnActionType.ID);
    tempEntity.newPolicyViolation(evaluation1, policy2, policy2.getThreatLevel(), policy2.getThreatCategory(),
        component.getGroupId(), component.getArtifactId(), component.getVersion(), hash, null);

    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2");
    tempEntity.newPolicyViolation(evaluation2, policy1, policy1.getThreatLevel(), policy1.getThreatCategory(),
        component.getGroupId(), component.getArtifactId(), component.getVersion(), hash, WarnActionType.ID);
    tempEntity.newPolicyViolation(evaluation2, policy2, policy2.getThreatLevel(), policy2.getThreatCategory(),
        component.getGroupId(), component.getArtifactId(), component.getVersion(), hash, FailActionType.ID);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId(), is(app1.getId()));
    assertThat(dto.stageDetails, hasSize(4));
    assertStageDetails(dto.stageDetails.get(0), StageTypes.BUILD, WarnActionType.ID, null, evaluation1.getTime().getTime());
    assertStageDetails(dto.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(2), StageTypes.RELEASE, FailActionType.ID, null, evaluation2.getTime().getTime());
    assertStageDetails(dto.stageDetails.get(3), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHash_MostRecentTimeAmongMostSevereActionForAppLevel() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    ApplicationComponent component = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        "groupId", "artifactId", "version");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, component.getHash(), component.getGroupId(),
        component.getArtifactId(), component.getVersion());

    Policy policy1 = tempEntity.newPolicy(app1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(app1.getId(), "policy2");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        new Date());
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, policy1, policy1.getThreatLevel(),
        policy1.getThreatCategory(), component.getGroupId(), component.getArtifactId(), component.getVersion(), hash,
        WarnActionType.ID);
    tempEntity.newNewestPolicyViolation(violation1.getId(), app1.getId(), BuildStageType.ID);

    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2", new Date(
        evaluation1.getTime().getTime() + 1000));
    tempEntity.newPolicyViolation(evaluation2, policy1, policy1.getThreatLevel(), policy1.getThreatCategory(),
        component.getGroupId(), component.getArtifactId(), component.getVersion(), hash, WarnActionType.ID);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation2, policy2, policy2.getThreatLevel(),
        policy2.getThreatCategory(), component.getGroupId(), component.getArtifactId(), component.getVersion(), hash,
        WarnActionType.ID);
    tempEntity.newNewestPolicyViolation(violation2.getId(), app1.getId(), BuildStageType.ID);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs, notNullValue());
    assertThat(appComponentDetailsDTOs, hasSize(1));
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId(), is(app1.getId()));
    assertThat(dto.stageDetails, hasSize(4));
    assertStageDetails(dto.stageDetails.get(0), StageTypes.BUILD, WarnActionType.ID, null, evaluation2.getTime().getTime());
    assertStageDetails(dto.stageDetails.get(1), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(2), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(3), StageTypes.OPERATE, null, null, null);
  }

  private void assertStageDetails(StageDetailDTO stageDetailDTO, StageType stageType, String actionType, String scanId,
      Long time)
  {
    assertThat(stageDetailDTO.stageTypeId, is(stageType.getId()));
    assertThat(stageDetailDTO.stageTypeName, is(stageType.getName()));
    assertThat(stageDetailDTO.actionTypeId, is(actionType));
    assertThat(stageDetailDTO.scanId, is(scanId));
    assertThat(stageDetailDTO.time, is(time));
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, "groupId1", "artifactId1", "version1");
    // Force different times on the two ApplicationComponents
    Thread.sleep(1);
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, "groupId2", "artifactId2", "version2");

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, is("groupId2:artifactId2:version2"));
  }

  @Test
  public void testGetComponentNameByHash_UnnownHash() throws Exception {
    String hash = "ababababab";

    try {
      componentDetailService.getComponentNameByHash(hash);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Unknown component with hash ababababab"));
    }
  }

  @Test
  public void testGetComponentNameByHash_NoGAV() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* groupId */,
        null /* artifactId */, null /* version */, "somepath");

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, is("somepath"));
  }

  @Test
  public void testGetComponentNameByHash_NoGAVOrPathnames() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* groupId */,
        null /* artifactId */, null /* version */);

    String name = componentDetailService.getComponentNameByHash(hash);
    assertThat(name, nullValue());
  }

  private PolicyViolationSummaryDTO getPolicyViolationSummaryDTO(String policyId,
      List<PolicyViolationSummaryDTO> policyViolations)
  {
    for (PolicyViolationSummaryDTO policyViolation : policyViolations) {
      if (policyViolation.policyId.equals(policyId)) {
        return policyViolation;
      }
    }
    return null;
  }
}
