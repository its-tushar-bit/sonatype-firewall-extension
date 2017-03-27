/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.component.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class NewestRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  private NewestRiskService newestRiskService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplicationWithParent("app2", "app2");
    orgPolicy = tempEntity.newPolicy(org.getParentOrganizationId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
  }

  @Test
  public void testGetNewestRisks_FilterByApplication() throws Exception {
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app2.getId()), null,
        null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByOrganization() throws Exception {
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(Collections.singleton(app2.getParentOwnerId()),
        null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), ReleaseStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 
        1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app2.getId(), DevelopStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app2.getId()), null,
        null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    for (StageDetailDTO stageDetailDTO : riskDTO.stageDetails) {
      assertThat(stageDetailDTO.stageTypeId, is(not(DevelopStageType.ID)));
    }

    try {
      newestRiskService.getNewestRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null, null,
          DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetNewestRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null,
        Collections.singleton(app2Tag.getId()), null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatCategory() throws Exception {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 5,
        PolicyThreatCategory.OTHER, "gid", "aid", "1", "hash1");
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), BuildStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null, null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER), null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD,
        1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatLevel() throws Exception {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 7,
        PolicyThreatCategory.OTHER, "gid", "aid", "1", "hash1");
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), BuildStageType.ID);
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null,
        new PolicyThreatLevelFilter(7, 7), null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTO, app1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    WaivedPolicyViolation waivedViolation = tempEntity
        .newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy, "gid", "aid", "1", "hash1", policyWaiver);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation policyViolation = policyViolationDAO.getById(waivedViolation.getId());
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED), DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, app1PolicyEvaluation.getTime());

    riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN), DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(riskDTOs, hasSize(3));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(1), app1, app1PolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(2), app1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN),
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(riskDTOs, hasSize(4));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(1), app1, app1PolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(2), app1, policyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(3), app1, orgPolicyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_SortAndResultCapping() throws Exception {
    // Limit to high value
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(riskDTOs, hasSize(3));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(1), app1, app1PolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(2), app1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    // Limit to 1
    riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app1PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, app1Policy,
        app1PolicyViolation.getThreatLevel(), app1PolicyViolation.getThreatCategory(),
        app1PolicyViolation.getComponentIdentifier(), app1PolicyViolation.getHash());
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), ReleaseStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null, null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(riskDTOs, hasSize(3));

    NewestRiskDTO riskDTO0 = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO0, app1, policyViolation, policyEvaluation.getTime());
    assertThat(riskDTO0.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO0, BuildStageType.ID, app1PolicyEvaluation.getScanId(),
        app1PolicyViolation.getActionTypeId(), app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTO0, ReleaseStageType.ID, policyEvaluation.getScanId(),
        policyViolation.getActionTypeId(), policyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO0, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO0, OperateStageType.ID);

    NewestRiskDTO riskDTO1 = riskDTOs.get(1);
    assertNewestRiskDTO(riskDTO1, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertThat(riskDTO1.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO1, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, ReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, OperateStageType.ID);

    NewestRiskDTO riskDTO2 = riskDTOs.get(2);
    assertNewestRiskDTO(riskDTO2, app1, orgPolicyViolation, app1PolicyEvaluation.getTime());
    assertThat(riskDTO2.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO2, BuildStageType.ID, app1PolicyEvaluation.getScanId(),
        app1PolicyViolation.getActionTypeId(), app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, ReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, OperateStageType.ID);
  }

  @Test
  public void testGetNewestRisks_NewerThanNDays() throws Exception {
    Integer maxDaysOld = 5;

    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    Date beforeNDays = new DateTime().minusDays(maxDaysOld + 1).toDate();
    String oldScanId = "test old scan id";
    PolicyEvaluation oldPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, oldScanId,
        beforeNDays);
    PolicyViolation oldPolicyViolation = tempEntity.newPolicyViolation(oldPolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(oldPolicyViolation.getId(), app.getId(), BuildStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null,
        null, null, null, null, maxDaysOld, 100);
    assertThat(riskDTOs, hasSize(0));

    Date afterNDays = new DateTime().minusDays(maxDaysOld - 1).toDate();
    String newScanId = "test new scan id";
    PolicyEvaluation newPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, newScanId,
        afterNDays);
    PolicyViolation newPolicyViolation = tempEntity.newPolicyViolation(newPolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(newPolicyViolation.getId(), app.getId(), ReleaseStageType.ID);

    riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
        maxDaysOld, 100);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app, newPolicyViolation, newPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), BuildStageType.ID, oldScanId,
        orgPolicyViolation.getActionTypeId(), oldPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), ReleaseStageType.ID, newScanId,
        newPolicyViolation.getActionTypeId(), newPolicyEvaluation.getTime());

    // check 1 since its the lowest value that should work without throwing an exception
    maxDaysOld = 1;
    riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, 
        maxDaysOld, 100);
    assertThat(riskDTOs, hasSize(0));
  }

  @Test
  public void testGetNewestRisks_NoTimeLimit() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    Date oldDate = new Date(0);
    String oldScanId = "test old scan id";
    PolicyEvaluation oldPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, oldScanId,
        oldDate);
    PolicyViolation oldPolicyViolation = tempEntity.newPolicyViolation(oldPolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(oldPolicyViolation.getId(), app.getId(), BuildStageType.ID);

    // first run a query that has a number of days small enough that it should not include this scan
    Integer maxDaysOld = 1000;
    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null,
        null, null, null, null, maxDaysOld, 100);
    assertThat(riskDTOs, hasSize(0));

    // then run a query with no date limit
    maxDaysOld = null;
    riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
        maxDaysOld, 100);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app, oldPolicyViolation, oldPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), BuildStageType.ID, oldScanId,
        orgPolicyViolation.getActionTypeId(), oldPolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_InvalidTimeLimit() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    Integer maxDaysOld;
    try {
      maxDaysOld = -50;
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
          maxDaysOld, 100);
      fail("Expected IllegalArgumentException when maxDaysOld is negative");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Max Days Old must be a positive integer"));
    }

    try {
      maxDaysOld = 0;
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
          maxDaysOld, 100);
      fail("Expected IllegalArgumentException when maxDaysOld is zero");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Max Days Old must be a positive integer"));
    }
  }

  @Test
  public void testGetNewestRisks_LastViolationNotFirstOccurrence() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    DateTime time1 = new DateTime().minusDays(1);
    String scanId1 = "scanId1";
    PolicyEvaluation policyEval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1,
        time1.toDate());
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEval1, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation1.getId(), app.getId(), BuildStageType.ID);

    DateTime time2 = time1.plusHours(1);
    String scanId2 = "scanId2";
    PolicyEvaluation policyEval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId2,
        time2.toDate());
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEval2, orgPolicy);

    DateTime time3 = time2.plusHours(1);
    String scanId3 = "scanId3";
    PolicyEvaluation policyEval3 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId3,
        time3.toDate());
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEval3, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation3.getId(), app.getId(), ReleaseStageType.ID);

    DateTime time4 = time3.plusHours(1);
    String scanId4 = "scanId4";
    PolicyEvaluation policyEval4 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId4,
        time4.toDate());
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEval4, orgPolicy);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null,
        null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app, policyViolation1, policyEval3.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), BuildStageType.ID, scanId2,
        policyViolation2.getActionTypeId(), policyEval1.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), ReleaseStageType.ID, scanId4,
        policyViolation4.getActionTypeId(), policyEval3.getTime());
  }

  @Test
  public void testGetNewestRisks_ViolationWithoutHash() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy, "g", "a", "v",
        null /* hash */, "reason");
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), ReleaseStageType.ID);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_ViolationWithoutFirstOccurrence() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<NewestRiskDTO> riskDTOs = newestRiskService.getNewestRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, evaluation.getTime());
  }

  private void assertNewestRiskDTO(NewestRiskDTO actual, Application app, PolicyViolation policyViolation, Date time) {
    assertThat(actual.applicationName, is(app.getName()));
    assertThat(actual.applicationPublicId, is(app.getPublicId()));
    assertThat(actual.threatLevel, is(policyViolation.getThreatLevel()));
    assertThat(actual.time, is(time.getTime()));
    assertThat(actual.policyName, is(policyViolation.getPolicyName()));
    assertThat(actual.policyId, is(policyViolation.getPolicyId()));
    assertThat(actual.hash, is(policyViolation.getHash()));
    if (policyViolation.getComponentIdentifier() != null) {
      assertDisplayFieldValues(actual.displayName.parts, policyViolation);
    }
    else {
      assertThat(actual.displayName, is(nullValue()));
    }
    assertThat(actual.pathnames, is(policyViolation.getPathnames()));
  }

  private void assertNewestRiskDTOContainsStageDetails(NewestRiskDTO actual,
                                                       String stageTypeId,
                                                       String scanId,
                                                       String actionTypeId,
                                                       Date time)
  {
    for (StageDetailDTO stageDetailDTO : actual.stageDetails) {
      if (stageTypeId.equals(stageDetailDTO.stageTypeId)) {
        assertThat(stageDetailDTO.actionTypeId, is(actionTypeId));
        assertThat(stageDetailDTO.time, is(time.getTime()));
        assertThat(stageDetailDTO.scanId, is(scanId));
        return;
      }
    }
    fail("NewestRiskDTO does not contain details for stage " + stageTypeId);
  }

  private void assertNewestRiskDTOContainsEmptyStageDetails(NewestRiskDTO actual, String stageTypeId) {
    for (StageDetailDTO stageDetailDTO : actual.stageDetails) {
      if (stageTypeId.equals(stageDetailDTO.stageTypeId)) {
        assertThat(stageDetailDTO.actionTypeId, nullValue());
        assertThat(stageDetailDTO.time, is(nullValue()));
        assertThat(stageDetailDTO.scanId, nullValue());
        return;
      }
    }
    fail("NewestRiskDTO does not contain details for stage " + stageTypeId);
  }
}
