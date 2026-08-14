/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

abstract class AbstractApplicationRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  private TestProductLicense testProductLicense;

  private Organization org;

  protected Application app1;

  protected Application app2;

  private Policy orgPolicy;

  private Policy app1Policy;

  private PolicyEvaluation app1PolicyEvaluation;

  private PolicyEvaluation app2PolicyEvaluation;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
  }

  protected abstract ApplicationRiskService getApplicationRiskService();

  @Test
  public void testGetApplicationRisks_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> getApplicationRiskService().getApplicationRisks(null, null, null, null, null, null, null, "-TOTAL_RISK",
            0,
            100));
  }

  @Test
  public void testGetApplicationRisks_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService()
        .getApplicationRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults.get(0).getStageRiskScore(DevelopStageType.ID)).isNull();
    assertThat(result.dashboardResults.get(1).getStageRiskScore(DevelopStageType.ID)).isNull();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> getApplicationRiskService()
        .getApplicationRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null,
            null, null, "-TOTAL_RISK", 0, 100))
        .withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGetApplicationRisks_StagesInChronologicalOrder() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), OperateStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), StageReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app1.getId()),
        new LinkedHashSet<>(Arrays.asList(ReleaseStageType.ID, OperateStageType.ID, BuildStageType.ID,
            StageReleaseStageType.ID)),
        null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).extracting(dto -> dto.stageTypeId)
        .containsExactly(BuildStageType.ID,
            StageReleaseStageType.ID, ReleaseStageType.ID, OperateStageType.ID);
  }

  @Test
  public void testGetApplicationRisks_ViolationForComponentWithoutHash() {
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, null, null, null, null, "unknown");

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService()
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null, null, "-TOTAL_RISK",
            0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk)
        .isEqualTo(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel() * 2);
  }

  @Test
  public void testGetApplicationRisks_FilterByPolicyViolationState() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService()
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk).isEqualTo(app1Policy.getThreatLevel());

    Policy app1LegacyViolationPolicy = tempEntity.newPolicy(app1);
    tempEntity.newLegacyPolicyViolation(app1PolicyEvaluation, app1LegacyViolationPolicy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    result = getApplicationRiskService()
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk).isEqualTo(app1LegacyViolationPolicy.getThreatLevel());

    result = getApplicationRiskService()
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.OPEN), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk).isEqualTo(orgPolicy.getThreatLevel()
        + app1Policy.getThreatLevel());

    result = getApplicationRiskService()
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION,
                PolicyViolationState.OPEN),
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk).isEqualTo(
        orgPolicy.getThreatLevel() + app1Policy.getThreatLevel() * 2 + app1LegacyViolationPolicy.getThreatLevel());
  }

  @Test
  public void testGetApplicationRisks_ResultsCountCanExceedNumberOfReturnedResults() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService()
        .getApplicationRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(true);
  }

  @Test
  public void testGetApplicationRisks() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app1.getId()), Collections.singleton(BuildStageType.ID), null, null, null, null,
        "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).hasSize(1);
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertRisk(appDTO.totalApplicationRisk, 0, 5, 3, 0, 8);
    assertThat(appDTO.organizationName).isEqualTo(org.getName());
    assertThat(appDTO.applicationName).isEqualTo(app1.getName());
    assertThat(appDTO.applicationId).isEqualTo(app1.getPublicId());
    assertThat(appDTO.stageRisks).hasSize(1);

    StageRiskScoreDTO buildStageRisk = appDTO.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk.risk, 0, 5, 3, 0, 8);
    assertThat(buildStageRisk.scanId).isEqualTo("test scan app1 id");
    assertThat(buildStageRisk.stageTypeName).isEqualTo(StageTypes.BUILD.getName());
  }

  @Test
  public void testGetApplicationRisks_ExcludesRisksOfZero() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "app owned policy", 0);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID,
        "test scan app id", new Date());
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app.getId()), Collections.singleton(BuildStageType.ID), null, null, null, null,
        "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).isEmpty();
  }

  @Test
  public void testGetApplicationRisks_ExcludesStageWithoutViolations() {
    tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scan app1 id");

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app1.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks).hasSize(1);
    assertThat(appDTO.stageRisks.get(0).stageTypeId).isEqualTo(BuildStageType.ID);
  }

  @Test
  public void testGetApplicationRisks_SortedByRiskThenAppId() {
    Application app0 = tempEntity.newApplication("app0", "app0", org.getId());
    PolicyEvaluation policyEvaluation0 = tempEntity.newPolicyEvaluation(app0.getId(), BuildStageType.ID,
        "test scan app0 id", new Date());
    tempEntity.newPolicyViolation(policyEvaluation0, orgPolicy);

    Application app3 = tempEntity.newApplication("app3", "app3", org.getId());
    Policy policy3 = tempEntity.newPolicy(app3.getId(), "app owned policy", 9);
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID,
        "test scan app3 id", new Date());
    tempEntity.newPolicyViolation(policyEvaluation3, policy3);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(null, null, null,
            null, null, null, null, "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).hasSize(4);

    // app3 should be first because it has the highest risk.
    // app0 and app2 have the same risk, so app0 must be before app2 based on their IDs.

    ApplicationRiskScoreDTO applicationRiskScoreDTO = result.dashboardResults.get(0);
    assertThat(applicationRiskScoreDTO.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO.applicationName).isEqualTo(app3.getName());
    assertThat(applicationRiskScoreDTO.applicationId).isEqualTo(app3.getPublicId());
    assertThat(applicationRiskScoreDTO.totalApplicationRisk.totalRisk).isEqualTo(9);

    applicationRiskScoreDTO = result.dashboardResults.get(1);
    assertThat(applicationRiskScoreDTO.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO.applicationName).isEqualTo(app1.getName());
    assertThat(applicationRiskScoreDTO.applicationId).isEqualTo(app1.getPublicId());
    assertThat(applicationRiskScoreDTO.totalApplicationRisk.totalRisk).isEqualTo(8);

    applicationRiskScoreDTO = result.dashboardResults.get(2);
    assertThat(applicationRiskScoreDTO.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO.applicationName).isEqualTo(app0.getName());
    assertThat(applicationRiskScoreDTO.applicationId).isEqualTo(app0.getPublicId());
    assertThat(applicationRiskScoreDTO.totalApplicationRisk.totalRisk).isEqualTo(3);

    applicationRiskScoreDTO = result.dashboardResults.get(3);
    assertThat(applicationRiskScoreDTO.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO.applicationName).isEqualTo(app2.getName());
    assertThat(applicationRiskScoreDTO.applicationId).isEqualTo(app2.getPublicId());
    assertThat(applicationRiskScoreDTO.totalApplicationRisk.totalRisk).isEqualTo(3);
  }

  @Test
  public void testGetApplicationRisks_SortedByName() {
    Application app = tempEntity.newApplication("Sandbox-app", "Sandbox-app", org.getId());
    PolicyEvaluation policyEvaluation0 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID,
        "test scan app0 id", new Date());
    tempEntity.newPolicyViolation(policyEvaluation0, orgPolicy);

    DashboardResultsDTO<ApplicationRiskScoreDTO> results =
        getApplicationRiskService().getApplicationRisks(null, null, null,
            null, null, null, null, "-NAME", 0, Integer.MAX_VALUE);
    List<ApplicationRiskScoreDTO> apps = results.dashboardResults;

    assertThat(apps).hasSize(3);
    assertThat(apps.get(0).applicationName).isEqualTo(app.getName());
    assertThat(apps.get(1).applicationName).isEqualTo(app2.getName());
    assertThat(apps.get(2).applicationName).isEqualTo(app1.getName());
  }

  @Test
  public void testGetApplicationRisks_TotalApplicationRiskDeDupesAcrossStagesUsingLatestEvaluationData() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "app owned policy1", 5);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID,
        "test scan app id1", new Date(System.currentTimeMillis() - 4000));
    tempEntity.newPolicyViolation(policyEvaluation1, policy);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID,
        "test scan app id2", new Date(System.currentTimeMillis() - 2000));
    tempEntity.newPolicyViolation(policyEvaluation2, policy);
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID,
        "test scan app id3", new Date());
    tempEntity.newPolicyViolation(policyEvaluation3, policy, 10, policy.getThreatCategory());

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).hasSize(1);

    ApplicationRiskScoreDTO applicationRiskScoreDTO = result.dashboardResults.get(0);
    assertRisk(applicationRiskScoreDTO.totalApplicationRisk, 10, 5, 0, 0, 15);
    assertThat(applicationRiskScoreDTO.stageRisks).hasSize(3);

    StageRiskScoreDTO buildStageRisk = applicationRiskScoreDTO.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);

    StageRiskScoreDTO stageReleaseStageRisk = applicationRiskScoreDTO.getStageRiskScore(StageReleaseStageType.ID);
    assertRisk(stageReleaseStageRisk.risk, 10, 0, 0, 0, 10);

    StageRiskScoreDTO releaseStageRisk = applicationRiskScoreDTO.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk.risk, 0, 5, 0, 0, 5);
  }

  @Test
  public void testGetApplicationRisks_TwoStages() {
    Application app = tempEntity.newApplication("ts-app", "ts-app", org.getId());
    Policy policy1 = tempEntity.newPolicy(app.getId(), "app owned policy1", 5);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID,
        "test scan app id1", new Date());
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    Policy policy2 = tempEntity.newPolicy(app.getId(), "app owned policy2", 7);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID,
        "test scan app id2", new Date());
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app.getId()), new LinkedHashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)),
        null, null, null, null, "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).hasSize(1);
    ApplicationRiskScoreDTO applicationRiskScoreDTO = result.dashboardResults.get(0);
    assertRisk(applicationRiskScoreDTO.totalApplicationRisk, 0, 12, 0, 0, 12);
    assertThat(applicationRiskScoreDTO.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO.applicationName).isEqualTo(app.getName());
    assertThat(applicationRiskScoreDTO.applicationId).isEqualTo(app.getPublicId());
    assertThat(applicationRiskScoreDTO.stageRisks).hasSize(2);

    StageRiskScoreDTO buildStageRisk = applicationRiskScoreDTO.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk.scanId).isEqualTo(policyEvaluation1.getScanId());

    StageRiskScoreDTO releaseStageRisk = applicationRiskScoreDTO.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk.risk, 0, 7, 0, 0, 7);
    assertThat(releaseStageRisk.scanId).isEqualTo(policyEvaluation2.getScanId());
  }

  @Test
  public void testGetApplicationRisks_TwoStagesTwoApps() {
    Application app1 = tempEntity.newApplication("tsta-app1", "tsta-app1", org.getId());
    Policy policy11 = tempEntity.newPolicy(app1.getId(), "app owned policy11", 5);
    PolicyEvaluation policyEvaluation11 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID,
        "test scan app id11", new Date());
    tempEntity.newPolicyViolation(policyEvaluation11, policy11);
    Policy policy12 = tempEntity.newPolicy(app1.getId(), "app owned policy12", 7);
    PolicyEvaluation policyEvaluation12 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app id12", new Date());
    tempEntity.newPolicyViolation(policyEvaluation12, policy12);

    Application app2 = tempEntity.newApplication("tsta-app2", "tsta-app2", org.getId());
    Policy policy21 = tempEntity.newPolicy(app2.getId(), "app owned policy21", 1);
    PolicyEvaluation policyEvaluation21 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app id21", new Date());
    tempEntity.newPolicyViolation(policyEvaluation21, policy21);
    Policy policy22 = tempEntity.newPolicy(app2.getId(), "app owned policy22", 3);
    PolicyEvaluation policyEvaluation22 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID,
        "test scan app id22", new Date());
    tempEntity.newPolicyViolation(policyEvaluation22, policy22);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        new LinkedHashSet<>(Arrays.asList(app1.getId(), app2.getId())),
        new LinkedHashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), null, null, null, null,
        "-TOTAL_RISK", 0, Integer.MAX_VALUE);

    assertThat(result.dashboardResults).hasSize(2);

    ApplicationRiskScoreDTO applicationRiskScoreDTO1 = result.dashboardResults.get(0);
    assertRisk(applicationRiskScoreDTO1.totalApplicationRisk, 0, 12, 0, 0, 12);
    assertThat(applicationRiskScoreDTO1.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO1.applicationName).isEqualTo(app1.getName());
    assertThat(applicationRiskScoreDTO1.applicationId).isEqualTo(app1.getPublicId());
    assertThat(applicationRiskScoreDTO1.stageRisks).hasSize(2);

    StageRiskScoreDTO buildStageRisk1 = applicationRiskScoreDTO1.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk1.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk1.scanId).isEqualTo(policyEvaluation11.getScanId());

    StageRiskScoreDTO releaseStageRisk1 = applicationRiskScoreDTO1.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk1.risk, 0, 7, 0, 0, 7);
    assertThat(releaseStageRisk1.scanId).isEqualTo(policyEvaluation12.getScanId());

    ApplicationRiskScoreDTO applicationRiskScoreDTO2 = result.dashboardResults.get(1);
    assertRisk(applicationRiskScoreDTO2.totalApplicationRisk, 0, 0, 3, 1, 4);
    assertThat(applicationRiskScoreDTO2.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO2.applicationName).isEqualTo(app2.getName());
    assertThat(applicationRiskScoreDTO2.applicationId).isEqualTo(app2.getPublicId());
    assertThat(applicationRiskScoreDTO2.stageRisks).hasSize(2);

    StageRiskScoreDTO buildStageRisk2 = applicationRiskScoreDTO2.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk2.risk, 0, 0, 0, 1, 1);
    assertThat(buildStageRisk2.scanId).isEqualTo(policyEvaluation21.getScanId());

    StageRiskScoreDTO releaseStageRisk2 = applicationRiskScoreDTO2.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk2.risk, 0, 0, 3, 0, 3);
    assertThat(releaseStageRisk2.scanId).isEqualTo(policyEvaluation22.getScanId());
  }

  @Test
  public void testGetApplicationRisks_Pagination() {
    Application app1 = tempEntity.newApplication("tsta-app1", "tsta-app1", org.getId());
    Policy policy11 = tempEntity.newPolicy(app1.getId(), "app owned policy11", 5);
    PolicyEvaluation policyEvaluation11 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID,
        "test scan app id11", new Date());
    tempEntity.newPolicyViolation(policyEvaluation11, policy11);
    Policy policy12 = tempEntity.newPolicy(app1.getId(), "app owned policy12", 7);
    PolicyEvaluation policyEvaluation12 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app id12", new Date());
    tempEntity.newPolicyViolation(policyEvaluation12, policy12);

    Application app2 = tempEntity.newApplication("tsta-app2", "tsta-app2", org.getId());
    Policy policy21 = tempEntity.newPolicy(app2.getId(), "app owned policy21", 1);
    PolicyEvaluation policyEvaluation21 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app id21", new Date());
    tempEntity.newPolicyViolation(policyEvaluation21, policy21);
    Policy policy22 = tempEntity.newPolicy(app2.getId(), "app owned policy22", 3);
    PolicyEvaluation policyEvaluation22 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID,
        "test scan app id22", new Date());
    tempEntity.newPolicyViolation(policyEvaluation22, policy22);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        new LinkedHashSet<>(Arrays.asList(app1.getId(), app2.getId())),
        new LinkedHashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), null, null, null, null,
        "-TOTAL_RISK", 0, 1);

    assertThat(result.dashboardResults).hasSize(1);

    ApplicationRiskScoreDTO applicationRiskScoreDTO1 = result.dashboardResults.get(0);
    assertRisk(applicationRiskScoreDTO1.totalApplicationRisk, 0, 12, 0, 0, 12);
    assertThat(applicationRiskScoreDTO1.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO1.applicationName).isEqualTo(app1.getName());
    assertThat(applicationRiskScoreDTO1.applicationId).isEqualTo(app1.getPublicId());
    assertThat(applicationRiskScoreDTO1.stageRisks).hasSize(2);

    StageRiskScoreDTO buildStageRisk1 = applicationRiskScoreDTO1.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk1.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk1.scanId).isEqualTo(policyEvaluation11.getScanId());

    StageRiskScoreDTO releaseStageRisk1 = applicationRiskScoreDTO1.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk1.risk, 0, 7, 0, 0, 7);
    assertThat(releaseStageRisk1.scanId).isEqualTo(policyEvaluation12.getScanId());

    // Get next page
    result = getApplicationRiskService().getApplicationRisks(null,
        new LinkedHashSet<>(Arrays.asList(app1.getId(), app2.getId())),
        new LinkedHashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), null, null, null, null,
        "-TOTAL_RISK", 1, 1);

    ApplicationRiskScoreDTO applicationRiskScoreDTO2 = result.dashboardResults.get(0);
    assertRisk(applicationRiskScoreDTO2.totalApplicationRisk, 0, 0, 3, 1, 4);
    assertThat(applicationRiskScoreDTO2.organizationName).isEqualTo(org.getName());
    assertThat(applicationRiskScoreDTO2.applicationName).isEqualTo(app2.getName());
    assertThat(applicationRiskScoreDTO2.applicationId).isEqualTo(app2.getPublicId());
    assertThat(applicationRiskScoreDTO2.stageRisks).hasSize(2);

    StageRiskScoreDTO buildStageRisk2 = applicationRiskScoreDTO2.getStageRiskScore(BuildStageType.ID);
    assertRisk(buildStageRisk2.risk, 0, 0, 0, 1, 1);
    assertThat(buildStageRisk2.scanId).isEqualTo(policyEvaluation21.getScanId());

    StageRiskScoreDTO releaseStageRisk2 = applicationRiskScoreDTO2.getStageRiskScore(ReleaseStageType.ID);
    assertRisk(releaseStageRisk2.risk, 0, 0, 3, 0, 3);
    assertThat(releaseStageRisk2.scanId).isEqualTo(policyEvaluation22.getScanId());

    // Get next page
    result = getApplicationRiskService().getApplicationRisks(null,
        new LinkedHashSet<>(Arrays.asList(app1.getId(), app2.getId())),
        new LinkedHashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), null, null, null, null,
        "-TOTAL_RISK", 2, 1);

    assertThat(result.dashboardResults).isEmpty();
  }

  @Test
  public void testGetApplicationRisks_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> getApplicationRiskService()
        .getApplicationRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, Integer.MAX_VALUE))
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGetRiskForOwner_Application_populatesOrgAndAppFields() {
    ApplicationRiskScoreDTO dto =
        getApplicationRiskService().getRiskForOwner(app1, Collections.singleton(StageTypes.BUILD));

    assertThat(dto).isNotNull();
    // The constructor packs the app publicId into the `applicationId` field and the internal
    // id into the `id` field (labelled that way for wire compatibility with existing consumers).
    assertThat(dto.id).isEqualTo(app1.getId());
    assertThat(dto.applicationId).isEqualTo(app1.getPublicId());
    assertThat(dto.applicationName).isEqualTo(app1.getName());
    assertThat(dto.organizationId).isEqualTo(app1.getOrganizationId());
    assertThat(dto.organizationName).isEqualTo(org.getName());
    assertThat(dto.totalApplicationRisk.totalRisk)
        .isEqualTo(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel());
  }

  @Test
  public void testGetRiskForOwner_Hrc_populatesIdOnly_andAggregatesViolationsByOwnerId() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Policy hrcPolicy = tempEntity.newPolicy(hrc.getId(), "hrc policy", 7);
    PolicyEvaluation hrcEval = tempEntity.newPolicyEvaluation(hrc.getId(), BuildStageType.ID,
        "hrc-scan-id", new Date(System.currentTimeMillis() - 1000));
    tempEntity.newPolicyViolation(hrcEval, hrcPolicy);

    ApplicationRiskScoreDTO dto =
        getApplicationRiskService().getRiskForOwner(hrc, Collections.singleton(StageTypes.BUILD));

    assertThat(dto).isNotNull();
    // Only `id` is populated for non-Application owners. Callers must not read the org /
    // appName / applicationId fields on this branch — they are null by design.
    assertThat(dto.id).isEqualTo(hrc.getId());
    assertThat(dto.applicationId).isNull();
    assertThat(dto.applicationName).isNull();
    assertThat(dto.organizationId).isNull();
    assertThat(dto.organizationName).isNull();
    assertThat(dto.totalApplicationRisk.totalRisk).isEqualTo(hrcPolicy.getThreatLevel());
  }

  @Test
  public void testGetRiskForOwner_Application_matchesEquivalent_Hrc_aggregation() {
    // Parity guarantee: given identical seed data, the totalRisk returned for an HRC owner and
    // for an Application owner is the same integer.
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Policy hrcPolicy = tempEntity.newPolicy(hrc.getId(), "shared policy", 6);
    PolicyEvaluation hrcEval = tempEntity.newPolicyEvaluation(hrc.getId(), BuildStageType.ID,
        "hrc-parity-scan", new Date(System.currentTimeMillis() - 1000));
    tempEntity.newPolicyViolation(hrcEval, hrcPolicy);

    Application parityApp = tempEntity.newApplication("parity-app", "parity-app", org.getId());
    Policy parityAppPolicy = tempEntity.newPolicy(parityApp.getId(), "shared policy", 6);
    PolicyEvaluation parityAppEval = tempEntity.newPolicyEvaluation(parityApp.getId(), BuildStageType.ID,
        "app-parity-scan", new Date(System.currentTimeMillis() - 1000));
    tempEntity.newPolicyViolation(parityAppEval, parityAppPolicy);

    int hrcTotal = getApplicationRiskService()
        .getRiskForOwner(hrc, Collections.singleton(StageTypes.BUILD)).totalApplicationRisk.totalRisk;
    int appTotal = getApplicationRiskService()
        .getRiskForOwner(parityApp, Collections.singleton(StageTypes.BUILD)).totalApplicationRisk.totalRisk;

    assertThat(hrcTotal).isEqualTo(appTotal);
  }

  private void assertRisk(RiskDTO risk, int criticalRisk, int severeRisk, int moderateRisk, int lowRisk, int netRisk) {
    assertThat(risk).isNotNull();
    assertThat(risk.criticalRisk).isEqualTo(criticalRisk);
    assertThat(risk.severeRisk).isEqualTo(severeRisk);
    assertThat(risk.moderateRisk).isEqualTo(moderateRisk);
    assertThat(risk.lowRisk).isEqualTo(lowRisk);
    assertThat(risk.totalRisk).isEqualTo(netRisk);
  }
}
