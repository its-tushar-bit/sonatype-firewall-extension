/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiCrossStageViolationServiceTest
    extends AbstractComponentTest
{
  private Date baseDate;

  private Organization org;

  private Organization policyOwnerOrg;

  private Application app;

  private Application app2;

  private Policy policy;

  private PolicyViolationDAO policyViolationDAO;

  private ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foo", "1.0.0");

  @Inject
  private ApiCrossStageViolationService service;

  @Before
  public void setup() {
    baseDate = new Date();
    org = tempEntity.newOrganization();
    policyOwnerOrg = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    app2 = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(policyOwnerOrg.getId(), "p1", 7);
    policyViolationDAO = new PolicyViolationDAO();
  }

  @Test
  public void testGetCrossStageViolationById() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    ConstraintFact constraintFact = violation1.getConstraintFacts().get(0);
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, componentIdentifier, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval3, policy, componentIdentifier, "1234", "vuln1");

    // equivalent, different app
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval4, policy, componentIdentifier, "1234", "vuln1");

    // not equivalent (different constraint)
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan5",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval5, policy, componentIdentifier, "1234", "vuln2");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.policyViolationId).isEqualTo(violation1.getId());
    assertThat(result.policyId).isEqualTo(policy.getId());
    assertThat(result.policyName).isEqualTo(policy.getName());
    assertThat(result.threatLevel).isEqualTo(policy.getThreatLevel());
    assertThat(result.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(result.applicationName).isEqualTo(app.getName());
    assertThat(result.organizationName).isEqualTo(org.getName());
    assertThat(result.openTime).isEqualTo(baseDate.getTime());
    assertThat(result.fixTime).isNull();
    assertThat(result.hash).isEqualTo(violation1.getHash());
    assertThat(result.policyThreatCategory).isEqualTo("security");
    assertThat(result.displayName.toString()).isEqualTo("foo : 1.0.0");
    assertThat(result.stageData).hasSize(3);
    assertThat(result.stageData.get(Stage.ID_BUILD)).extracting("mostRecentEvaluationTime", "mostRecentScanId")
        .containsExactly(baseDate.getTime(), "scan1");
    assertThat(result.stageData.get(Stage.ID_RELEASE)).extracting("mostRecentEvaluationTime", "mostRecentScanId")
        .containsExactly(baseDate.getTime() + 2, "scan2");
    assertThat(result.stageData.get(Stage.ID_STAGE_RELEASE)).extracting("mostRecentEvaluationTime", "mostRecentScanId")
        .containsExactly(baseDate.getTime() + 4, "scan3");

    assertThat(result.constraintViolations).hasSize(1);
    assertThat(result.constraintViolations.get(0)).extracting("constraintId", "constraintName")
        .containsExactly(constraintFact.getConstraintId(), constraintFact.getConstraintName());

    assertThat(result.constraintViolations.get(0).reasons).hasSize(1);
    assertThat(result.constraintViolations.get(0).reasons.get(0).reason).isEqualTo("vuln1");

    assertThat(result.policyOwner.ownerId).isEqualTo(policyOwnerOrg.getId());
    assertThat(result.policyOwner.ownerPublicId).isNull();
    assertThat(result.policyOwner.ownerName).isEqualTo(policyOwnerOrg.getName());
    assertThat(result.policyOwner.ownerType).isEqualTo("organization");
  }

  @Test
  public void testGetCrossStageViolationById_WithFixTime() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation1);

    // equivalent, but opened after all relevant violations were closed.  Inserted into db before eval2 to test
    // that eval2 is still correctly picked up
    PolicyEvaluation eval6 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, "scan6",
        new Date(baseDate.getTime() + 6));
    tempEntity.newPolicyViolation(eval6, policy, componentIdentifier, "1234", "vuln1");

    // equivalent, different stage, opened before violation1 is fixed
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, componentIdentifier, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, componentIdentifier, "1234", "vuln1");
    violation3.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation3);

    // equivalent, different app
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval4, policy, componentIdentifier, "1234", "vuln1");

    // not equivalent (different constraint)
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan5",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval5, policy, componentIdentifier, "1234", "vuln2");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime).isEqualTo(baseDate.getTime() + 5);
  }

  // this tests the case where the first violation isn't fixed until after the second violation is fixed
  @Test
  public void testGetCrossStageViolationById_ViolationsClosedAfterOthersOpenedAndClosed() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, componentIdentifier, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 4));
    policyViolationDAO.update(violation2);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime).isEqualTo(baseDate.getTime() + 5);
  }

  @Test
  public void testGetCrossStageViolationById_WithWaivers() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, componentIdentifier, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    violation2.setWaiveTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, componentIdentifier, "1234", "vuln1");
    violation3.setFixTime(new Date(baseDate.getTime() + 3));
    violation3.setWaiveTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation3);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime).isEqualTo(baseDate.getTime() + 3);
  }

  @Test
  public void testGetCrossStageViolationById_IdNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getCrossStageViolationById("foo");
    });
  }

  @Test
  public void testGetCrossStageViolationById_IdOfNonFirstViolation() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, componentIdentifier, "1234", "vuln1");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getCrossStageViolationById(violation2.getId());
    });
  }

  @Test
  public void testGetCrossStageViolationById_ApplicationPolicyOwner() {
    Application policyOwnerApp = tempEntity.newApplication("public-foo", org.getId());
    Policy policy = tempEntity.newPolicy(policyOwnerApp.getId(), "p1", 7);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, componentIdentifier, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.policyOwner.ownerId).isEqualTo(policyOwnerApp.getId());
    assertThat(result.policyOwner.ownerPublicId).isEqualTo("public-foo");
    assertThat(result.policyOwner.ownerName).isEqualTo(policyOwnerApp.getName());
    assertThat(result.policyOwner.ownerType).isEqualTo("application");
  }
}
