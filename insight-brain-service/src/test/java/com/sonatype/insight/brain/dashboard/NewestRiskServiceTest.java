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
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByOrganization() throws Exception {
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(Collections.singleton(app2.getParentOwnerId()),
            null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertThat(riskDTO.stageTypeId).isNotEqualTo(DevelopStageType.ID);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      newestRiskService.getNewestRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null, null,
          null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    }).withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGetNewestRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org1.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, Collections.singleton(app2Tag.getId()), null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatCategory() throws Exception {
    Policy licensePolicy =
        tempEntity.newPolicy(app1, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(app1PolicyEvaluation, licensePolicy, "gid", "aid", "1", "hash1");

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null, null, null, null,
        new PolicyThreatCategoryFilter(policyViolation.getThreatCategory()), null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(result.dashboardResults).hasSize(1);
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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, waivedViolation, app1PolicyEvaluation.getTime());

    Policy app1GrandfatherPolicy = tempEntity.newPolicy(app1.getId(), "policy Grandfather", 1);
    PolicyViolation grandfatherViolation = tempEntity
        .newGrandfatheredPolicyViolation(app1PolicyEvaluation, app1GrandfatherPolicy,
            ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    result = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.GRANDFATHERED), null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, grandfatherViolation, app1PolicyEvaluation.getTime());

    result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, new PolicyViolationStateFilter(PolicyViolationState.OPEN),
            "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults).hasSize(3);
    assertThat(result.numResults).isEqualTo(3);
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    result = newestRiskService.getNewestRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.GRANDFATHERED,
            PolicyViolationState.OPEN), "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults).hasSize(5);
    assertThat(result.numResults).isEqualTo(5);
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
    assertThat(result.dashboardResults).hasSize(3);
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    // Limit to 1
    result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(3);
    assertNewestRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app1PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, app1Policy,
        app1PolicyViolation.getComponentIdentifier(), app1PolicyViolation.getHash());

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 100);
    assertThat(result.dashboardResults).hasSize(3);
    assertThat(result.numResults).isEqualTo(3);

    NewestRiskDTO riskDTO0 = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO0, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO0, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());

    NewestRiskDTO riskDTO1 = result.dashboardResults.get(1);
    assertNewestRiskDTO(riskDTO1, app1, org1, app1PolicyViolation, app1PolicyEvaluation.getTime());
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
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.numResults).isEqualTo(2);

    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.derivedComponentName).isEqualTo("b.zip"); // we use the last file in the path name
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolationPathName, evaluation.getTime());

    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.derivedComponentName).isEqualTo("Unknown");
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
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);

    // then run a query with no date limit
    maxDaysOld = null;
    result = newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null,
        null, maxDaysOld, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertNewestRiskDTO(result.dashboardResults.get(0), app, org1, oldPolicyViolation, oldPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(result.dashboardResults.get(0), BuildStageType.ID, oldScanId,
        orgPolicyViolation.getActionTypeId(), oldPolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_InvalidTimeLimit() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
          -50, 100);
    }).withMessage("Max Days Old must be a positive integer");

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      newestRiskService.getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, null, 0,
          100);
    }).withMessage("Max Days Old must be a positive integer");
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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);

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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);

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
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_LatestReport() throws Exception {
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());

    // run a few evals and make sure we return the latest
    PolicyEvaluation releaseEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app2PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(releaseEvaluation, org1Policy,
        app2PolicyViolation.getComponentIdentifier(), app2PolicyViolation.getHash());

    result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetail(riskDTO, ReleaseStageType.ID, releaseEvaluation.getScanId(),
        policyViolation.getActionTypeId(), releaseEvaluation.getTime());

    PolicyEvaluation buildEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app2 build id", new Date(app2PolicyEvaluation.getTime().getTime() + 2));

    result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
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
    violation1.setConstraintFacts(Collections
        .singletonList(buildConstraintFact(org1Policy, "{\"conditionIndex\":1,\"trigger\":{\"foo\":\"bar\"}}")));
    new PolicyViolationDAO().update(violation1);
    violation2.setConstraintFacts(Collections
        .singletonList(buildConstraintFact(org1Policy, "{\"conditionIndex\":1,\"trigger\":{\"foo\":\"bar\"}}")));
    new PolicyViolationDAO().update(violation2);

    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService.getNewestRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD,
        1000);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    NewestRiskDTO riskDTO = result.dashboardResults.get(0);
    assertNewestRiskDTO(riskDTO, app, org1, violation1, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> {
      newestRiskService
          .getNewestRisks(null, null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD,
              1000);
    }).withMessage("The dashboard feature has been disabled.");
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
    assertThat(actual.organizationName).isEqualTo(org.getName());
    assertThat(actual.applicationName).isEqualTo(app.getName());
    assertThat(actual.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(actual.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(actual.firstOccurrenceTime).isEqualTo(firstOccurrenceTime.getTime());
    assertThat(actual.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(actual.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(actual.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(actual.hash).isEqualTo(policyViolation.getHash());
    if (policyViolation.getComponentIdentifier() != null) {
      assertDisplayFieldValues(actual.displayName.parts, policyViolation);
      assertThat(actual.derivedComponentName).isEqualTo(actual.displayName.toString());
    }
    else {
      assertThat(actual.displayName).isNull();
    }
    assertThat(actual.filename).isEqualTo(policyViolation.getFilename());
  }

  private void assertNewestRiskDTOContainsStageDetail(NewestRiskDTO actual,
                                                      String stageTypeId,
                                                      String scanId,
                                                      String actionTypeId,
                                                      Date lastOccurrenceTime)
  {
    assertThat(actual.stageTypeId).isEqualTo(stageTypeId);
    assertThat(actual.actionTypeId).isEqualTo(actionTypeId);
    assertThat(actual.scanId).isEqualTo(scanId);
    assertThat(actual.lastOccurrenceTime).isEqualTo(lastOccurrenceTime.getTime());
  }
}
