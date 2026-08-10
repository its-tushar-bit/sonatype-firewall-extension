/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

@ComponentH2Test
public class ApiCrossStageViolationServiceTest
    extends AbstractComponentH2Test
{
  private Date baseDate;

  private Organization org;

  private Organization policyOwnerOrg;

  private Application app;

  private Application app2;

  private Policy policy;

  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createNpmCoordinates("foo", "1.0.0");

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApiCrossStageViolationService service;

  @BeforeEach
  public void setup() {
    baseDate = new Date();
    org = tempEntity.newOrganization();
    policyOwnerOrg = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    app2 = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy(policyOwnerOrg.getId(), "p1", 7);

  }

  @Test
  public void testGetCrossStageViolationById() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    violation1.setActionTypeId("fail");
    violation1.setFilename("foo.js");
    ConstraintFact constraintFact = violation1.getConstraintFacts().get(0);
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    violation2.setActionTypeId("warn");
    violation2.setFilename("foo.js");
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation3.setFilename("foo.js");
    policyViolationDAO.update(violation3);

    // equivalent, different app
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent (different constraint)
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan5",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval5, policy, COMPONENT_IDENTIFIER, "1234", "vuln2");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.policyViolationId).isEqualTo(violation1.getId());
    assertThat(result.policyId).isEqualTo(policy.getId());
    assertThat(result.policyName).isEqualTo(policy.getName());
    assertThat(result.threatLevel).isEqualTo(policy.getThreatLevel());
    assertThat(result.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(result.applicationName).isEqualTo(app.getName());
    assertThat(result.organizationName).isEqualTo(org.getName());
    assertThat(result.openTime).isEqualTo(baseDate);
    assertThat(result.fixTime).isNull();
    assertThat(result.hash).isEqualTo(violation1.getHash());
    assertThat(result.policyThreatCategory).isEqualTo("security");
    assertThat(result.displayName.toString()).isEqualTo("foo : 1.0.0");
    assertThat(result.filename).isEqualTo("foo.js");
    assertThat(result.stageData).hasSize(3);
    assertCrossStageData(result, Stage.ID_BUILD, baseDate, "scan1", "fail");
    assertCrossStageData(result, Stage.ID_RELEASE, new Date(baseDate.getTime() + 2), "scan2", "warn");
    assertCrossStageData(result, Stage.ID_STAGE_RELEASE, new Date(baseDate.getTime() + 4), "scan3", null);

    assertThat(result.constraintViolations).extracting(dto -> dto.constraintId, dto -> dto.constraintName)
        .containsExactly(tuple(constraintFact.getConstraintId(), constraintFact.getConstraintName()));

    List<ApiConstraintViolationReasonDTO> violationReasons = result.constraintViolations.get(0).reasons;
    assertThat(violationReasons).hasSize(1);
    assertThat(violationReasons.get(0).reference.value).isEqualTo("vuln1");
    assertThat(violationReasons.get(0).reference.type).isEqualTo("SECURITY_VULNERABILITY_REFID");
    assertThat(violationReasons.get(0).reason).isEqualTo("vuln1");

    assertThat(result.policyOwner.ownerId).isEqualTo(policyOwnerOrg.getId());
    assertThat(result.policyOwner.ownerPublicId).isNull();
    assertThat(result.policyOwner.ownerName).isEqualTo(policyOwnerOrg.getName());
    assertThat(result.policyOwner.ownerType).isEqualTo("organization");

    assertThat(result.componentIdentifier.getFormat()).isEqualTo(COMPONENT_IDENTIFIER.getFormat());
    assertThat(result.componentIdentifier.getCoordinates().keySet().toArray())
        .containsExactly(COMPONENT_IDENTIFIER.getCoordinates().keySet().toArray());
    assertThat(result.componentIdentifier.getCoordinates().values().toArray())
        .containsExactly(COMPONENT_IDENTIFIER.getCoordinates().values().toArray());
  }

  @Test
  public void testGetCrossStageViolationById_WithFixTime() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation1);

    // equivalent, but opened after all relevant violations were closed. Inserted into db before eval2 to test
    // that eval2 is still correctly picked up
    PolicyEvaluation eval6 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, "scan6",
        new Date(baseDate.getTime() + 6));
    tempEntity.newPolicyViolation(eval6, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // equivalent, different stage, opened before violation1 is fixed
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation3.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation3);

    // equivalent, different app
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent (different constraint)
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan5",
        new Date(baseDate.getTime() + 4));
    tempEntity.newPolicyViolation(eval5, policy, COMPONENT_IDENTIFIER, "1234", "vuln2");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime.getTime()).isEqualTo(baseDate.getTime() + 5);
  }

  // this tests the case where the first violation isn't fixed until after the second violation is fixed
  @Test
  public void testGetCrossStageViolationById_ViolationsClosedAfterOthersOpenedAndClosed() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 4));
    policyViolationDAO.update(violation2);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime.getTime()).isEqualTo(baseDate.getTime() + 5);
  }

  @Test
  public void testGetCrossStageViolationById_WithWaivers() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    violation2.setWaiveTime(new Date(baseDate.getTime() + 3));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation3.setFixTime(new Date(baseDate.getTime() + 3));
    violation3.setWaiveTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation3);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime.getTime()).isEqualTo(baseDate.getTime() + 3);
  }

  @Test
  public void testGetCrossStageViolationById_IdNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.getCrossStageViolationById("foo"));
  }

  @Test
  public void testGetCrossStageViolationById_IdOfNonFirstViolation() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getCrossStageViolationById(violation2.getId()));
  }

  @Test
  public void testGetCrossStageViolationById_ApplicationPolicyOwner() {
    Application policyOwnerApp = tempEntity.newApplication("public-foo", org.getId());
    Policy policy = tempEntity.newPolicy(policyOwnerApp.getId(), "p1", 7);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.policyOwner.ownerId).isEqualTo(policyOwnerApp.getId());
    assertThat(result.policyOwner.ownerPublicId).isEqualTo("public-foo");
    assertThat(result.policyOwner.ownerName).isEqualTo(policyOwnerApp.getName());
    assertThat(result.policyOwner.ownerType).isEqualTo("application");
  }

  @Test
  public void testGetCrossStageViolationById_ApplicationPolicyOwner_PolicyNoLongerExists() {
    Application policyOwnerApp = tempEntity.newApplication("public-foo", org.getId());
    Policy policy = tempEntity.newPolicy(policyOwnerApp.getId(), "p1", 7);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    policyDAO.delete(policy);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.policyOwner).isNotNull();
    assertThat(result.policyOwner.ownerId).isNull();
    assertThat(result.policyOwner.ownerPublicId).isNull();
    assertThat(result.policyOwner.ownerName).isNull();
    assertThat(result.policyOwner.ownerType).isNull();
  }

  @Test
  public void testGetCrossStageViolationByConstituentId() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    violation1.setFilename("foo.js");
    policyViolationDAO.update(violation1);

    // equivalent, different stage
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation2);

    // equivalent, different stage, opened after violation1 is fixed but before violation2 is fixed
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent - same app
    PolicyViolation violation4 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1235", "vuln2");

    // equivalent, different app
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation5 = tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent (different constraint and different app)
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scan6",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation6 = tempEntity.newPolicyViolation(eval5, policy, COMPONENT_IDENTIFIER, "1235", "vuln2");

    // For the initial violation it should return its id
    ApiCrossStageViolationDTOV2 crossViolation1 = service.getCrossStageViolationByConstituentId(violation1.getId());
    assertThat(crossViolation1.policyViolationId).isEqualTo(violation1.getId());
    assertThat(crossViolation1.openTime).isEqualTo(baseDate);
    assertThat(crossViolation1.fixTime).isNull();
    assertThat(crossViolation1.policyId).isEqualTo(policy.getId());
    assertThat(crossViolation1.stageData).hasSize(3);
    assertCrossStageData(crossViolation1, Stage.ID_BUILD, baseDate, "scan1", null);
    assertCrossStageData(crossViolation1, Stage.ID_RELEASE, violation2.getOpenTime(), "scan2", null);
    assertCrossStageData(crossViolation1, Stage.ID_STAGE_RELEASE, violation3.getOpenTime(), "scan3", null);
    assertThat(crossViolation1.componentIdentifier.getFormat()).isEqualTo(COMPONENT_IDENTIFIER.getFormat());
    assertThat(crossViolation1.componentIdentifier.getCoordinates().keySet().toArray())
        .containsExactly(COMPONENT_IDENTIFIER.getCoordinates().keySet().toArray());
    assertThat(crossViolation1.componentIdentifier.getCoordinates().values().toArray())
        .containsExactly(COMPONENT_IDENTIFIER.getCoordinates().values().toArray());

    // For an equivalent violation in a different stage it should return the earliest id
    ApiCrossStageViolationDTOV2 crossViolation2 = service.getCrossStageViolationByConstituentId(violation2.getId());
    assertThat(crossViolation2.policyViolationId).isEqualTo(violation1.getId());
    assertThat(crossViolation2.stageData).hasSize(3);
    assertCrossStageData(crossViolation2, Stage.ID_BUILD, baseDate, "scan1", null);
    assertCrossStageData(crossViolation2, Stage.ID_RELEASE, violation2.getOpenTime(), "scan2", null);
    assertCrossStageData(crossViolation2, Stage.ID_STAGE_RELEASE, violation3.getOpenTime(), "scan3", null);

    ApiCrossStageViolationDTOV2 crossViolation3 = service.getCrossStageViolationByConstituentId(violation3.getId());
    assertThat(crossViolation3.policyViolationId).isEqualTo(violation1.getId());
    assertThat(crossViolation3.stageData).hasSize(3);
    assertCrossStageData(crossViolation3, Stage.ID_BUILD, baseDate, "scan1", null);
    assertCrossStageData(crossViolation3, Stage.ID_RELEASE, violation2.getOpenTime(), "scan2", null);
    assertCrossStageData(crossViolation3, Stage.ID_STAGE_RELEASE, violation3.getOpenTime(), "scan3", null);

    // For a single-stage violation its crossStageViolationId is its own id.
    ApiCrossStageViolationDTOV2 crossViolation4 = service.getCrossStageViolationByConstituentId(violation4.getId());
    assertThat(crossViolation4.policyViolationId).isEqualTo(violation4.getId());
    assertThat(crossViolation4.stageData).hasSize(1);
    assertCrossStageData(crossViolation4, Stage.ID_STAGE_RELEASE, violation4.getOpenTime(), "scan3", null);

    ApiCrossStageViolationDTOV2 crossViolation5 = service.getCrossStageViolationByConstituentId(violation5.getId());
    assertThat(crossViolation5.policyViolationId).isEqualTo(violation5.getId());
    assertThat(crossViolation5.stageData).hasSize(1);
    assertCrossStageData(crossViolation5, Stage.ID_OPERATE, violation5.getOpenTime(), "scan4", null);

    ApiCrossStageViolationDTOV2 crossViolation6 = service.getCrossStageViolationByConstituentId(violation6.getId());
    assertThat(crossViolation6.policyViolationId).isEqualTo(violation6.getId());
    assertThat(crossViolation6.stageData).hasSize(1);
    assertCrossStageData(crossViolation6, Stage.ID_BUILD, violation6.getOpenTime(), "scan6", null);
  }

  @Test
  public void testGetCrossStageViolationByConstituentId_SingleStages() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent - same app
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan2",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1235", "vuln2");

    // equivalent, different app
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // not equivalent (different constraint and different app)
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation4 = tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1235", "vuln2");

    // For a single-stage violation its crossStageViolationId is its own id.
    ApiCrossStageViolationDTOV2 crossViolation1 = service.getCrossStageViolationByConstituentId(violation1.getId());
    assertThat(crossViolation1.policyViolationId).isEqualTo(violation1.getId());
    ApiCrossStageViolationDTOV2 crossViolation2 = service.getCrossStageViolationByConstituentId(violation2.getId());
    assertThat(crossViolation2.policyViolationId).isEqualTo(violation2.getId());
    ApiCrossStageViolationDTOV2 crossViolation3 = service.getCrossStageViolationByConstituentId(violation3.getId());
    assertThat(crossViolation3.policyViolationId).isEqualTo(violation3.getId());
    ApiCrossStageViolationDTOV2 crossViolation4 = service.getCrossStageViolationByConstituentId(violation4.getId());
    assertThat(crossViolation4.policyViolationId).isEqualTo(violation4.getId());
  }

  @Test
  public void testGetCrossStageViolationByConstituentId_WithClosedEquivalentEarlierViolation() {
    // A violation, closed.
    PolicyEvaluation eval1 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 1));
    policyViolationDAO.update(violation1);

    // Equivalent, same stage. Opened after violation1 was closed.
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation2.setFixTime(new Date(baseDate.getTime() + 5));
    policyViolationDAO.update(violation2);

    // Equivalent, different stage. Opened while violation2 was open.
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan3",
        new Date(baseDate.getTime() + 3));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation3.setFixTime(new Date(baseDate.getTime() + 7));
    policyViolationDAO.update(violation3);

    // Equivalent, different stage, opened after violation2 is fixed but before violation3 is fixed
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan4",
        new Date(baseDate.getTime() + 6));
    PolicyViolation violation4 = tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // If the given id is the id of the earliest violation then the calculated id should be the same
    ApiCrossStageViolationDTOV2 crossViolation1 = service.getCrossStageViolationByConstituentId(violation1.getId());
    assertThat(crossViolation1.policyViolationId).isEqualTo(violation1.getId());
    ApiCrossStageViolationDTOV2 crossViolation2 = service.getCrossStageViolationByConstituentId(violation2.getId());
    assertThat(crossViolation2.policyViolationId).isEqualTo(violation2.getId());

    // For an equivalent violation in a different stage it should return the earliest id
    ApiCrossStageViolationDTOV2 crossViolation3 = service.getCrossStageViolationByConstituentId(violation3.getId());
    assertThat(crossViolation3.policyViolationId).isEqualTo(violation2.getId());

    ApiCrossStageViolationDTOV2 crossViolation4 = service.getCrossStageViolationByConstituentId(violation4.getId());
    assertThat(crossViolation4.policyViolationId).isEqualTo(violation2.getId());
  }

  @Test
  public void testGetCrossStageViolationByConstituentId_WithOpenEquivalentEarlierViolation() {
    // An earlier, closed violation.
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 1));
    policyViolationDAO.update(violation1);

    // First violation, unfixed
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // Equivalent, different stage, unfixed.
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scan2",
        new Date(baseDate.getTime() + 3));
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // Equivalent, different stage, unfixed.
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan3",
        new Date(baseDate.getTime() + 4));
    PolicyViolation violation4 = tempEntity.newPolicyViolation(eval4, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 crossViolation1 = service.getCrossStageViolationByConstituentId(violation1.getId());
    assertThat(crossViolation1.policyViolationId).isEqualTo(violation1.getId());

    ApiCrossStageViolationDTOV2 crossViolation2 = service.getCrossStageViolationByConstituentId(violation2.getId());
    assertThat(crossViolation2.policyViolationId).isEqualTo(violation2.getId());

    ApiCrossStageViolationDTOV2 crossViolation3 = service.getCrossStageViolationByConstituentId(violation3.getId());
    assertThat(crossViolation3.policyViolationId).isEqualTo(violation2.getId());

    ApiCrossStageViolationDTOV2 crossViolation4 = service.getCrossStageViolationByConstituentId(violation4.getId());
    assertThat(crossViolation4.policyViolationId).isEqualTo(violation2.getId());
  }

  @Test
  public void testGetCrossStageViolationByConstituentId_IdNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getCrossStageViolationByConstituentId("foo"));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getCrossStageViolationByConstituentId(null));
  }

  @Test
  public void testGetCrossStageViolationById_UnwaivedViolation() {
    // A waived violation
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation buildStageViolation1 =
        tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    buildStageViolation1.setWaiveTime(new Date(baseDate.getTime() + 3));

    // equivalent, different stage, opened before violation1 is fixed
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2",
        new Date(baseDate.getTime() + 2));
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    // The un-waived version of buildStageViolation1
    PolicyEvaluation eval3 =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan3", new Date(baseDate.getTime() + 6));
    PolicyViolation buildStageViolation2 =
        tempEntity.newPolicyViolation(eval3, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    buildStageViolation1.setFixTime(new Date(baseDate.getTime() + 6));
    policyViolationDAO.update(buildStageViolation1);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationByConstituentId(buildStageViolation1.getId());
    // The waive was rescinded, thus the violation isn't fixed.
    assertThat(result.fixTime).isNull();
    assertThat(result.policyViolationId).isEqualTo(buildStageViolation1.getId());
    assertCrossStageData(result, Stage.ID_BUILD, eval3.getTime(), "scan3", null);
    assertCrossStageData(result, Stage.ID_RELEASE, eval2.getTime(), "scan2", null);

    ApiCrossStageViolationDTOV2 result2 = service.getCrossStageViolationByConstituentId(violation2.getId());
    assertThat(result2.fixTime).isNull();
    assertThat(result2.policyViolationId).isEqualTo(buildStageViolation1.getId());
    assertCrossStageData(result2, Stage.ID_RELEASE, eval2.getTime(), "scan2", null);
    assertCrossStageData(result2, Stage.ID_BUILD, eval3.getTime(), "scan3", null);

    ApiCrossStageViolationDTOV2 result3 = service.getCrossStageViolationByConstituentId(buildStageViolation2.getId());
    assertThat(result3.fixTime).isNull();
    assertThat(result3.policyViolationId).isEqualTo(buildStageViolation1.getId());
    assertCrossStageData(result3, Stage.ID_BUILD, eval3.getTime(), "scan3", null);
    assertCrossStageData(result3, Stage.ID_RELEASE, eval2.getTime(), "scan2", null);
  }

  @Test
  public void testGetCrossStageViolationById_WithAutoWaivers() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setFixTime(new Date(baseDate.getTime() + 3));
    violation1.setWaiveTime(new Date(baseDate.getTime() + 3));
    violation1.setAutoPolicyWaiverId("waiver1");
    policyViolationDAO.update(violation1);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime.getTime()).isEqualTo(baseDate.getTime() + 3);
    assertCrossStageData(result, Stage.ID_BUILD, baseDate, "scan1", null);
  }

  @Test
  public void testGetCrossStageViolationById_OutsideOfLatestViolationDateRange() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    violation1.setOpenTime(new Date(baseDate.getTime() + 2000));
    violation1.setFixTime(new Date(baseDate.getTime() + 3000));
    policyViolationDAO.update(violation1);

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.fixTime.getTime()).isEqualTo(baseDate.getTime() + 3000);

    assertThat(result.stageData).isEmpty();
  }

  /**
   * This test verifies that a waived violation for a component as soon as it appears
   * behaves correctly and does not cause an NPE due to the pre-existing matching waiver.
   */
  @Test
  public void testGetCrossStageViolationByConstituentId_PreExistingWaivedViolation() {
    // waived violation as soon as it appears
    Date time1 = new Date(baseDate.getTime());
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", time1);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    PolicyWaiver waiver1 = tempEntity.newWaiver(policy.getId(), policy.getOwnerId());

    violation1.setOwnerId(app.getId());
    violation1.setPolicyWaiverId(waiver1.getId());
    violation1.setOpenTime(time1);
    violation1.setWaiveTime(time1);
    policyViolationDAO.update(violation1);

    ApiCrossStageViolationDTOV2 result1 = service.getCrossStageViolationByConstituentId(violation1.getId());
    assertThat(result1.openTime).isEqualTo(time1);
    assertThat(result1.fixTime).isEqualTo(time1);
    assertThat(result1.policyViolationId).isEqualTo(violation1.getId());
    assertThat(result1.stageData).doesNotContainKey(Stage.ID_BUILD);
  }

  @Test
  public void testGetReachabilityStatus() {
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");
    policyViolationDAO.update(violation1);
    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.reachabilityStatus).isNull();

    violation1.setReachabilityStatus(ReachabilityStatus.REACHABLE);
    policyViolationDAO.update(violation1);
    result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.reachabilityStatus).isEqualTo(ReachabilityStatus.REACHABLE);

    violation1.setReachabilityStatus(ReachabilityStatus.NON_REACHABLE);
    policyViolationDAO.update(violation1);
    result = service.getCrossStageViolationById(violation1.getId());
    assertThat(result.reachabilityStatus).isEqualTo(ReachabilityStatus.NON_REACHABLE);
  }

  /**
   * CLM-40943 — for an archive-of-archives upload (e.g. {@code outer.zip} containing
   * {@code inner.jar}), the evaluator persists inner-pathname {@code ProxyRepositoryPolicyViolation}
   * rows like {@code outer.zip!/inner.jar} but the synthetic {@code Application} is created
   * ONLY for the outer pathname. The DTO builder must strip the {@code "!/..."} suffix and
   * resolve to the outer's synthetic app, instead of failing with a 404.
   */
  @Test
  public void testCreateDtoFromRepositoryViolation_innerArchivePathname_resolvesToOuterApp() {
    com.sonatype.insight.brain.model.repository.Repository repository =
        tempEntity.newRepository("rm1", "r1", "maven2");
    String outerPathname = "outer.zip";
    String innerPathname = outerPathname + "!/log4j-core-2.14.1.jar";

    // Create synthetic Application keyed on the OUTER pathname only (matches the consumer's
    // post-fan-out cleanup pattern: inner proxy_repository_component rows are deleted, but inner
    // proxy_repository_policy_violation rows survive and reference the outer Application).
    String outerAppPublicId =
        com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService
            .generatePublicId(repository.getPublicId(), outerPathname);
    Application outerSyntheticApp = tempEntity.newApplication(
        "synthetic-app-" + outerPathname, outerAppPublicId, org.getId());

    // Persist an inner-pathname violation. The DAO under test must resolve via the outer app.
    com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation innerViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), innerPathname);

    ApiCrossStageViolationDTOV2 result =
        service.getCrossStageViolationByConstituentId(innerViolation.getId());

    assertThat(result.applicationPublicId).isEqualTo(outerSyntheticApp.getPublicId());
    assertThat(result.policyViolationId).isEqualTo(innerViolation.getId());
  }

  /**
   * CLM-40943 — for an outer-pathname violation (no {@code "!/"} marker), behavior is
   * unchanged from pre-fan-out single-component scans: resolve directly to the outer's
   * synthetic app.
   */
  @Test
  public void testCreateDtoFromRepositoryViolation_outerPathname_resolvesUnchanged() {
    com.sonatype.insight.brain.model.repository.Repository repository =
        tempEntity.newRepository("rm2", "r2", "maven2");
    String outerPathname = "plain-jar-no-inner.jar";

    String outerAppPublicId =
        com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService
            .generatePublicId(repository.getPublicId(), outerPathname);
    Application outerSyntheticApp = tempEntity.newApplication(
        "synthetic-app-" + outerPathname, outerAppPublicId, org.getId());

    com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation outerViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), outerPathname);

    ApiCrossStageViolationDTOV2 result =
        service.getCrossStageViolationByConstituentId(outerViolation.getId());

    assertThat(result.applicationPublicId).isEqualTo(outerSyntheticApp.getPublicId());
    assertThat(result.policyViolationId).isEqualTo(outerViolation.getId());
  }

  private void assertCrossStageData(
      ApiCrossStageViolationDTOV2 result,
      String stageId,
      Date expectedEvaluationTime,
      String expectedScanId,
      String expectedActionTypeId)
  {
    assertThat(result.stageData).hasEntrySatisfying(stageId, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(expectedEvaluationTime);
      assertThat(stageData.mostRecentScanId).isEqualTo(expectedScanId);
      assertThat(stageData.actionTypeId).isEqualTo(expectedActionTypeId);
    });
  }
}
