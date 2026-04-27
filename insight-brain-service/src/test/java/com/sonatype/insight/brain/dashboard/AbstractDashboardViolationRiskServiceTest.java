/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
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
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

abstract class AbstractDashboardViolationRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private TestProductLicense testProductLicense;

  protected Organization org1;

  protected Organization org2;

  protected Application app1;

  protected Application app2;

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

  protected abstract DashboardViolationRiskService getDashboardViolationRiskService();

  @Test
  public void testGet_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getDashboardViolationRiskService().get(null, null, null, null, null, null, null, null,
                DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100));
  }

  @Test
  public void testGet_FilterByApplication() {
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_FilterByOrganization() {
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(Collections.singleton(app2.getParentOwnerId()),
            null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_FilterByStage() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGet_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "newScanId");
    Policy policy = tempEntity.newPolicy(app2);
    tempEntity.newPolicyViolation(evaluation, policy);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> getDashboardViolationRiskService()
        .get(null, null, Collections.singleton(DevelopStageType.ID), null, null, null, null,
            null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100))
        .withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGet_FilterByTag() {
    Tag app2Tag = tempEntity.newTag(org1.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, null, Collections.singleton(app2Tag.getId()), null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_FilterByPolicyThreatCategory() {
    Policy licensePolicy =
        tempEntity.newPolicy(app1, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(app1PolicyEvaluation, licensePolicy, "gid", "aid", "1", "hash1");

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null, null, null,
        null, new PolicyThreatCategoryFilter(policyViolation.getThreatCategory()), null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_FilterByPolicyThreatLevel() {
    Policy app1Policy1 = tempEntity.newPolicy(app1.getId(), "app owned policy1", 7);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy1, "gid", "aid",
        "1", "hash1");
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null, null, null,
        null, null, new PolicyThreatLevelFilter(7, 7), null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(result.dashboardResults).hasSize(1);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_FilterByPolicyViolationState() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, new PolicyViolationStateFilter(PolicyViolationState.WAIVED),
            null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, waivedViolation, app1PolicyEvaluation.getTime());

    Policy app1LegacyViolationPolicy = tempEntity.newPolicy(app1.getId(), "Legacy Violation Policy", 1);
    PolicyViolation legacyViolation = tempEntity
        .newLegacyPolicyViolation(app1PolicyEvaluation, app1LegacyViolationPolicy,
            ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    result = getDashboardViolationRiskService().get(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION), null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, legacyViolation, app1PolicyEvaluation.getTime());

    result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, new PolicyViolationStateFilter(PolicyViolationState.OPEN),
            "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(3);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation,
        app1PolicyEvaluation.getTime());

    result = getDashboardViolationRiskService().get(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION,
            PolicyViolationState.OPEN),
        "-AGE,-THREAT_LEVEL", DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(5);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(2), app1, org1, waivedViolation,
        app1PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(3), app1, org1, orgPolicyViolation,
        app1PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(4), app1, org1, legacyViolation,
        app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_SortAndResultCapping() {
    // Limit to high value
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(3);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(1), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());
    assertDashboardViolationRiskDTO(result.dashboardResults.get(2), app1, org1, orgPolicyViolation,
        app1PolicyEvaluation.getTime());

    // Page size 1, get page 0
    result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(true);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app2, org2, app2PolicyViolation,
        app2PolicyEvaluation.getTime());

    // Page size 1, get page 1
    result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 1, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(true);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app1, org1, app1PolicyViolation,
        app1PolicyEvaluation.getTime());

    // Must return an empty list when page out of bounds
    result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 3, 1);
    assertThat(result.dashboardResults).isEmpty();
  }

  @Test
  public void testGet() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app1PolicyEvaluation.getTime().getTime() + 1));
    tempEntity.newPolicyViolation(policyEvaluation, app1Policy,
        app1PolicyViolation.getComponentIdentifier(), app1PolicyViolation.getHash());

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(3);
    assertThat(result.hasNextPage).isEqualTo(false);

    DashboardViolationRiskDTO riskDTO0 = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO0, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());

    DashboardViolationRiskDTO riskDTO1 = result.dashboardResults.get(1);
    assertDashboardViolationRiskDTO(riskDTO1, app1, org1, app1PolicyViolation, app1PolicyEvaluation.getTime());

    DashboardViolationRiskDTO riskDTO2 = result.dashboardResults.get(2);
    assertDashboardViolationRiskDTO(riskDTO2, app1, org1, orgPolicyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_Unknown() throws Exception {
    ComponentIdentifier nullComponentIdentifier = null;

    // create 2 violations with no component identifier and give one no pathname and one with a pathname.
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(evaluation1, app1Policy, nullComponentIdentifier, "hash-4", "unknown");
    // Ensure policy violations don't have the same openTime.
    awaitNextTimestamp();
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), OperateStageType.ID, "newScanIdApp2");
    PolicyViolation policyViolationPathName = tempEntity.newPolicyViolation(evaluation2, app1Policy,
        nullComponentIdentifier, "filename-hash", "unknown2", "b.zip");

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null, null,
        Set.of(ReleaseStageType.ID, OperateStageType.ID), null, null, null, null, "-AGE",
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);

    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.derivedComponentName).isEqualTo("b.zip"); // we use the last file in the path name
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolationPathName, evaluation2.getTime());

    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.derivedComponentName).isEqualTo("Unknown");
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, evaluation1.getTime());
  }

  private void awaitNextTimestamp() throws Exception {
    long now = System.currentTimeMillis();
    while (System.currentTimeMillis() <= now) {
      Thread.sleep(1);
    }
  }

  @Test
  public void testGet_TimeLimit() {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    Date oldDate = new Date(0);
    String oldScanId = "test old scan id";
    PolicyEvaluation oldPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, oldScanId,
        oldDate);
    PolicyViolation oldPolicyViolation = tempEntity.newPolicyViolation(oldPolicyEvaluation, org1Policy);

    // first run a query that has a number of days small enough that it should not include this scan
    Integer maxDaysOld = 1000;
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null,
        Collections.singleton(app.getId()), null, null, null, null, null, null, maxDaysOld, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);

    // then run a query with no date limit
    maxDaysOld = null;
    result = getDashboardViolationRiskService().get(null, Collections.singleton(app.getId()), null, null,
        null, null, null,
        null, maxDaysOld, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app, org1, oldPolicyViolation,
        oldPolicyEvaluation.getTime());
  }

  @Test
  public void testGet_InvalidTimeLimit() {
    Application app = tempEntity.newApplication("myapp", "myapp", org1.getId());

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> getDashboardViolationRiskService()
        .get(null, Collections.singleton(app.getId()), null, null, null, null, null, null, -50, 0, 100))
        .withMessage("Max Days Old must be a positive integer");

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> getDashboardViolationRiskService()
        .get(null, Collections.singleton(app.getId()), null, null, null, null, null, null, 0, 0, 100))
        .withMessage("Max Days Old must be a positive integer");
  }

  @Test
  public void testGet_LastViolationWithFirstOccurrenceTime() {
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
    tempEntity.newPolicyViolation(policyEval3, org1Policy);

    DateTime time4 = time3.plusHours(1);
    String scanId4 = "scanId4";
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId4, time4.toDate());

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null,
        Collections.singleton(app.getId()), null, null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);

    assertDashboardViolationRiskDTO(result.dashboardResults.get(0), app, org1, policyViolation1, firstOccurrenceTime);
  }

  @Test
  public void testGet_ViolationWithoutHash() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy, "g", "a", "v",
        null /* hash */, "reason");

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, null,
        DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);

    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGet_ViolationWithoutFirstOccurrence() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, null, Collections.singleton(ReleaseStageType.ID), null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app1, org1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGet_LatestReport() {
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());

    // run a few evals and make sure we return the latest
    PolicyEvaluation releaseEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app2PolicyEvaluation.getTime().getTime() + 1));
    tempEntity.newPolicyViolation(releaseEvaluation, org1Policy,
        app2PolicyViolation.getComponentIdentifier(), app2PolicyViolation.getHash());

    result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());

    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 build id",
        new Date(app2PolicyEvaluation.getTime().getTime() + 2));

    result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app2.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO, app2, org2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGet_MultipleConstraints() {
    Application app = tempEntity.newApplication(org1.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation, org1Policy);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation, org1Policy);
    // Set different constraint facts on the policy violations to ensure they are different.
    violation1.setConstraintFacts(Collections
        .singletonList(buildConstraintFact(org1Policy, "{\"conditionIndex\":1,\"trigger\":{\"foo\":\"bar1\"}}")));
    policyViolationDAO.update(violation1);
    violation2.setConstraintFacts(Collections
        .singletonList(buildConstraintFact(org1Policy, "{\"conditionIndex\":1,\"trigger\":{\"foo\":\"bar2\"}}")));
    policyViolationDAO.update(violation2);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService().get(null,
        Collections.singleton(app.getId()), null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD,
        0, 100);

    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    DashboardViolationRiskDTO riskDTO1 = result.dashboardResults.get(0);
    assertDashboardViolationRiskDTO(riskDTO1, app, org1, violation1, evaluation.getTime());
    DashboardViolationRiskDTO riskDTO2 = result.dashboardResults.get(1);
    assertDashboardViolationRiskDTO(riskDTO2, app, org1, violation2, evaluation.getTime());
  }

  @Test
  public void testGet_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> getDashboardViolationRiskService()
        .get(null, null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0,
            100))
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGet_FilterByWaived_DoesNotIncludeExcludedViolations() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    final PolicyViolation policyViolation1 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation2 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation3 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);

    // No exclusions exist
    DashboardResultsDTO<DashboardViolationRiskDTO> results = getDashboardViolationRiskService().get(Set.of(org.getId()),
        Set.of(app.getId()), Set.of(StageTypes.BUILD.getId()), Set.of(), null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED)),
        DashboardViolationRiskOrderByEnum.AGE.name(), null, 0, 100);
    assertThat(results.dashboardResults)
        .hasSize(3)
        .extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder(policyViolation1.getId(), policyViolation2.getId(), policyViolation3.getId());

    // Add an exclusion for policyViolation1, so it should not be included in the results
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), "", "", waiver.getId(), scanId, policyViolation1);

    results = getDashboardViolationRiskService().get(Set.of(),
        Set.of(app.getId()), Set.of(StageTypes.BUILD.getId()), Set.of(), null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED)),
        DashboardViolationRiskOrderByEnum.AGE.name(), null, 0, 100);
    assertThat(results.dashboardResults)
        .hasSize(2)
        .extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder(policyViolation2.getId(), policyViolation3.getId());
  }

  @Test
  public void testGet_FilterByWaived_DoesNotIncludeOrgLevelExcludedViolations() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    final PolicyViolation policyViolation1 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation2 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);

    // Create exclusion at the org level (ancestor), not the app level
    tempEntity.newAutoPolicyWaiverExclusion(org.getId(), "", "", waiver.getId(), scanId, policyViolation1);

    // The org-level exclusion should still filter out policyViolation1
    DashboardResultsDTO<DashboardViolationRiskDTO> results = getDashboardViolationRiskService().get(Set.of(),
        Set.of(app.getId()), Set.of(StageTypes.BUILD.getId()), Set.of(), null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED)),
        DashboardViolationRiskOrderByEnum.AGE.name(), null, 0, 100);
    assertThat(results.dashboardResults)
        .hasSize(1)
        .extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder(policyViolation2.getId());
  }

  @Test
  public void testGet_FilterByWaived_DoesNotCheckForExcludedViolations_WhenFilteringByWaivedPlusOtherState() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    final PolicyViolation policyViolation1 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation2 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation3 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);

    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), "", "", waiver.getId(), scanId, policyViolation1);

    final DashboardResultsDTO<DashboardViolationRiskDTO> results = getDashboardViolationRiskService().get(Set.of(),
        Set.of(app.getId()), Set.of(StageTypes.BUILD.getId()), Set.of(), null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED, PolicyViolationState.OPEN)),
        DashboardViolationRiskOrderByEnum.AGE.name(), null, 0, 100);
    assertThat(results.dashboardResults)
        .hasSize(3)
        .extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder(policyViolation1.getId(), policyViolation2.getId(), policyViolation3.getId());
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

  private void assertDashboardViolationRiskDTO(
      DashboardViolationRiskDTO actual,
      Application app,
      Organization org,
      PolicyViolation policyViolation,
      Date firstOccurrenceTime)
  {
    assertThat(actual.organizationName).isEqualTo(org.getName());
    assertThat(actual.applicationName).isEqualTo(app.getName());
    assertThat(actual.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(actual.firstOccurrenceTime).isEqualTo(firstOccurrenceTime.getTime());
    assertThat(actual.policyName).isEqualTo(policyViolation.getPolicyName());
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

    // Only check constraint facts if they are loaded - they may not be loaded in all test scenarios
    if (policyViolation.constraintFactsAreLoaded()) {
      Optional<ConditionFact> conditionFact =
          policyViolation.getConstraintFacts().isEmpty()
              ? Optional.empty()
              : policyViolation.getConstraintFacts()
                  .get(0)
                  .getConditionFacts()
                  .stream()
                  .filter(Objects::nonNull)
                  .findFirst();
      if (conditionFact.filter(condition -> condition.getReference() != null &&
          Type.SECURITY_VULNERABILITY_REFID.equals(condition.getReference().getType())).isPresent())
      {
        assertThat(actual.referenceId).isEqualTo(conditionFact.get().getReference().getValue());
      }
      else {
        assertThat(actual.referenceId).isNull();
      }
    }
  }
}
