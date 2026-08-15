/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public abstract class AbstractComponentRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  protected PolicyViolationDAO violationDAO;

  @Inject
  protected TestProductLicense testProductLicense;

  protected Organization org;

  protected Application app1;

  protected Application app2;

  protected Policy orgPolicy;

  protected Policy app1Policy;

  protected PolicyEvaluation app1PolicyEvaluation;

  protected PolicyEvaluation app2PolicyEvaluation;

  protected PolicyViolation orgPolicyViolation;

  protected PolicyViolation app1PolicyViolation;

  protected PolicyViolation app2PolicyViolation;

  protected Tag tag1;

  protected Tag tag2;

  protected abstract DashboardComponentRiskService getComponentRiskService();

  @Before
  @BeforeEach
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplicationWithParent("app2", "app2");
    orgPolicy = tempEntity.newPolicy(org.getParentOwnerId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getParentOwnerId());
    tag2 = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
  }

  @Test
  public void testGetComponentRisks_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getComponentRiskService()
                .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0,
                    100));
  }

  @Test
  public void testGetComponentRisks_DedupViolationsForSameAppAndPolicyByPickingMostRecentViolationAcrossStages() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel() + 1,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1");

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(violation.getThreatLevel() + orgPolicy.getThreatLevel() * 2);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_MultipleViolationConstraintsOnSameComponent() {
    PolicyViolation violation1 = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, "Group1", "Artifact1",
        "Version1", "hash", "ConstraintFact1", "unknown.jar");
    PolicyViolation violation2 = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, "Group1", "Artifact1",
        "Version1", "hash", "ConstraintFact2", "unknown.jar");

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation1.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation1);
    assertThat(riskDTO.score).isEqualTo(violation1.getThreatLevel() * 3 + orgPolicy.getThreatLevel() * 2);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
    assertDisplayFieldValues(riskDTO.displayName.parts, violation2);
  }

  @Test
  public void testGetComponentRisks_FilterByApplication() {
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null,
        Collections.singleton(app2.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByOrganization() {
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByEmptyOrganization() {
    Organization emptyOrg = tempEntity.newOrganization();
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(Collections.singleton(emptyOrg.getId()), null, null, null, null, null, null, "-TOTAL_RISK",
            0, 100);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_FilterByStage() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, "-TOTAL_RISK", 0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults.get(0).hash).isEqualTo(app1PolicyViolation.getHash());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getComponentRiskService()
                .getComponentRisks(null, null, Collections.singleton(DevelopStageType.ID), null,
                    null, null, null, "-TOTAL_RISK", 0, 100))
        .withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGetComponentRisks_FilterByTag() {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, Collections.singleton(app2Tag.getId()), null, null, null, "-TOTAL_RISK",
            0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatCategory() {
    Policy licensePolicy =
        tempEntity.newPolicy(app1, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    PolicyViolation violation =
        tempEntity.newPolicyViolation(app1PolicyEvaluation, licensePolicy, "gid", "aid", "1", "hash");

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, new PolicyThreatCategoryFilter(violation.getThreatCategory()), null,
            null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatLevel() {
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, new PolicyThreatLevelFilter(3, 3), null,
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel() * 2);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_FilterByPolicyViolationState() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    PolicyViolationDAO policyViolationDAO = violationDAO;
    PolicyViolation policyViolation = policyViolationDAO.getById(waivedViolation.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = getComponentRiskService().getComponentRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel() +
        app2PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(2);

    result = getComponentRiskService().getComponentRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel() +
        app2PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByApplicationAndPolicyViolationState() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    PolicyViolationDAO policyViolationDAO = violationDAO;
    PolicyViolation policyViolation = policyViolationDAO.getById(waivedViolation.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = getComponentRiskService()
        .getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.OPEN), "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = getComponentRiskService()
        .getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN),
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ScoreBreakdown() {
    for (int i = 0; i <= 10; i++) {
      if (i == app2PolicyViolation.getThreatLevel()) {
        continue;
      }
      Policy orgPolicy = tempEntity.newPolicy(org.getId(), "policy " + i, i);
      tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    }

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null,
        Collections.singleton(app2.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.scoreCritical).isEqualTo(27);
    assertThat(riskDTO.scoreSevere).isEqualTo(22);
    assertThat(riskDTO.scoreModerate).isEqualTo(5);
    assertThat(riskDTO.scoreLow).isEqualTo(1);
    assertThat(riskDTO.score).isEqualTo(55);
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ScoreBreakdown_Org() {
    for (int i = 0; i <= 10; i++) {
      if (i == app2PolicyViolation.getThreatLevel()) {
        continue;
      }
      Policy orgPolicy = tempEntity.newPolicy(org.getId(), "policy " + i, i);
      tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    }

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.scoreCritical).isEqualTo(27);
    assertThat(riskDTO.scoreSevere).isEqualTo(22);
    assertThat(riskDTO.scoreModerate).isEqualTo(5);
    assertThat(riskDTO.scoreLow).isEqualTo(1);
    assertThat(riskDTO.score).isEqualTo(55);
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ResultCapping() {
    String gid = "gid";
    String aid = "aid";
    String ver = "1";
    String hash = "somehash";
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(true);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(hash);
    assertThat(riskDTO.score).isEqualTo(12);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_ResultCapping_NextPage() {
    String gid = "gid";
    String aid = "aid";
    String ver = "1";
    String hash = "somehash";
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 1, 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo("hash");
    assertThat(riskDTO.score).isEqualTo(11);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_ResultCapping_PageOutOfBounds() {
    String gid = "gid";
    String aid = "aid";
    String ver = "1";
    String hash = "somehash";
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 2, 1);
    assertThat(result.dashboardResults).isEmpty();
  }

  @Test
  public void testGetComponentRisks_Unknown() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "pathnames-hash", null, "a.zip/b.zip",
        MatchState.UNKNOWN, false, evaluation.getTime());

    // Create 2 violations without component identifiers: one with a pathname and one without.
    ComponentIdentifier nullComponentIdentifier = null;
    tempEntity.newPolicyViolation(evaluation, app1Policy, nullComponentIdentifier, "hash-4", "unknown");
    tempEntity.newPolicyViolation(evaluation, app1Policy, nullComponentIdentifier, "filename-hash", "unknown2",
        "b.zip");

    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, "TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);

    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.filename).isEqualTo("b.zip");
    assertThat(riskDTO.derivedComponentName).isEqualTo("b.zip"); // we use the last file of the first path name

    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.derivedComponentName).isEqualTo("Unknown");
  }

  @Test
  public void testGetComponentRisks_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    assertThatExceptionOfType(ConflictException.class).isThrownBy(
        () -> getComponentRiskService().getComponentRisks(null, null, null, null, null, null, null, "NAME", 0, 100))
        .withMessage("The dashboard feature has been disabled.");
  }
}
