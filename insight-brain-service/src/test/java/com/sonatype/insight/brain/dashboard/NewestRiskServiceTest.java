/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
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

  private Organization org1;

  private Organization org2;
  private Application app1;
  private Application app2;
  private Policy org1Policy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;

  @Before
  public void setup() {
    org1 = tempEntity.newOrganization("org1");
    org2 = tempEntity.newOrganization("org2");
    app1 = tempEntity.newApplication("app1", "app1", org1.getId());
    app2 = tempEntity.newApplication("app2", "app2", org2.getId());
    org1Policy = tempEntity.newPolicy(org1.getParentOrganizationId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, org1Policy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, org1Policy);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
  }

  @Test
  public void testGetNewestRisks_FilterByApplication() throws Exception {
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByOrganization() throws Exception {
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(Collections.singleton(app2.getParentOwnerId()),
            null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, null, 
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "newScanId");
    tempEntity.newPolicyViolation(evaluation, org1Policy);

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null, 
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertThat(riskDTO.stageTypeId, is(not(DevelopStageType.ID)));

    try {
      newestRiskService.getNewestRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null, null,
          null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetNewestRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org1.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, Collections.singleton(app2Tag.getId()), null, null, null, null, 
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatCategory() throws Exception {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, "gid", "aid", "1",
        "hash1");

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null, null, null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY), null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatLevel() throws Exception {
    Policy app1Policy1 = tempEntity.newPolicy(app1.getId(), "app owned policy1", 7);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy1, "gid", "aid",
        "1", "hash1");
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null, null, null, null,
        new PolicyThreatLevelFilter(7, 7), null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(result.dashboardResults, hasSize(1));
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, new PolicyViolationStateFilter(PolicyViolationState.WAIVED),
            null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, waivedViolation, app1PolicyEvaluation.getTime());

    Policy app1GrandfatherPolicy = tempEntity.newPolicy(app1.getId(), "policy Grandfather", 1);
    PolicyViolation grandfatherViolation = tempEntity
        .newGrandfatheredPolicyViolation(app1PolicyEvaluation, app1GrandfatherPolicy,
            ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    result = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.GRANDFATHERED), null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, grandfatherViolation, app1PolicyEvaluation.getTime());

    result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, new PolicyViolationStateFilter(PolicyViolationState.OPEN),
            "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults, hasSize(3));
    assertThat(result.numResults, is(3));
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    result = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.GRANDFATHERED,
            PolicyViolationState.OPEN), "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults, hasSize(5));
    assertThat(result.numResults, is(5));
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(2), app1, org1, waivedViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(3), app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(4), app1, org1, grandfatherViolation,
        app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_SortAndResultCapping() throws Exception {
    // Limit to high value
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL", 
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults, hasSize(3));
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    // Limit to 1
    result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL", 
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(3));
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app1PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, app1Policy,
        app1PolicyViolation.getThreatLevel(), app1PolicyViolation.getThreatCategory(),
        app1PolicyViolation.getComponentIdentifier(), app1PolicyViolation.getHash());

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL", 
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults, hasSize(3));
    assertThat(result.numResults, is(3));

    NewestRiskDTO riskDTO0 = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO0, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO0, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());

    NewestRiskDTO riskDTO1 = result.dashboardResults.get(1);
    assertNewestRiskDTO(riskDTO1, app1, org1, policyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO1, ReleaseStageType.ID, policyEvaluation.getScanId(),
        policyViolation.getActionTypeId(), policyEvaluation.getTime());

    NewestRiskDTO riskDTO2 = result.dashboardResults.get(2);
    assertNewestRiskDTO(riskDTO2, app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO2, BuildStageType.ID, app1PolicyEvaluation.getScanId(),
        app1PolicyViolation.getActionTypeId(), app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_Unknown() throws Exception {
    ComponentIdentifier nullComponentIdentifier = null;
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "pathnames-hash", nullComponentIdentifier,
        "a.zip/b.zip", MatchState.UNKNOWN, false, evaluation.getTime());

    // create 2 violations with no component identifier and give one no pathname and one with a pathname.
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy, nullComponentIdentifier,
        "hash-4", "unknown");
    PolicyViolation policyViolationPathName = tempEntity.newPolicyViolation(evaluation, app1Policy,
        nullComponentIdentifier, "filename-hash", "unknown2", "b.zip");

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, "COMPONENT_NAME",
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(2));
    assertThat(result.numResults, is(2));

    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.derivedComponentName, is("b.zip")); // we use the last file in the path name
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolationPathName, evaluation.getTime());

    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.derivedComponentName, is("Unknown"));
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_NoTimeLimit() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    Date oldDate = new Date(0);
    String oldScanId = "test old scan id";
    PolicyEvaluation oldPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, oldScanId,
        oldDate);
    PolicyViolation oldPolicyViolation = tempEntity.newPolicyViolation(oldPolicyEvaluation, org1Policy);

    // first run a query that has a number of days small enough that it should not include this scan
    Integer maxDaysOld = 1000;
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, 
        Collections.singleton(app.getId()), null, null, null, null, null, null, maxDaysOld, 100);
    assertThat(result.dashboardResults, hasSize(0));
    assertThat(result.numResults, is(0));

    // then run a query with no date limit
    maxDaysOld = null;
    result = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
        null, maxDaysOld, 100);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    assertNewestRiskDTO(result.dashboardResults.get(0), app, org1, oldPolicyViolation, oldPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(result.dashboardResults.get(0), BuildStageType.ID, oldScanId,
        orgPolicyViolation.getActionTypeId(), oldPolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_InvalidTimeLimit() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    Integer maxDaysOld;
    try {
      maxDaysOld = -50;
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
          null, maxDaysOld, 100);
      fail("Expected IllegalArgumentException when maxDaysOld is negative");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Max Days Old must be a positive integer"));
    }

    try {
      maxDaysOld = 0;
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
          null, maxDaysOld, 100);
      fail("Expected IllegalArgumentException when maxDaysOld is zero");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Max Days Old must be a positive integer"));
    }
  }

  @Test
  public void testGetNewestRisks_LastViolationWithFirstOccurrenceTime() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    DateTime time1 = new DateTime().minusDays(1);
    Date firstOccurrenceTime = time1.toDate();
    String scanId1 = "scanId1";
    PolicyEvaluation policyEval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1,
        time1.toDate());
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEval1, org1Policy);

    DateTime time2 = time1.plusHours(1);
    String scanId2 = "scanId2";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId2, time2.toDate());

    DateTime time3 = time2.plusHours(1);
    String scanId3 = "scanId3";
    PolicyEvaluation policyEval3 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId3,
        time3.toDate());
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEval3, org1Policy);

    DateTime time4 = time3.plusHours(1);
    String scanId4 = "scanId4";
    PolicyEvaluation policyEval4 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId4,
        time4.toDate());

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, 
        Collections.singleton(app.getId()), null, null, null, null, null, null, 
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));

    assertNewestRiskDTO(result.dashboardResults.get(0), app, org1, policyViolation1, firstOccurrenceTime);
    assertNewestRiskDTOContainsStageDetail(result.dashboardResults.get(0), ReleaseStageType.ID, scanId4,
        policyViolation3.getActionTypeId(), policyEval4.getTime());
  }

  @Test
  public void testGetNewestRisks_ViolationWithoutHash() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy, "g", "a", "v",
        null /* hash */, "reason");

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null, 
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, null, 
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));

    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_ViolationWithoutFirstOccurrence() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, Collections.singleton(ReleaseStageType.ID), null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_LatestReport() throws Exception {
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());

    // run a few evals and make sure we return the latest
    PolicyEvaluation releaseEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app2PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(releaseEvaluation, org1Policy,
        app2PolicyViolation.getThreatLevel(), app2PolicyViolation.getThreatCategory(),
        app2PolicyViolation.getComponentIdentifier(), app2PolicyViolation.getHash());

    result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, policyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO, ReleaseStageType.ID, releaseEvaluation.getScanId(),
        policyViolation.getActionTypeId(), releaseEvaluation.getTime());

    PolicyEvaluation buildEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app2 build id", new Date(app2PolicyEvaluation.getTime().getTime() + 2));

    result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, policyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO, BuildStageType.ID, buildEvaluation.getScanId(),
        policyViolation.getActionTypeId(), buildEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_Deduplicate() throws Exception {
    Application app = tempEntity.newApplication(org1.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation, org1Policy);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation, org1Policy);
    // Set different constraint facts on the policy violations to ensure they are different.
    violation1.setConstraintFacts(Collections.singletonList(buildConstraintFact(org1Policy, "trigger1")));
    new PolicyViolationDAO().update(violation1);
    violation2.setConstraintFacts(Collections.singletonList(buildConstraintFact(org1Policy, "trigger1")));
    new PolicyViolationDAO().update(violation2);

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD,
        1000);

    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app, org1, violation1, evaluation.getTime());
  }

  private ConstraintFact buildConstraintFact(Policy policy, String trigger) {
    Constraint constraint = policy.getConstraints().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().toString());
    ConditionFact conditionFact = new ConditionFact(constraint.getConditions().get(0).getConditionTypeId(), 0,
        "summary", "reason");
    conditionFact.setTriggerJson(trigger);
    constraintFact.addConditionFact(conditionFact);

    return constraintFact;
  }

  private void assertNewestRiskDTO(NewestRiskDTO actual,
                                   Application app,
                                   Organization org,
                                   PolicyViolation policyViolation,
                                   Date firstOccurrenceTime)
  {
    assertThat(actual.organizationName, is(org.getName()));
    assertThat(actual.applicationName, is(app.getName()));
    assertThat(actual.applicationPublicId, is(app.getPublicId()));
    assertThat(actual.threatLevel, is(policyViolation.getThreatLevel()));
    assertThat(actual.firstOccurrenceTime, is(firstOccurrenceTime.getTime()));
    assertThat(actual.policyName, is(policyViolation.getPolicyName()));
    assertThat(actual.policyId, is(policyViolation.getPolicyId()));
    assertThat(actual.hash, is(policyViolation.getHash()));
    if (policyViolation.getComponentIdentifier() != null) {
      assertDisplayFieldValues(actual.displayName.parts, policyViolation);
      assertThat(actual.derivedComponentName, is(actual.displayName.toString()));
    }
    else {
      assertThat(actual.displayName, is(nullValue()));
    }
    assertThat(actual.filename, is(policyViolation.getFilename()));
  }

  private void assertNewestRiskDTOContainsStageDetail(NewestRiskDTO actual,
                                                      String stageTypeId,
                                                      String scanId,
                                                      String actionTypeId,
                                                      Date lastOccurrenceTime)
  {
    assertThat(actual.stageTypeId, is(stageTypeId));
    assertThat(actual.actionTypeId, is(actionTypeId));
    assertThat(actual.scanId, is(scanId));
    assertThat(actual.lastOccurrenceTime, is(lastOccurrenceTime.getTime()));
  }
}
