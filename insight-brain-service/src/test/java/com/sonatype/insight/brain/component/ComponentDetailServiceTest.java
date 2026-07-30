/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
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
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValue;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class ComponentDetailServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ComponentDetailService componentDetailService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetApplicationDetailsByHash() {
    String hash = "ababababab";

    // app1 has the component without any policy violations
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    // app2 has the component with policy violations
    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    // add two policy violations for a stage
    Policy policy1 = tempEntity.newPolicy(app2);
    Policy policy2 = tempEntity.newPolicy(app2);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash);
    tempEntity.newPolicyViolation(policyEvaluation1, policy2, "groupId", "artifactId", "version", hash);
    // add another policy violation for a different stage and with a different threat level
    policy1.setThreatLevel(2);
    policyDAO.update(policy1);
    while (System.currentTimeMillis() <= policyEvaluation1.getTime().getTime()) {
      // just spinning until next policy eval time is guaranteed to be greater than time for the eval created above
    }
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "scanId2");
    tempEntity.newPolicyViolation(policyEvaluation2, policy1, "groupId", "artifactId", "version", hash);

    // app3 does not have the component
    tempEntity.newApplicationWithParent("app3");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).hasSize(2);
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId()).isEqualTo(app1.getId());
    assertThat(appComponentDetailsDTO.policyViolations).isEmpty();
    appComponentDetailsDTO = appComponentDetailsDTOs.get(1);
    assertThat(appComponentDetailsDTO.application.getId()).isEqualTo(app2.getId());
    assertThat(appComponentDetailsDTO.policyViolations).hasSize(2);
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy1.getId(),
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO).isNotNull();
    assertThat(policyViolationSummaryDTO.policyName).isEqualTo(policy1.getName());
    assertThat(policyViolationSummaryDTO.threatLevel).isEqualTo(2);
    assertThat(policyViolationSummaryDTO.stageDetails).hasSize(5);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.BUILD, null,
        policyEvaluation1.getScanId(), policyEvaluation1.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.RELEASE, null,
        policyEvaluation2.getScanId(), policyEvaluation2.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(4), StageTypes.OPERATE, null, null, null);

    policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policy2.getId(), appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO).isNotNull();
    assertThat(policyViolationSummaryDTO.policyName).isEqualTo(policy2.getName());
    assertThat(policyViolationSummaryDTO.threatLevel).isEqualTo(5);
    assertThat(policyViolationSummaryDTO.stageDetails).hasSize(5);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.BUILD, null,
        policyEvaluation1.getScanId(), policyEvaluation1.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(4), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHashMultipleConstraints() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    // add two policy violations for a stage, differing only in condition constraint fact reasons
    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "constraint1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "constraint2");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId()).isEqualTo(app1.getId());
    assertThat(appComponentDetailsDTO.policyViolations).hasSize(2);

    PolicyViolationSummaryDTO policyViolationSummaryDTO1 = getPolicyViolationSummaryDTO(policy1.getId(),
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO1).isNotNull();
    assertThat(policyViolationSummaryDTO1.policyName).isEqualTo(policy1.getName());
    assertThat(policyViolationSummaryDTO1.threatLevel).isEqualTo(5);
    assertThat(policyViolationSummaryDTO1.stageDetails).hasSize(5);
    assertStageDetails(policyViolationSummaryDTO1.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO1.stageDetails.get(1), StageTypes.BUILD, null,
        policyEvaluation1.getScanId(), policyEvaluation1.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO1.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO1.stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO1.stageDetails.get(4), StageTypes.OPERATE, null, null, null);

    PolicyViolationSummaryDTO policyViolationSummaryDTO2 = getPolicyViolationSummaryDTO(policy1.getId(),
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO2).isEqualTo(policyViolationSummaryDTO1);
  }

  @Test
  public void testGetApplicationDetailsByHash_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentDetailService.getApplicationDetailsByHash("some-hash"));
  }

  @Test
  public void testGetApplicationDetailsByHash_MissingPolicy() {
    String hash = "ababababab";

    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy policy = tempEntity.newPolicy(app);
    String policyId = policy.getId();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "groupId", "artifactId", "version", hash, "reason");
    policyDAO.delete(policy);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).isNotNull();
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId()).isEqualTo(app.getId());
    assertThat(appComponentDetailsDTO.policyViolations).isNotNull();
    assertThat(appComponentDetailsDTO.policyViolations).hasSize(1);
    PolicyViolationSummaryDTO policyViolationSummaryDTO = getPolicyViolationSummaryDTO(policyId,
        appComponentDetailsDTO.policyViolations);
    assertThat(policyViolationSummaryDTO).isNotNull();
    assertThat(policyViolationSummaryDTO.policyName).isEqualTo(policy.getName());
    assertThat(policyViolationSummaryDTO.threatLevel).isEqualTo(5);
    assertThat(policyViolationSummaryDTO.stageDetails).hasSize(5);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(1), StageTypes.BUILD, null,
        policyEvaluation.getScanId(), policyEvaluation.getTime().getTime());
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(policyViolationSummaryDTO.stageDetails.get(4), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHash_ExcludesDevelopStage() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), DevelopStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");

    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), DevelopStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy policy2 = tempEntity.newPolicy(app2);
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation2, policy2, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    PolicyEvaluation evaluation3 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation3, policy2, "groupId", "artifactId", "version", hash, "reason1");

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).isNotNull();
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId()).isEqualTo(app2.getId());
    assertThat(dto.stageDetails).hasSize(5);
    assertStageDetails(dto.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(dto.stageDetails.get(1), StageTypes.BUILD, null, "scanId1", evaluation3.getTime().getTime());
    assertStageDetails(dto.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(4), StageTypes.OPERATE, null, null, null);
    assertThat(dto.policyViolations).hasSize(1);
    assertThat(dto.policyViolations.get(0).stageDetails).hasSize(5);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(1), StageTypes.BUILD, null,
        evaluation3.getScanId(), evaluation3.getTime().getTime());
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(4), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHash_FirstViolationOccurrence_LatestReportAndAction() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    OwnerComponent component = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", new Date(
        System.currentTimeMillis() - 1000));
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation1, policy1, policy1.getThreatLevel(),
        policy1.getThreatCategory(), component.getComponentIdentifier(), hash, WarnActionType.ID);

    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2");
    violation.setActionTypeId(FailActionType.ID);
    policyViolationDAO.update(violation);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).isNotNull();
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId()).isEqualTo(app1.getId());
    assertThat(dto.policyViolations).hasSize(1);
    assertThat(dto.policyViolations.get(0).stageDetails).hasSize(5);
    assertStageDetails(dto.policyViolations.get(0).stageDetails.get(1), StageTypes.BUILD, FailActionType.ID,
        evaluation2.getScanId(), evaluation1.getTime().getTime());
  }

  @Test
  public void testGetApplicationDetailsByHash_FirstOccurrenceTimeForAppLevel() {
    String hash = "ababababab";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    OwnerComponent component = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, component.getHash(),
        component.getComponentIdentifier());

    Policy policy1 = tempEntity.newPolicy(app1);
    Policy policy2 = tempEntity.newPolicy(app1);

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(evaluation1, policy1, policy1.getThreatLevel(), policy1.getThreatCategory(),
        component.getComponentIdentifier(), hash, WarnActionType.ID);

    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2", new Date(
        evaluation1.getTime().getTime() + 1000));
    tempEntity.newPolicyViolation(evaluation2, policy1, policy1.getThreatLevel(), policy1.getThreatCategory(),
        component.getComponentIdentifier(), hash, WarnActionType.ID);
    tempEntity.newPolicyViolation(evaluation2, policy2, policy2.getThreatLevel(), policy2.getThreatCategory(),
        component.getComponentIdentifier(), hash, WarnActionType.ID);

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO dto = appComponentDetailsDTOs.get(0);
    assertThat(dto.application.getId()).isEqualTo(app1.getId());
    assertThat(dto.stageDetails).hasSize(5);
    // should show the first occurence time and link to most recent scan report
    assertStageDetails(dto.stageDetails.get(0), StageTypes.SOURCE, null, null, null);
    assertStageDetails(dto.stageDetails.get(1), StageTypes.BUILD, WarnActionType.ID, "scanId2", evaluation1.getTime()
        .getTime());
    assertStageDetails(dto.stageDetails.get(2), StageTypes.STAGE_RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(3), StageTypes.RELEASE, null, null, null);
    assertStageDetails(dto.stageDetails.get(4), StageTypes.OPERATE, null, null, null);
  }

  @Test
  public void testGetApplicationDetailsByHash_ExcludesCostlyContactInfo() {
    String hash = "ababababab";

    Application app = tempEntity.newApplication("appName", "appId", tempEntity.newOrganization().getId(), "admin");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    List<ApplicationComponentDetailsDTO> appComponentDetailsDTOs = componentDetailService
        .getApplicationDetailsByHash(hash);
    assertThat(appComponentDetailsDTOs).hasSize(1);
    ApplicationComponentDetailsDTO appComponentDetailsDTO = appComponentDetailsDTOs.get(0);
    assertThat(appComponentDetailsDTO.application.getId()).isEqualTo(app.getId());
    assertThat(appComponentDetailsDTO.application.getContact()).isNull();
  }

  private void assertStageDetails(
      StageDetailDTO stageDetailDTO,
      StageType stageType,
      String actionType,
      String scanId,
      Long time)
  {
    assertThat(stageDetailDTO.stageTypeId).isEqualTo(stageType.getId());
    assertThat(stageDetailDTO.stageTypeName).isEqualTo(stageType.getName());
    assertThat(stageDetailDTO.actionTypeId).isEqualTo(actionType);
    assertThat(stageDetailDTO.scanId).isEqualTo(scanId);
    assertThat(stageDetailDTO.time).isEqualTo(time);
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1", "version1"));
    // Force different times on the two ApplicationComponents
    Thread.sleep(1);
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId2", "artifactId2", "version2"));

    List<ComponentDisplayNamePart> name = componentDetailService.getComponentNameByHash(hash).parts;
    assertDisplayFieldValuesForGAV(name, "groupId2", "artifactId2", "version2");
  }

  @Test
  public void testGetComponentNameByHash_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> componentDetailService.getComponentNameByHash("some-hash"));
  }

  @Test
  public void testGetComponentNameByHash_UnknownHash() {
    String hash = "ababababab";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentDetailService.getComponentNameByHash(hash))
        .withMessage("Unknown component with hash " + hash + ".");
  }

  @Test
  public void testGetComponentNameByHash_NoGAV() {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* componentIdentifier */,
        "somepath");

    List<ComponentDisplayNamePart> name = componentDetailService.getComponentNameByHash(hash).parts;
    assertThat(name).hasSize(1);
    assertDisplayFieldValue(name.get(0), "Pathname", "somepath");
  }

  @Test
  public void testGetComponentNameByHash_NoGAVOrPathnames() {
    String hash = "ababababab";
    Application app = tempEntity.newApplicationWithParent("app");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, hash, null /* componentIdentifier */);

    assertThat(componentDetailService.getComponentNameByHash(hash)).isNull();
  }

  private PolicyViolationSummaryDTO getPolicyViolationSummaryDTO(
      String policyId,
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
